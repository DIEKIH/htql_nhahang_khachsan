package com.example.htql_nhahang_khachsan.service;

import com.example.htql_nhahang_khachsan.dto.*;
import com.example.htql_nhahang_khachsan.entity.*;
import com.example.htql_nhahang_khachsan.enums.*;
import com.example.htql_nhahang_khachsan.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ReceptionistBookingService {

    private final RoomBookingRepository bookingRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomRepository roomRepository;
    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final BranchRepository branchRepository;

//    public DashboardStatsDTO getDashboardStats(Long branchId, LocalDate today) {
//        List<RoomBookingEntity> todayBookings = bookingRepository.findAll().stream()
//                .filter(b -> b.getBranch().getId().equals(branchId))
//                .filter(b -> !b.getCheckInDate().isAfter(today) && !b.getCheckOutDate().isBefore(today))
//                .collect(Collectors.toList());
//
//        long checkInToday = todayBookings.stream()
//                .filter(b -> b.getCheckInDate().equals(today))
//                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED || b.getStatus() == BookingStatus.PENDING)
//                .count();
//
//        long checkOutToday = todayBookings.stream()
//                .filter(b -> b.getCheckOutDate().equals(today))
//                .filter(b -> b.getStatus() == BookingStatus.CHECKED_IN)
//                .count();
//
//        long occupied = todayBookings.stream()
//                .filter(b -> b.getStatus() == BookingStatus.CHECKED_IN)
//                .count();
//
//        long totalRooms = roomRepository.findByBranchId(branchId).size();
//        long available = totalRooms - occupied;
//
//        return DashboardStatsDTO.builder()
//                .checkInToday((int) checkInToday)
//                .checkOutToday((int) checkOutToday)
//                .occupied((int) occupied)
//                .available((int) available)
//                .build();
//    }

    public List<ReceptionistBookingDTO> searchBookings(Long branchId, String search, BookingStatus status, LocalDate date) {
        List<RoomBookingEntity> bookings = bookingRepository.findAll().stream()
                .filter(b -> b.getBranch().getId().equals(branchId))
                .filter(b -> search == null ||
                        b.getBookingCode().contains(search) ||
                        b.getGuestName().contains(search) ||
                        b.getGuestPhone().contains(search))
                .filter(b -> status == null || b.getStatus() == status)
                .filter(b -> date == null ||
                        (b.getCheckInDate().equals(date) || b.getCheckOutDate().equals(date)))
                .sorted(Comparator.comparing(RoomBookingEntity::getCheckInDate).reversed())
                .collect(Collectors.toList());

        return bookings.stream()
                .map(this::convertToReceptionistDTO)
                .collect(Collectors.toList());
    }

    public RoomBookingEntity createWalkinBooking(WalkinBookingRequest request, Long branchId, Long staffId) {
        RoomTypeEntity roomType = roomTypeRepository.findById(request.getRoomTypeId())
                .orElseThrow(() -> new EntityNotFoundException("Room type not found"));

        // Check availability
        List<RoomEntity> availableRooms = roomRepository.findAvailableRoomsByTypeAndDateRange(
                request.getRoomTypeId(),
                request.getCheckInDate(),
                request.getCheckOutDate(),
                RoomStatus.AVAILABLE,
                Arrays.asList(BookingStatus.CANCELLED, BookingStatus.NO_SHOW)
        );

        if (availableRooms.isEmpty()) {
            throw new IllegalStateException("Không còn phòng trống");
        }

        // Assign first available room
        RoomEntity room = availableRooms.get(0);

        // Calculate prices
        int nights = (int) ChronoUnit.DAYS.between(request.getCheckInDate(), request.getCheckOutDate());
        BigDecimal roomPrice = roomType.getPrice();
        BigDecimal totalRoomPrice = roomPrice.multiply(BigDecimal.valueOf(nights));
        BigDecimal serviceFee = totalRoomPrice.multiply(new BigDecimal("0.10"));
        BigDecimal vat = totalRoomPrice.add(serviceFee).multiply(new BigDecimal("0.10"));
        BigDecimal totalAmount = totalRoomPrice.add(serviceFee).add(vat);
        BigDecimal depositAmount = totalAmount.multiply(new BigDecimal("0.5"));

        // Create booking
        RoomBookingEntity booking = RoomBookingEntity.builder()
                .branch(branchRepository.findById(branchId).orElseThrow())
                .roomType(roomType)
                .room(room)
                .checkInDate(request.getCheckInDate())
                .checkOutDate(request.getCheckOutDate())
                .numberOfRooms(1)
                .adults(request.getAdults())
                .children(request.getChildren())
                .guestName(request.getGuestName())
                .guestEmail(request.getGuestEmail())
                .guestPhone(request.getGuestPhone())
                .guestIdNumber(request.getGuestIdNumber())
                .roomPrice(roomPrice)
                .numberOfNights(nights)
                .totalRoomPrice(totalRoomPrice)
                .serviceFee(serviceFee)
                .vat(vat)
                .totalAmount(totalAmount)
                .depositAmount(depositAmount)
                .remainingAmount(totalAmount.subtract(depositAmount))
                .specialRequests(request.getSpecialRequests())
                .status(BookingStatus.CONFIRMED)
                .paymentStatus(PaymentStatus.PENDING)
                .paymentMethod(request.getPaymentMethod())
                .build();

        return bookingRepository.save(booking);
    }

    public ReceptionistBookingDetailDTO getBookingDetail(Long bookingId, Long branchId) {
        RoomBookingEntity booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found"));

        if (!booking.getBranch().getId().equals(branchId)) {
            throw new SecurityException("Access denied");
        }

        return convertToDetailDTO(booking);
    }

    public void checkIn(Long bookingId, String idNumber, Long branchId, Long staffId) {
        RoomBookingEntity booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found"));

        if (!booking.getBranch().getId().equals(branchId)) {
            throw new SecurityException("Access denied");
        }

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Chỉ có thể check-in booking đã xác nhận");
        }

        if (booking.getRoom() == null) {
            throw new IllegalStateException("Chưa gán phòng cho booking này");
        }

        // Update booking
        booking.setStatus(BookingStatus.CHECKED_IN);
        booking.setCheckedInAt(LocalDateTime.now());
        if (idNumber != null) {
            booking.setGuestIdNumber(idNumber);
        }

        // Update room status
        RoomEntity room = booking.getRoom();
        room.setStatus(RoomStatus.OCCUPIED);
        roomRepository.save(room);

        bookingRepository.save(booking);
        log.info("Check-in successful for booking {}", booking.getBookingCode());
    }

    public InvoiceEntity checkOut(Long bookingId, Long branchId, Long staffId) {
        RoomBookingEntity booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found"));

        if (!booking.getBranch().getId().equals(branchId)) {
            throw new SecurityException("Access denied");
        }

        if (booking.getStatus() != BookingStatus.CHECKED_IN) {
            throw new IllegalStateException("Chỉ có thể check-out booking đã check-in");
        }

        // Update booking
        booking.setStatus(BookingStatus.CHECKED_OUT);
        booking.setCheckedOutAt(LocalDateTime.now());

        // Update room status
        if (booking.getRoom() != null) {
            booking.getRoom().setStatus(RoomStatus.CLEANING);
            roomRepository.save(booking.getRoom());
        }

        bookingRepository.save(booking);

        // Create invoice
        InvoiceEntity invoice = InvoiceEntity.builder()
                .roomBooking(booking)
                .subtotal(booking.getTotalRoomPrice())
                .serviceFee(booking.getServiceFee())
                .vat(booking.getVat())
                .total(booking.getTotalAmount())
                .paymentMethod(booking.getPaymentMethod())
                .issuedBy("Staff ID: " + staffId)
                .build();

        invoice = invoiceRepository.save(invoice);
        log.info("Check-out successful for booking {}, invoice {}", booking.getBookingCode(), invoice.getInvoiceCode());

        return invoice;
    }

    public void collectDeposit(Long bookingId, BigDecimal amount, PaymentMethod method, Long staffId) {
        RoomBookingEntity booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found"));

        PaymentEntity payment = PaymentEntity.builder()
                .roomBooking(booking)
                .amount(amount)
                .method(method)
                .status(PaymentStatus.PAID)
                .processedBy(staffId)
                .processedAt(LocalDateTime.now())
                .build();

        paymentRepository.save(payment);

        // Update booking payment status
        BigDecimal totalPaid = paymentRepository.findByRoomBookingId(bookingId).stream()
                .filter(p -> p.getStatus() == PaymentStatus.PAID)
                .map(PaymentEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalPaid.compareTo(booking.getTotalAmount()) >= 0) {
            booking.setPaymentStatus(PaymentStatus.PAID);
        } else {
            booking.setPaymentStatus(PaymentStatus.PARTIALLY_PAID);
        }

        bookingRepository.save(booking);
        log.info("Collected deposit {} for booking {}", amount, booking.getBookingCode());
    }

    public void changeRoom(Long bookingId, Long newRoomId, String reason, Long branchId) {
        RoomBookingEntity booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found"));

        if (!booking.getBranch().getId().equals(branchId)) {
            throw new SecurityException("Access denied");
        }

        RoomEntity newRoom = roomRepository.findById(newRoomId)
                .orElseThrow(() -> new EntityNotFoundException("Room not found"));

        // Check if new room is available
        boolean isAvailable = !bookingRepository.existsBookingForRoomInDateRange(
                newRoomId,
                booking.getCheckInDate(),
                booking.getCheckOutDate(),
                Arrays.asList(BookingStatus.CANCELLED, BookingStatus.NO_SHOW)
        );

        if (!isAvailable) {
            throw new IllegalStateException("Phòng này đã được đặt");
        }

        // Release old room
        if (booking.getRoom() != null) {
            booking.getRoom().setStatus(RoomStatus.AVAILABLE);
            roomRepository.save(booking.getRoom());
        }

        // Assign new room
        booking.setRoom(newRoom);
        if (booking.getStatus() == BookingStatus.CHECKED_IN) {
            newRoom.setStatus(RoomStatus.OCCUPIED);
        }
        roomRepository.save(newRoom);

        bookingRepository.save(booking);
        log.info("Changed room for booking {} from {} to {}", booking.getBookingCode(),
                booking.getRoom().getRoomNumber(), newRoom.getRoomNumber());
    }

    public void extendStay(Long bookingId, LocalDate newCheckOut) {
        RoomBookingEntity booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found"));

        if (newCheckOut.isBefore(booking.getCheckOutDate())) {
            throw new IllegalArgumentException("Ngày check-out mới phải sau ngày hiện tại");
        }

        // Calculate additional charges
        int additionalNights = (int) ChronoUnit.DAYS.between(booking.getCheckOutDate(), newCheckOut);
        BigDecimal additionalAmount = booking.getRoomPrice()
                .multiply(BigDecimal.valueOf(additionalNights));

        booking.setCheckOutDate(newCheckOut);
        booking.setNumberOfNights(booking.getNumberOfNights() + additionalNights);
        booking.setTotalAmount(booking.getTotalAmount().add(additionalAmount));
        booking.setRemainingAmount(booking.getRemainingAmount().add(additionalAmount));

        bookingRepository.save(booking);
        log.info("Extended stay for booking {} to {}", booking.getBookingCode(), newCheckOut);
    }

    public List<RoomTypeEntity> getAvailableRoomTypes(Long branchId) {
        return roomTypeRepository.findAll().stream()
                .filter(rt -> rt.getBranch().getId().equals(branchId))
                .filter(rt -> rt.getStatus() == Status.ACTIVE)
                .collect(Collectors.toList());
    }

    public AvailabilityResponse checkAvailability(Long roomTypeId, String checkIn, String checkOut) {
        LocalDate checkInDate = LocalDate.parse(checkIn);
        LocalDate checkOutDate = LocalDate.parse(checkOut);

        List<RoomEntity> available = roomRepository.findAvailableRoomsByTypeAndDateRange(
                roomTypeId, checkInDate, checkOutDate,
                RoomStatus.AVAILABLE,
                Arrays.asList(BookingStatus.CANCELLED, BookingStatus.NO_SHOW)
        );

        return AvailabilityResponse.builder()
                .available(!available.isEmpty())
                .availableRooms(available.size())
                .message(available.isEmpty() ? "Không còn phòng trống" : "Có " + available.size() + " phòng trống")
                .build();
    }

    public List<RoomStatusDTO> getRoomStatusToday(Long branchId, LocalDate today) {
        List<RoomEntity> rooms = roomRepository.findByBranchId(branchId);

        return rooms.stream().map(room -> {
            // Find booking for this room today
            Optional<RoomBookingEntity> booking = bookingRepository.findAll().stream()
                    .filter(b -> b.getRoom() != null && b.getRoom().getId().equals(room.getId()))
                    .filter(b -> !b.getCheckInDate().isAfter(today) && !b.getCheckOutDate().isBefore(today))
                    .filter(b -> b.getStatus() == BookingStatus.CONFIRMED || b.getStatus() == BookingStatus.CHECKED_IN)
                    .findFirst();

            return RoomStatusDTO.builder()
                    .roomId(room.getId())
                    .roomNumber(room.getRoomNumber())
                    .roomType(room.getRoomType().getName())
                    .floor(room.getFloor())
                    .status(room.getStatus())
                    .bookingCode(booking.map(RoomBookingEntity::getBookingCode).orElse(null))
                    .guestName(booking.map(RoomBookingEntity::getGuestName).orElse(null))
                    .checkInDate(booking.map(RoomBookingEntity::getCheckInDate).orElse(null))
                    .checkOutDate(booking.map(RoomBookingEntity::getCheckOutDate).orElse(null))
                    .build();
        }).collect(Collectors.toList());
    }

    private ReceptionistBookingDTO convertToReceptionistDTO(RoomBookingEntity entity) {
        return ReceptionistBookingDTO.builder()
                .id(entity.getId())
                .bookingCode(entity.getBookingCode())
                .guestName(entity.getGuestName())
                .guestPhone(entity.getGuestPhone())
                .roomTypeName(entity.getRoomType().getName())
                .roomNumber(entity.getRoom() != null ? entity.getRoom().getRoomNumber() : "Chưa gán")
                .checkInDate(entity.getCheckInDate())
                .checkOutDate(entity.getCheckOutDate())
                .adults(entity.getAdults())
                .children(entity.getChildren())
                .totalAmount(entity.getTotalAmount())
                .depositAmount(entity.getDepositAmount())
                .status(entity.getStatus())
                .paymentStatus(entity.getPaymentStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private ReceptionistBookingDetailDTO convertToDetailDTO(RoomBookingEntity entity) {
        return ReceptionistBookingDetailDTO.builder()
                .id(entity.getId())
                .bookingCode(entity.getBookingCode())
                .guestName(entity.getGuestName())
                .guestEmail(entity.getGuestEmail())
                .guestPhone(entity.getGuestPhone())
                .guestIdNumber(entity.getGuestIdNumber())
                .roomTypeId(entity.getRoomType().getId())
                .roomTypeName(entity.getRoomType().getName())
                .roomId(entity.getRoom() != null ? entity.getRoom().getId() : null)
                .roomNumber(entity.getRoom() != null ? entity.getRoom().getRoomNumber() : null)
                .checkInDate(entity.getCheckInDate())
                .checkOutDate(entity.getCheckOutDate())
                .numberOfNights(entity.getNumberOfNights())
                .adults(entity.getAdults())
                .children(entity.getChildren())
                .roomPrice(entity.getRoomPrice())
                .totalRoomPrice(entity.getTotalRoomPrice())
                .serviceFee(entity.getServiceFee())
                .vat(entity.getVat())
                .totalAmount(entity.getTotalAmount())
                .depositAmount(entity.getDepositAmount())
                .remainingAmount(entity.getRemainingAmount())
                .specialRequests(entity.getSpecialRequests())
                .paymentMethod(entity.getPaymentMethod())
                .paymentStatus(entity.getPaymentStatus())
                .status(entity.getStatus())
                .bookingDate(entity.getBookingDate())
                .checkedInAt(entity.getCheckedInAt())
                .checkedOutAt(entity.getCheckedOutAt())
                .build();
    }

    // ✅ THÊM method xác nhận thanh toán phần còn lại
    @Transactional
    public void confirmRemainingPayment(Long bookingId, Long staffId, Long branchId) {
        RoomBookingEntity booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found"));

        if (!booking.getBranch().getId().equals(branchId)) {
            throw new SecurityException("Access denied");
        }

        if (booking.getPaymentStatus() == PaymentStatus.PAID) {
            throw new IllegalStateException("Booking đã được thanh toán đầy đủ");
        }

        // Tính số tiền đã thanh toán
        BigDecimal totalPaid = paymentRepository.findByRoomBookingId(bookingId).stream()
                .filter(p -> p.getStatus() == PaymentStatus.PAID)
                .map(PaymentEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Tính số tiền còn lại
        BigDecimal remainingAmount = booking.getTotalAmount().subtract(totalPaid);

        if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Không còn số tiền cần thanh toán");
        }

        // Tạo payment cho phần còn lại
        PaymentEntity payment = PaymentEntity.builder()
                .roomBooking(booking)
                .amount(remainingAmount)
                .method(booking.getPaymentMethod() != null ? booking.getPaymentMethod() : PaymentMethod.CASH)
                .status(PaymentStatus.PAID)
                .processedBy(staffId)
                .processedAt(LocalDateTime.now())
                .build();

        paymentRepository.save(payment);

        // Cập nhật booking
        booking.setPaymentStatus(PaymentStatus.PAID);
        booking.setRemainingAmount(BigDecimal.ZERO);
        bookingRepository.save(booking);

        log.info("Confirmed remaining payment {} VND for booking {} by staff {}",
                remainingAmount, bookingId, staffId);
    }

    // Thêm vào ReceptionistBookingService.java

    public DashboardStatsDTO getDashboardStats(Long branchId, LocalDate today) {
        List<RoomBookingEntity> allBookings = bookingRepository.findAll().stream()
                .filter(b -> b.getBranch().getId().equals(branchId))
                .collect(Collectors.toList());

        // Check-in hôm nay (bookings có checkInDate = today và status = CONFIRMED hoặc PENDING)
        long checkInToday = allBookings.stream()
                .filter(b -> b.getCheckInDate().equals(today))
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED ||
                        b.getStatus() == BookingStatus.PENDING)
                .count();

        // Check-out hôm nay (bookings có checkOutDate = today và status = CHECKED_IN)
        long checkOutToday = allBookings.stream()
                .filter(b -> b.getCheckOutDate().equals(today))
                .filter(b -> b.getStatus() == BookingStatus.CHECKED_IN)
                .count();

        // Phòng đang sử dụng (bookings đang active)
        long occupied = allBookings.stream()
                .filter(b -> !b.getCheckInDate().isAfter(today) &&
                        !b.getCheckOutDate().isBefore(today))
                .filter(b -> b.getStatus() == BookingStatus.CHECKED_IN)
                .count();

        // Phòng trống
        long totalRooms = roomRepository.findByBranchId(branchId).size();
        long available = totalRooms - occupied;

        return DashboardStatsDTO.builder()
                .checkInToday((int) checkInToday)
                .checkOutToday((int) checkOutToday)
                .occupied((int) occupied)
                .available((int) available)
                .build();
    }

    public DashboardDataDTO getDashboardData(Long branchId, LocalDate today) {
        // Get stats
        DashboardStatsDTO stats = getDashboardStats(branchId, today);

        // Get today's check-ins
        List<ReceptionistBookingDTO> todayCheckIns = bookingRepository.findAll().stream()
                .filter(b -> b.getBranch().getId().equals(branchId))
                .filter(b -> b.getCheckInDate().equals(today))
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED ||
                        b.getStatus() == BookingStatus.PENDING ||
                        b.getStatus() == BookingStatus.CHECKED_IN)
                .sorted(Comparator.comparing(RoomBookingEntity::getCheckInDate))
                .limit(10)
                .map(this::convertToReceptionistDTO)
                .collect(Collectors.toList());

        // Get today's check-outs
        List<ReceptionistBookingDTO> todayCheckOuts = bookingRepository.findAll().stream()
                .filter(b -> b.getBranch().getId().equals(branchId))
                .filter(b -> b.getCheckOutDate().equals(today))
                .filter(b -> b.getStatus() == BookingStatus.CHECKED_IN)
                .sorted(Comparator.comparing(RoomBookingEntity::getCheckOutDate))
                .limit(10)
                .map(this::convertToReceptionistDTO)
                .collect(Collectors.toList());

        // Get unpaid deposits count
        long unpaidDeposits = bookingRepository.findAll().stream()
                .filter(b -> b.getBranch().getId().equals(branchId))
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED ||
                        b.getStatus() == BookingStatus.PENDING)
                .filter(b -> b.getPaymentStatus() == PaymentStatus.PENDING)
                .count();

        // Get rooms needing cleaning
        long roomsCleaning = roomRepository.findByBranchId(branchId).stream()
                .filter(r -> r.getStatus() == RoomStatus.CLEANING)
                .count();

        // Monthly bookings
        LocalDate firstDayOfMonth = today.withDayOfMonth(1);
        long monthlyBookings = bookingRepository.findAll().stream()
                .filter(b -> b.getBranch().getId().equals(branchId))
                .filter(b -> !b.getBookingDate().isBefore(firstDayOfMonth.atStartOfDay()))
                .filter(b -> b.getStatus() != BookingStatus.CANCELLED)
                .count();

        // Occupancy rate
        long totalRooms = roomRepository.findByBranchId(branchId).size();
        double occupancyRate = totalRooms > 0
                ? (stats.getOccupied() * 100.0 / totalRooms)
                : 0;

        // Today's revenue
        BigDecimal todayRevenue = paymentRepository.findAll().stream()
                .filter(p -> p.getProcessedAt() != null)
                .filter(p -> p.getProcessedAt().toLocalDate().equals(today))
                .filter(p -> p.getStatus() == PaymentStatus.PAID)
                .map(PaymentEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return DashboardDataDTO.builder()
                .stats(stats)
                .todayCheckIns(todayCheckIns)
                .todayCheckOuts(todayCheckOuts)
                .unpaidDepositsCount((int) unpaidDeposits)
                .roomsCleaningCount((int) roomsCleaning)
                .monthlyBookingsCount((int) monthlyBookings)
                .occupancyRate(occupancyRate)
                .todayRevenue(todayRevenue)
                .build();
    }
}
