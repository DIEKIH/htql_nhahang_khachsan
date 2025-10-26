package com.example.htql_nhahang_khachsan.service;

import com.example.htql_nhahang_khachsan.dto.*;
import com.example.htql_nhahang_khachsan.entity.*;
import com.example.htql_nhahang_khachsan.enums.BookingStatus;
import com.example.htql_nhahang_khachsan.enums.TableStatus;
import com.example.htql_nhahang_khachsan.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TableBookingService {

    private final TableBookingRepository bookingRepository;
    private final RestaurantTableRepository tableRepository;
    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final TableBookingEmailService emailService;

    @Transactional
    public TableBookingResponse createBooking(Long branchId, TableBookingRequest request, Long customerId) {
        BranchEntity branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chi nhánh"));

        UserEntity customer = userRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));

        // Kiểm tra ngày giờ hợp lệ
        validateBookingDateTime(request.getBookingDate(), request.getBookingTime());

        // Tạo booking
        TableBookingEntity booking = TableBookingEntity.builder()
                .customer(customer)
                .customerName(request.getCustomerName())
                .customerEmail(request.getCustomerEmail())
                .branch(branch)
                .bookingDate(request.getBookingDate())
                .bookingTime(request.getBookingTime())
                .partySize(request.getPartySize())
                .contactPhone(request.getContactPhone())
                .specialRequests(request.getSpecialRequests())
                .status(BookingStatus.PENDING)
                .tables(new ArrayList<>())
                .build();

        // Gợi ý bàn tự động nếu không chọn bàn cụ thể
        if (request.getPreferredTableIds() == null || request.getPreferredTableIds().isEmpty()) {
            List<RestaurantTableEntity> suggestedTables = autoSuggestTables(
                    branchId,
                    request.getPartySize(),
                    request.getBookingDate(),
                    request.getBookingTime()
            );
            booking.setTables(suggestedTables);
        } else {
            List<RestaurantTableEntity> selectedTables = tableRepository.findAllById(request.getPreferredTableIds());
            booking.setTables(selectedTables);
        }

        TableBookingEntity savedBooking = bookingRepository.save(booking);

        // Gửi email xác nhận
        emailService.sendBookingConfirmation(savedBooking);

        return convertToResponse(savedBooking);
    }

    public TableSuggestionResponse getSuggestedTables(Long branchId, Integer partySize,
                                                      LocalDate date, LocalTime time) {
        List<RestaurantTableEntity> availableTables = getAvailableTables(branchId, date, time);

        List<TableCombination> suggestions = new ArrayList<>();

        // Tìm 1 bàn vừa đủ
        for (RestaurantTableEntity table : availableTables) {
            if (table.getCapacity() >= partySize && table.getCapacity() <= partySize + 2) {
                TableCombination single = TableCombination.builder()
                        .tables(Arrays.asList(convertToTableInfo(table)))
                        .totalCapacity(table.getCapacity())
                        .description("Bàn " + table.getTableNumber() + " (" + table.getCapacity() + " chỗ)")
                        .build();
                suggestions.add(single);
            }
        }

        // Tìm kết hợp 2 bàn
        if (suggestions.isEmpty() || partySize > 6) {
            for (int i = 0; i < availableTables.size(); i++) {
                for (int j = i + 1; j < availableTables.size(); j++) {
                    RestaurantTableEntity table1 = availableTables.get(i);
                    RestaurantTableEntity table2 = availableTables.get(j);
                    int totalCap = table1.getCapacity() + table2.getCapacity();

                    if (totalCap >= partySize && totalCap <= partySize + 3) {
                        TableCombination combo = TableCombination.builder()
                                .tables(Arrays.asList(
                                        convertToTableInfo(table1),
                                        convertToTableInfo(table2)
                                ))
                                .totalCapacity(totalCap)
                                .description("Ghép bàn " + table1.getTableNumber() +
                                        " + " + table2.getTableNumber() + " (" + totalCap + " chỗ)")
                                .build();
                        suggestions.add(combo);
                    }
                }
            }
        }

        boolean canAccommodate = !suggestions.isEmpty();
        String message = canAccommodate
                ? "Tìm thấy " + suggestions.size() + " phương án phù hợp"
                : "Không tìm thấy bàn phù hợp cho " + partySize + " khách. Vui lòng chọn thời gian khác.";

        return TableSuggestionResponse.builder()
                .canAccommodate(canAccommodate)
                .message(message)
                .suggestions(suggestions.stream().limit(5).collect(Collectors.toList()))
                .build();
    }

    private List<RestaurantTableEntity> autoSuggestTables(Long branchId, Integer partySize,
                                                          LocalDate date, LocalTime time) {
        TableSuggestionResponse suggestion = getSuggestedTables(branchId, partySize, date, time);

        if (!suggestion.isCanAccommodate() || suggestion.getSuggestions().isEmpty()) {
            return new ArrayList<>();
        }

        // Lấy phương án đầu tiên
        TableCombination bestOption = suggestion.getSuggestions().get(0);
        return bestOption.getTables().stream()
                .map(info -> tableRepository.findById(info.getId()).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<RestaurantTableEntity> getAvailableTables(Long branchId, LocalDate date, LocalTime time) {
        List<RestaurantTableEntity> allTables = tableRepository.findByBranchIdAndStatus(
                branchId, TableStatus.AVAILABLE);

        // Lọc bỏ các bàn đã được đặt trong khung giờ (±2 tiếng)
        LocalTime startTime = time.minusHours(2);
        LocalTime endTime = time.plusHours(2);

        List<TableBookingEntity> conflictBookings = bookingRepository
                .findByBranchIdAndDateAndTimeRange(
                        branchId,
                        date,
                        startTime,
                        endTime,
                        List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED, BookingStatus.CHECKED_IN) // ✅ thêm danh sách trạng thái
                );

        Set<Long> bookedTableIds = conflictBookings.stream()
                .flatMap(b -> b.getTables().stream())
                .map(RestaurantTableEntity::getId)
                .collect(Collectors.toSet());

        return allTables.stream()
                .filter(t -> !bookedTableIds.contains(t.getId()))
                .collect(Collectors.toList());
    }


    private void validateBookingDateTime(LocalDate date, LocalTime time) {
        LocalDateTime bookingDateTime = LocalDateTime.of(date, time);
        LocalDateTime now = LocalDateTime.now();

        if (bookingDateTime.isBefore(now.plusHours(2))) {
            throw new RuntimeException("Vui lòng đặt bàn trước ít nhất 2 tiếng");
        }

        if (date.isAfter(now.toLocalDate().plusMonths(2))) {
            throw new RuntimeException("Chỉ nhận đặt bàn trong vòng 2 tháng");
        }

        // Kiểm tra giờ mở cửa (7h - 23h)
        if (time.isBefore(LocalTime.of(7, 0)) || time.isAfter(LocalTime.of(23, 0))) {
            throw new RuntimeException("Nhà hàng chỉ phục vụ từ 7:00 - 23:00");
        }
    }

    private TableBookingResponse convertToResponse(TableBookingEntity booking) {
        return TableBookingResponse.builder()
                .id(booking.getId())
                .bookingCode(booking.getBookingCode())
                .customerName(booking.getCustomerName())
                .customerEmail(booking.getCustomerEmail())
                .contactPhone(booking.getContactPhone())
                .branchName(booking.getBranch().getName())
                .bookingDate(booking.getBookingDate())
                .bookingTime(booking.getBookingTime())
                .partySize(booking.getPartySize())
                .assignedTables(booking.getTables().stream()
                        .map(this::convertToTableInfo)
                        .collect(Collectors.toList()))
                .status(booking.getStatus())
                .statusDisplay(getStatusDisplay(booking.getStatus()))
                .specialRequests(booking.getSpecialRequests())
                .createdAt(booking.getCreatedAt())
                .actualArrival(booking.getActualArrival())
                .build();
    }

    private TableInfo convertToTableInfo(RestaurantTableEntity table) {
        return TableInfo.builder()
                .id(table.getId())
                .tableNumber(table.getTableNumber())
                .capacity(table.getCapacity())
                .build();
    }

    private String getStatusDisplay(BookingStatus status) {
        switch (status) {
            case PENDING:
                return "Chờ xác nhận";
            case CONFIRMED:
                return "Đã xác nhận";
            case CHECKED_IN:
                return "Đã nhận bàn";
            case CHECKED_OUT:
                return "Hoàn thành";
            case CANCELLED:
                return "Đã hủy";
            case NO_SHOW:
                return "Khách không đến";
            default:
                return status.name();
        }
    }


    public List<TableBookingResponse> getAllBookingsByBranch(Long branchId) {
        List<TableBookingEntity> bookings = bookingRepository.findByBranchIdOrderByCreatedAtDesc(branchId);
        return bookings.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public TableBookingResponse getBookingById(Long id, Long branchId) {
        TableBookingEntity booking = bookingRepository.findByIdAndBranchId(id, branchId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt bàn"));
        return convertToResponse(booking);
    }

    @Transactional
    public void confirmBooking(Long id, Long branchId) {
        TableBookingEntity booking = bookingRepository.findByIdAndBranchId(id, branchId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt bàn"));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new RuntimeException("Chỉ có thể xác nhận đơn đang chờ duyệt");
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        emailService.sendBookingConfirmed(booking);
    }

    @Transactional
    public void cancelBooking(Long id, Long branchId, String reason) {
        TableBookingEntity booking = bookingRepository.findByIdAndBranchId(id, branchId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn đặt bàn"));

        if (booking.getStatus() == BookingStatus.CONFIRMED || booking.getStatus() == BookingStatus.CANCELLED) {
            throw new RuntimeException("Không thể hủy đơn đã hoàn thành hoặc đã hủy");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        emailService.sendBookingCancelled(booking, reason);
    }

    public void updateBookingStatus(Long bookingId, Long branchId, BookingStatus newStatus) {
        TableBookingEntity booking = bookingRepository.findByIdAndBranchId(bookingId, branchId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đặt bàn"));
        booking.setStatus(newStatus);
        bookingRepository.save(booking);
    }

    public void resendBookingEmail(Long bookingId, Long branchId) {
        TableBookingEntity booking = bookingRepository.findByIdAndBranchId(bookingId, branchId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đặt bàn"));

        switch (booking.getStatus()) {
            case CONFIRMED -> emailService.sendBookingConfirmed(booking);
            case CANCELLED -> emailService.sendBookingCancelled(booking, "Hủy theo yêu cầu");
            default -> emailService.sendBookingConfirmation(booking);
        }
    }


}