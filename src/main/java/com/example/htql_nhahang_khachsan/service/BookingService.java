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
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final RoomBookingRepository bookingRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomRepository roomRepository;
    private final BranchRepository branchRepository;
    private final PromotionService promotionService;
    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;
    private final ChatbotBookingDraftRepository draftRepository;

    private final EmailService emailService;



    private static final BigDecimal BREAKFAST_FEE_PER_PERSON = new BigDecimal("200000");
    private static final BigDecimal SPA_FEE_PER_PERSON = new BigDecimal("500000");
    private static final BigDecimal AIRPORT_TRANSFER_FEE = new BigDecimal("300000");
    private static final BigDecimal SERVICE_FEE_RATE = new BigDecimal("0.10");
    private static final BigDecimal VAT_RATE = new BigDecimal("0.10");

    /**
     * Kiểm tra phòng trống
     */
    public AvailabilityResponse checkRoomAvailability(
            Long roomTypeId,
            String checkInDate,
            String checkOutDate,
            Integer numberOfRooms) {

        LocalDate checkIn = LocalDate.parse(checkInDate);
        LocalDate checkOut = LocalDate.parse(checkOutDate);

        // Lấy danh sách phòng trống
//        List<RoomEntity> availableRooms = roomRepository
//                .findAvailableRoomsByTypeAndDateRange(roomTypeId, checkIn, checkOut);

        List<RoomEntity> availableRooms = roomRepository.findAvailableRoomsByTypeAndDateRange(
                roomTypeId,
                checkIn,
                checkOut,
                RoomStatus.AVAILABLE,
                Arrays.asList(BookingStatus.CANCELLED, BookingStatus.NO_SHOW)
        );


        boolean isAvailable = availableRooms.size() >= numberOfRooms;
        String message = isAvailable
                ? "Có " + availableRooms.size() + " phòng trống"
                : "Chỉ còn " + availableRooms.size() + " phòng trống";

        return AvailabilityResponse.builder()
                .available(isAvailable)
                .availableRooms(availableRooms.size())
                .message(message)
                .build();
    }

    public BookingSessionDTO createBookingSession(
            Long roomTypeId,
            String checkInDate,
            String checkOutDate,
            Integer numberOfRooms,
            Integer adults,
            Integer children) {

        RoomTypeEntity roomType = roomTypeRepository.findById(roomTypeId)
                .orElseThrow(() -> new EntityNotFoundException("Room type not found"));

        LocalDate checkIn = LocalDate.parse(checkInDate);
        LocalDate checkOut = LocalDate.parse(checkOutDate);
        int nights = (int) ChronoUnit.DAYS.between(checkIn, checkOut);

        // ✅ Lấy giá sau giảm
        BigDecimal currentPrice = promotionService.calculateDiscountedPrice(
                roomType.getPrice(),
                roomType.getBranch().getId(),
                PromotionApplicability.ROOM
        );

        // ✅ THÊM log để debug
        System.out.println(">>> Creating booking session:");
        System.out.println(">>> - Room type: " + roomType.getName());
        System.out.println(">>> - Original price: " + roomType.getPrice());
        System.out.println(">>> - Discounted price: " + currentPrice);
        System.out.println(">>> - Nights: " + nights);
        System.out.println(">>> - Number of rooms: " + numberOfRooms);

        // ✅ Tính tổng tiền phòng
        BigDecimal totalRoomPrice = currentPrice
                .multiply(BigDecimal.valueOf(nights))
                .multiply(BigDecimal.valueOf(numberOfRooms));

        System.out.println(">>> - Total room price: " + totalRoomPrice);

        BigDecimal serviceFee = totalRoomPrice.multiply(SERVICE_FEE_RATE);
        BigDecimal subtotal = totalRoomPrice.add(serviceFee);
        BigDecimal vat = subtotal.multiply(VAT_RATE);
        BigDecimal totalAmount = subtotal.add(vat);

        System.out.println(">>> - Service fee: " + serviceFee);
        System.out.println(">>> - VAT: " + vat);
        System.out.println(">>> - Total amount: " + totalAmount);

        BigDecimal depositAmount = totalAmount.multiply(new BigDecimal("0.5"));
        BigDecimal remainingAmount = totalAmount.subtract(depositAmount);

        System.out.println(">>> - Deposit amount (50%): " + depositAmount);
        System.out.println(">>> - Remaining amount: " + remainingAmount);

        String sessionId = UUID.randomUUID().toString();

        return BookingSessionDTO.builder()
                .sessionId(sessionId)
                .roomTypeId(roomTypeId)
                .checkInDate(checkIn)
                .checkOutDate(checkOut)
                .numberOfNights(nights)
                .numberOfRooms(numberOfRooms)
                .adults(adults)
                .children(children)
                .roomPrice(currentPrice)
                .totalRoomPrice(totalRoomPrice)
                .serviceFee(serviceFee)
                .vat(vat)
                .totalAmount(totalAmount)
                .depositAmount(depositAmount)
                .remainingAmount(remainingAmount)
                .includeBreakfast(false)
                .breakfastFee(BigDecimal.ZERO)
                .includeSpa(false)
                .spaFee(BigDecimal.ZERO)
                .includeAirportTransfer(false)
                .airportTransferFee(BigDecimal.ZERO)
                .build();
    }

    /**
     * Tính lại giá khi chọn dịch vụ
     */
    public BookingSessionDTO recalculatePrice(BookingSessionDTO session) {
        BigDecimal totalServices = BigDecimal.ZERO;

        // Tính breakfast
        if (Boolean.TRUE.equals(session.getIncludeBreakfast())) {
            int totalGuests = session.getAdults() + session.getChildren();
            BigDecimal breakfastFee = BREAKFAST_FEE_PER_PERSON
                    .multiply(BigDecimal.valueOf(totalGuests))
                    .multiply(BigDecimal.valueOf(session.getNumberOfNights()));
            session.setBreakfastFee(breakfastFee);
            totalServices = totalServices.add(breakfastFee);
        } else {
            session.setBreakfastFee(BigDecimal.ZERO);
        }

        // Tính spa
        if (Boolean.TRUE.equals(session.getIncludeSpa())) {
            int totalGuests = session.getAdults() + session.getChildren();
            BigDecimal spaFee = SPA_FEE_PER_PERSON
                    .multiply(BigDecimal.valueOf(totalGuests));
            session.setSpaFee(spaFee);
            totalServices = totalServices.add(spaFee);
        } else {
            session.setSpaFee(BigDecimal.ZERO);
        }

        // Tính airport transfer
        if (Boolean.TRUE.equals(session.getIncludeAirportTransfer())) {
            session.setAirportTransferFee(AIRPORT_TRANSFER_FEE);
            totalServices = totalServices.add(AIRPORT_TRANSFER_FEE);
        } else {
            session.setAirportTransferFee(BigDecimal.ZERO);
        }

        // Tính lại tổng
        BigDecimal subtotal = session.getTotalRoomPrice().add(totalServices);
        BigDecimal serviceFee = subtotal.multiply(SERVICE_FEE_RATE);
        BigDecimal vat = subtotal.add(serviceFee).multiply(VAT_RATE);
        BigDecimal totalAmount = subtotal.add(serviceFee).add(vat);
        BigDecimal depositAmount = totalAmount.multiply(new BigDecimal("0.5"));
        BigDecimal remainingAmount = totalAmount.subtract(depositAmount);

        session.setServiceFee(serviceFee);
        session.setVat(vat);
        session.setTotalAmount(totalAmount);
        session.setDepositAmount(depositAmount);
        session.setRemainingAmount(remainingAmount);

        return session;
    }

    /**
     * Tạo booking từ session
     */

    public RoomBookingEntity createBooking(
            BookingSessionDTO session,
            Boolean isDepositOnly,
            PaymentMethod paymentMethod) {

        RoomTypeEntity roomType = roomTypeRepository.findById(session.getRoomTypeId())
                .orElseThrow(() -> new EntityNotFoundException("Room type not found"));

        List<RoomEntity> availableRooms = roomRepository.findAvailableRoomsByTypeAndDateRange(
                session.getRoomTypeId(),
                session.getCheckInDate(),
                session.getCheckOutDate(),
                RoomStatus.AVAILABLE,
                List.of(BookingStatus.CANCELLED, BookingStatus.NO_SHOW)
        );

        if (availableRooms.size() < session.getNumberOfRooms()) {
            throw new RuntimeException("Không đủ phòng trống để đặt.");
        }

        RoomEntity assignedRoom = availableRooms.get(0);
        assignedRoom.setStatus(RoomStatus.AVAILABLE);
        roomRepository.save(assignedRoom);

        // 🔹 Tạo booking entity
        RoomBookingEntity booking = RoomBookingEntity.builder()
                .roomType(roomType)
                .room(assignedRoom)
                .branch(roomType.getBranch())
                .checkInDate(session.getCheckInDate())
                .checkOutDate(session.getCheckOutDate())
                .numberOfRooms(session.getNumberOfRooms())
                .adults(session.getAdults())
                .children(session.getChildren())
                .guestName(session.getGuestName())
                .guestEmail(session.getGuestEmail())
                .guestPhone(session.getGuestPhone())
                .guestIdNumber(session.getGuestIdNumber())
                .roomPrice(session.getRoomPrice())
                .basePrice(session.getRoomPrice())
                .numberOfNights(session.getNumberOfNights())
                .totalRoomPrice(session.getTotalRoomPrice())
                .serviceFee(session.getServiceFee())
                .vat(session.getVat())
                .totalAmount(session.getTotalAmount())
                .depositAmount(session.getDepositAmount())
                .remainingAmount(session.getRemainingAmount())
                .includeBreakfast(session.getIncludeBreakfast())
                .breakfastFee(session.getBreakfastFee())
                .includeSpa(session.getIncludeSpa())
                .spaFee(session.getSpaFee())
                .includeAirportTransfer(session.getIncludeAirportTransfer())
                .airportTransferFee(session.getAirportTransferFee())
                .specialRequests(session.getSpecialRequests())
                .status(BookingStatus.PENDING)
                .paymentMethod(paymentMethod)
                .build();

        // ✅ SỬA: Set paymentStatus và remainingAmount dựa vào isDepositOnly
        if (isDepositOnly) {
            booking.setPaymentStatus(PaymentStatus.PARTIALLY_PAID);
            booking.setRemainingAmount(session.getRemainingAmount());  // Giữ nguyên remaining
        } else {
            booking.setPaymentStatus(PaymentStatus.PAID);
            booking.setRemainingAmount(BigDecimal.ZERO);  // ✅ Set về 0 khi thanh toán full
        }

        booking = bookingRepository.save(booking);

        // 🔹 Tạo payment record
        createPaymentRecord(booking, isDepositOnly);

        // 🔹 Gửi email xác nhận
        try {
            emailService.sendBookingConfirmation(booking);
        } catch (Exception e) {
            System.err.println("Failed to send confirmation email: " + e.getMessage());
        }

        // ✅ THÊM log để debug
        System.out.println(">>> Created booking:");
        System.out.println(">>> - Booking code: " + booking.getBookingCode());
        System.out.println(">>> - isDepositOnly: " + isDepositOnly);
        System.out.println(">>> - Total amount: " + booking.getTotalAmount());
        System.out.println(">>> - Remaining amount: " + booking.getRemainingAmount());
        System.out.println(">>> - Payment status: " + booking.getPaymentStatus());

//        // ✅ THÊM: Xóa draft nếu booking từ chatbot
//        try {
//            draftRepository.findBySessionId(session.getSessionId())
//                    .ifPresent(draft -> {
//                        draftRepository.delete(draft);
//                        System.out.println("✅ Deleted draft: " + draft.getDraftCode());
//                    });
//        } catch (Exception e) {
//            // Không cần throw lỗi nếu xóa draft fail
//            System.err.println("⚠️ Could not delete draft: " + e.getMessage());
//        }

        // ✅ SỬA: Xóa draft nếu sessionId là draft code (bắt đầu bằng DRAFT)
        if (session.getSessionId() != null &&
                session.getSessionId().startsWith("DRAFT")) {
            try {
                draftRepository.findByDraftCode(session.getSessionId())
                        .ifPresent(draft -> {
                            draftRepository.delete(draft);
                            log.info("✅ Deleted draft: {}", draft.getDraftCode());
                        });
            } catch (Exception e) {
                log.warn("⚠️ Could not delete draft: {}", e.getMessage());
            }
        }

        return booking;
    }

    /**
     * Tạo payment record
     */
    private void createPaymentRecord(RoomBookingEntity booking, Boolean isDepositOnly) {
        // ✅ SỬA: Tính đúng payment amount dựa vào isDepositOnly
        BigDecimal paymentAmount = isDepositOnly
                ? booking.getDepositAmount()
                : booking.getTotalAmount();

        PaymentEntity payment = PaymentEntity.builder()
                .roomBooking(booking)
                .amount(paymentAmount)  // ✅ SỬA: Dùng paymentAmount thay vì depositAmount
                .method(booking.getPaymentMethod())
                .status(isDepositOnly ? PaymentStatus.PARTIALLY_PAID : PaymentStatus.PAID)  // ✅ SỬA: Status theo isDepositOnly
                .processedAt(LocalDateTime.now())
                .build();

        paymentRepository.save(payment);

        // ✅ THÊM log để debug
        System.out.println(">>> Created payment record:");
        System.out.println(">>> - isDepositOnly: " + isDepositOnly);
        System.out.println(">>> - Payment amount: " + paymentAmount);
        System.out.println(">>> - Status: " + payment.getStatus());
    }

    public RoomBookingEntity getBookingByCode(String bookingCode) {
        return bookingRepository.findByBookingCode(bookingCode)
                .orElse(null);
    }

    public BookingConfirmationDTO buildConfirmationDTO(RoomBookingEntity booking) {
        return BookingConfirmationDTO.builder()
                .bookingCode(booking.getBookingCode())
                .roomTypeName(booking.getRoomType().getName())
                .branchName(booking.getBranch().getName())
                .branchAddress(booking.getBranch().getAddress())
                .checkInDate(booking.getCheckInDate())
                .checkOutDate(booking.getCheckOutDate())
                .numberOfNights(booking.getNumberOfNights())
                .numberOfRooms(booking.getNumberOfRooms())
                .adults(booking.getAdults())
                .children(booking.getChildren())
                .guestName(booking.getGuestName())
                .guestEmail(booking.getGuestEmail())
                .guestPhone(booking.getGuestPhone())
                .totalAmount(booking.getTotalAmount())
                .depositAmount(booking.getDepositAmount())
                .remainingAmount(booking.getRemainingAmount())
                .paymentMethod(booking.getPaymentMethod().name())
                .paymentStatus(booking.getPaymentStatus())
                .includeBreakfast(booking.getIncludeBreakfast())
                .includeSpa(booking.getIncludeSpa())
                .includeAirportTransfer(booking.getIncludeAirportTransfer())
                .specialRequests(booking.getSpecialRequests())
                .cancellationPolicy("Miễn phí hủy trước 24 giờ. Hủy trong vòng 24 giờ: Tính phí 50%. No-show: Tính 100%.")
                .checkInPolicy("Check-in: 15:00 | Check-out: 12:00")
                .build();
    }


    @Transactional
    public void updatePaymentStatus(String bookingCode, boolean success) {
        RoomBookingEntity booking = bookingRepository.findByBookingCode(bookingCode)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy booking với mã: " + bookingCode));

        if (success) {
            // ✅ SỬA: Kiểm tra remainingAmount thay vì depositAmount
            if (booking.getRemainingAmount().compareTo(BigDecimal.ZERO) > 0) {
                booking.setPaymentStatus(PaymentStatus.PARTIALLY_PAID);
            } else {
                booking.setPaymentStatus(PaymentStatus.PAID);
            }
        } else {
            booking.setPaymentStatus(PaymentStatus.FAILED);
        }

        bookingRepository.save(booking);

        // ✅ THÊM log để debug
        System.out.println(">>> updatePaymentStatus called:");
        System.out.println(">>> - Booking code: " + bookingCode);
        System.out.println(">>> - Success: " + success);
        System.out.println(">>> - Remaining amount: " + booking.getRemainingAmount());
        System.out.println(">>> - Payment status set to: " + booking.getPaymentStatus());
    }


//    này là của con chatbot booking draft thôi không phải booking service
    // ✅ THÊM vào BookingService

    public BookingSessionDTO createBookingSessionFromDraft(ChatbotBookingDraftEntity draft) {
        RoomTypeEntity roomType = draft.getRoomType();
        int nights = (int) ChronoUnit.DAYS.between(
                draft.getCheckInDate(),
                draft.getCheckOutDate()
        );

        BigDecimal currentPrice = promotionService.calculateDiscountedPrice(
                roomType.getPrice(),
                roomType.getBranch().getId(),
                PromotionApplicability.ROOM
        );

        BigDecimal totalRoomPrice = currentPrice
                .multiply(BigDecimal.valueOf(nights))
                .multiply(BigDecimal.valueOf(draft.getNumberOfRooms()));

        BigDecimal serviceFee = totalRoomPrice.multiply(SERVICE_FEE_RATE);
        BigDecimal subtotal = totalRoomPrice.add(serviceFee);
        BigDecimal vat = subtotal.multiply(VAT_RATE);
        BigDecimal totalAmount = subtotal.add(vat);
        BigDecimal depositAmount = totalAmount.multiply(new BigDecimal("0.5"));

        return BookingSessionDTO.builder()
                .sessionId(draft.getDraftCode())
                .sessionId(UUID.randomUUID().toString())
                .roomTypeId(roomType.getId())
                .checkInDate(draft.getCheckInDate())
                .checkOutDate(draft.getCheckOutDate())
                .numberOfNights(nights)
                .numberOfRooms(draft.getNumberOfRooms())
                .adults(draft.getAdults())
                .children(draft.getChildren())
                .guestName(draft.getGuestName())
                .guestEmail(draft.getGuestEmail())
                .guestPhone(draft.getGuestPhone())
                .guestIdNumber(draft.getGuestIdNumber())
                .specialRequests(draft.getSpecialRequests())
                .roomPrice(currentPrice)
                .totalRoomPrice(totalRoomPrice)
                .serviceFee(serviceFee)
                .vat(vat)
                .totalAmount(totalAmount)
                .depositAmount(depositAmount)
                .remainingAmount(totalAmount.subtract(depositAmount))
                .includeBreakfast(draft.getIncludeBreakfast())
                .includeSpa(draft.getIncludeSpa())
                .includeAirportTransfer(draft.getIncludeAirportTransfer())

                // ✅ THÊM: Các thông tin khách từ draft
                .guestName(draft.getGuestName())
                .guestEmail(draft.getGuestEmail())
                .guestPhone(draft.getGuestPhone())
                .guestIdNumber(draft.getGuestIdNumber())
                .specialRequests(draft.getSpecialRequests())

                .build();
    }


    /**
     * ✅ SỬA: Tạo booking từ draft code
     * Sử dụng createBookingSessionFromDraft() existing
     * Sau đó gọi createBooking() để tạo booking
     */
    @Transactional
    public RoomBookingEntity createBookingFromDraft(
            String draftCode,
            Boolean isDepositOnly,
            PaymentMethod paymentMethod) {

        log.info("=== CREATE BOOKING FROM DRAFT ===");
        log.info("Draft code: {}", draftCode);

        // 1. Tìm draft
        ChatbotBookingDraftEntity draft = draftRepository.findByDraftCode(draftCode)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy draft: " + draftCode));

        // 2. Check expired
        if (draft.getExpiresAt().isBefore(LocalDateTime.now())) {
            draftRepository.delete(draft);
            throw new RuntimeException("Draft đã hết hạn. Vui lòng đặt phòng lại.");
        }

        // 3. Check draft có đủ thông tin không
        if (draft.getGuestName() == null ||
                draft.getGuestEmail() == null ||
                draft.getGuestPhone() == null) {
            throw new RuntimeException("Thông tin đặt phòng chưa đầy đủ");
        }

        // 4. Tạo BookingSessionDTO từ draft (SỬ DỤNG METHOD EXISTING)
        BookingSessionDTO session = createBookingSessionFromDraft(draft);

        // ✅ QUAN TRỌNG: Set sessionId = draft code để tracking
        session.setSessionId(draft.getDraftCode());

        log.info("Created session from draft. Total: {}", session.getTotalAmount());

        // 5. Tạo booking từ session (SỬ DỤNG METHOD EXISTING)
        RoomBookingEntity booking = createBooking(session, isDepositOnly, paymentMethod);

        log.info("✅ Created booking {} from draft {}", booking.getBookingCode(), draftCode);

        // 6. ✅ XÓA DRAFT SAU KHI TẠO BOOKING THÀNH CÔNG
        try {
            draftRepository.delete(draft);
            log.info("✅ Deleted draft {} after creating booking {}", draftCode, booking.getId());
        } catch (Exception e) {
            log.warn("⚠️ Failed to delete draft {}: {}", draftCode, e.getMessage());
            // Không throw lỗi vì booking đã tạo thành công
        }

        return booking;
    }

    /**
     * ✅ THÊM: Cleanup tất cả draft đã expired
     * Gọi method này trong scheduled task hoặc khi cần
     */
    @Transactional
    public void cleanupExpiredDrafts() {
        LocalDateTime now = LocalDateTime.now();
        List<ChatbotBookingDraftEntity> expiredDrafts =
                draftRepository.findByExpiresAtBefore(now);

        if (!expiredDrafts.isEmpty()) {
            draftRepository.deleteAll(expiredDrafts);
            log.info("🗑️ Cleaned up {} expired drafts", expiredDrafts.size());
        }
    }

    /**
     * ✅ THÊM: Xóa draft theo code (khi user cancel)
     */
    @Transactional
    public void cancelDraft(String draftCode) {
        draftRepository.findByDraftCode(draftCode).ifPresent(draft -> {
            draftRepository.delete(draft);
            log.info("🗑️ Cancelled draft {}", draftCode);
        });
    }
}