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

    /**
     * Tạo booking session
     */
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

        BigDecimal roomPrice = roomType.getPrice();
//        BigDecimal totalRoomPrice = roomPrice
//                .multiply(BigDecimal.valueOf(nights))
//                .multiply(BigDecimal.valueOf(numberOfRooms));

        // ✅ DÙNG currentPrice thay vì roomType.getPrice()
        BigDecimal totalRoomPrice = currentPrice
                .multiply(BigDecimal.valueOf(nights))
                .multiply(BigDecimal.valueOf(numberOfRooms));

        BigDecimal serviceFee = totalRoomPrice.multiply(SERVICE_FEE_RATE);
        BigDecimal subtotal = totalRoomPrice.add(serviceFee);
        BigDecimal vat = subtotal.multiply(VAT_RATE);
        BigDecimal totalAmount = subtotal.add(vat);
        BigDecimal depositAmount = totalAmount.multiply(new BigDecimal("0.5"));
        BigDecimal remainingAmount = totalAmount.subtract(depositAmount);

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
                // QUAN TRỌNG: Khởi tạo các boolean service với giá trị false
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

        // 🔹 Tìm danh sách phòng trống
//        List<RoomEntity> availableRooms = roomRepository.findAvailableRoomsByTypeAndDateRange(
//                session.getRoomTypeId(),
//                session.getCheckInDate(),
//                session.getCheckOutDate()
//        );

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

        // 🔹 Chọn phòng đầu tiên (hoặc chọn list nếu đặt nhiều)
        RoomEntity assignedRoom = availableRooms.get(0);
        assignedRoom.setStatus(RoomStatus.AVAILABLE);
        roomRepository.save(assignedRoom);

        // 🔹 Tạo booking entity
        RoomBookingEntity booking = RoomBookingEntity.builder()
                .roomType(roomType)
                .room(assignedRoom) // ✅ GÁN PHÒNG THẬT
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

        booking.setPaymentStatus(isDepositOnly
                ? PaymentStatus.PARTIALLY_PAID
                : PaymentStatus.PAID);

        booking = bookingRepository.save(booking);

        // 🔹 Tạo payment record
        createPaymentRecord(booking, isDepositOnly);

        // 🔹 Gửi email xác nhận
        try {
            emailService.sendBookingConfirmation(booking);
        } catch (Exception e) {
            System.err.println("Failed to send confirmation email: " + e.getMessage());
        }

        return booking;
    }

    /**
     * Tạo payment record
     */
    private void createPaymentRecord(RoomBookingEntity booking, Boolean isDepositOnly) {
        BigDecimal paymentAmount = isDepositOnly
                ? booking.getDepositAmount()
                : booking.getTotalAmount();

        PaymentEntity payment = PaymentEntity.builder()
                .roomBooking(booking)
                .amount(paymentAmount)
                .method(booking.getPaymentMethod())
                .status(PaymentStatus.PAID)
                .processedAt(LocalDateTime.now())
                .build();

        paymentRepository.save(payment);
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
            booking.setPaymentStatus(PaymentStatus.PAID);
        } else {
            booking.setPaymentStatus(PaymentStatus.FAILED);
        }

        bookingRepository.save(booking);
    }






}