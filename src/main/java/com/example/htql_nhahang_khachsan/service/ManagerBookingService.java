package com.example.htql_nhahang_khachsan.service;

import com.example.htql_nhahang_khachsan.dto.BookingDetailDTO;
import com.example.htql_nhahang_khachsan.dto.BookingListDTO;
import com.example.htql_nhahang_khachsan.dto.PaymentDTO;
import com.example.htql_nhahang_khachsan.entity.PaymentEntity;
import com.example.htql_nhahang_khachsan.entity.RoomBookingEntity;
import com.example.htql_nhahang_khachsan.entity.RoomEntity;
import com.example.htql_nhahang_khachsan.enums.BookingStatus;
import com.example.htql_nhahang_khachsan.enums.PaymentStatus;
import com.example.htql_nhahang_khachsan.enums.RoomStatus;
import com.example.htql_nhahang_khachsan.repository.PaymentRepository;
import com.example.htql_nhahang_khachsan.repository.RoomBookingRepository;
import com.example.htql_nhahang_khachsan.repository.RoomRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManagerBookingService {

    private final RoomBookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final PaymentRepository paymentRepository;

    public List<BookingListDTO> searchBookings(Long branchId, String bookingCode,
                                               BookingStatus status, LocalDate fromDate, LocalDate toDate) {
        List<RoomBookingEntity> bookings;

        if (bookingCode != null || status != null || fromDate != null || toDate != null) {
            // Filter logic
            bookings = bookingRepository.findAll().stream()
                    .filter(b -> b.getBranch().getId().equals(branchId))
                    .filter(b -> bookingCode == null || b.getBookingCode().contains(bookingCode))
                    .filter(b -> status == null || b.getStatus() == status)
                    .filter(b -> fromDate == null || !b.getCheckInDate().isBefore(fromDate))
                    .filter(b -> toDate == null || !b.getCheckInDate().isAfter(toDate))
                    .sorted(Comparator.comparing(RoomBookingEntity::getCreatedAt).reversed())
                    .collect(Collectors.toList());
        } else {
            bookings = bookingRepository.findAll().stream()
                    .filter(b -> b.getBranch().getId().equals(branchId))
                    .sorted(Comparator.comparing(RoomBookingEntity::getCreatedAt).reversed())
                    .collect(Collectors.toList());
        }

        return bookings.stream()
                .map(this::convertToListDTO)
                .collect(Collectors.toList());
    }

    public BookingDetailDTO getBookingDetail(Long bookingId, Long branchId) {
        RoomBookingEntity booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found"));

        if (!booking.getBranch().getId().equals(branchId)) {
            throw new SecurityException("Access denied");
        }

        return convertToDetailDTO(booking);
    }

    @Transactional
    public void assignRoom(Long bookingId, Long roomId, Long branchId) {
        RoomBookingEntity booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found"));

        if (!booking.getBranch().getId().equals(branchId)) {
            throw new SecurityException("Access denied");
        }

        RoomEntity room = roomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("Room not found"));

        // Kiểm tra phòng có trống không
        boolean isAvailable = !bookingRepository.existsBookingForRoomInDateRange(
                roomId,
                booking.getCheckInDate(),
                booking.getCheckOutDate(),
                Arrays.asList(BookingStatus.CANCELLED, BookingStatus.NO_SHOW)
        );

        if (!isAvailable) {
            throw new IllegalStateException("Phòng này đã được đặt trong khoảng thời gian này");
        }

        booking.setRoom(room);
        bookingRepository.save(booking);

        log.info("Assigned room {} to booking {}", roomId, bookingId);
    }

    @Transactional
    public void updateBookingStatus(Long bookingId, BookingStatus status, String note, Long branchId) {
        RoomBookingEntity booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found"));

        if (!booking.getBranch().getId().equals(branchId)) {
            throw new SecurityException("Access denied");
        }

        booking.setStatus(status);

        switch (status) {
            case CONFIRMED:
                booking.setConfirmedAt(LocalDateTime.now());
                break;
            case CHECKED_IN:
                booking.setCheckedInAt(LocalDateTime.now());
                if (booking.getRoom() != null) {
                    booking.getRoom().setStatus(RoomStatus.OCCUPIED);
                    roomRepository.save(booking.getRoom());
                }
                break;
            case CHECKED_OUT:
                booking.setCheckedOutAt(LocalDateTime.now());
                if (booking.getRoom() != null) {
                    booking.getRoom().setStatus(RoomStatus.CLEANING);
                    roomRepository.save(booking.getRoom());
                }
                break;
            case CANCELLED:
                booking.setCancelledAt(LocalDateTime.now());
                booking.setCancellationReason(note);
                if (booking.getRoom() != null) {
                    booking.getRoom().setStatus(RoomStatus.AVAILABLE);
                    roomRepository.save(booking.getRoom());
                }
                break;
        }

        bookingRepository.save(booking);
        log.info("Updated booking {} status to {}", bookingId, status);
    }

    public List<PaymentDTO> getPaymentHistory(Long bookingId, Long branchId) {
        RoomBookingEntity booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found"));

        if (!booking.getBranch().getId().equals(branchId)) {
            throw new SecurityException("Access denied");
        }

        return paymentRepository.findByRoomBookingId(bookingId).stream()
                .map(this::convertToPaymentDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void confirmPayment(Long paymentId, Long staffId) {
        PaymentEntity payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new EntityNotFoundException("Payment not found"));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new IllegalStateException("Payment is not pending");
        }

        payment.setStatus(PaymentStatus.PAID);
        payment.setProcessedBy(staffId);
        payment.setProcessedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        // Cập nhật booking payment status
        if (payment.getRoomBooking() != null) {
            RoomBookingEntity booking = payment.getRoomBooking();
            BigDecimal totalPaid = paymentRepository.findByRoomBookingId(booking.getId()).stream()
                    .filter(p -> p.getStatus() == PaymentStatus.PAID)
                    .map(PaymentEntity::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (totalPaid.compareTo(booking.getTotalAmount()) >= 0) {
                booking.setPaymentStatus(PaymentStatus.PAID);
            } else {
                booking.setPaymentStatus(PaymentStatus.PARTIALLY_PAID);
            }
            bookingRepository.save(booking);
        }

        log.info("Confirmed payment {} by staff {}", paymentId, staffId);
    }

    private BookingListDTO convertToListDTO(RoomBookingEntity entity) {
        return BookingListDTO.builder()
                .id(entity.getId())
                .bookingCode(entity.getBookingCode())
                .guestName(entity.getGuestName())
                .guestEmail(entity.getGuestEmail())
                .guestPhone(entity.getGuestPhone())
                .branchName(entity.getBranch().getName())
                .roomTypeName(entity.getRoomType().getName())
                .roomNumber(entity.getRoom() != null ? entity.getRoom().getRoomNumber() : "Chưa gán")
                .checkInDate(entity.getCheckInDate())
                .checkOutDate(entity.getCheckOutDate())
                .numberOfNights(entity.getNumberOfNights())
                .totalAmount(entity.getTotalAmount())
                .paymentStatus(entity.getPaymentStatus())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private BookingDetailDTO convertToDetailDTO(RoomBookingEntity entity) {
        return BookingDetailDTO.builder()
                .id(entity.getId())
                .bookingCode(entity.getBookingCode())
                .guestName(entity.getGuestName())
                .guestEmail(entity.getGuestEmail())
                .guestPhone(entity.getGuestPhone())
                .guestIdNumber(entity.getGuestIdNumber())
                .branchName(entity.getBranch().getName())
                .roomTypeId(entity.getRoomType().getId())
                .roomTypeName(entity.getRoomType().getName())
                .roomId(entity.getRoom() != null ? entity.getRoom().getId() : null)
                .roomNumber(entity.getRoom() != null ? entity.getRoom().getRoomNumber() : null)
                .checkInDate(entity.getCheckInDate())
                .checkOutDate(entity.getCheckOutDate())
                .numberOfRooms(entity.getNumberOfRooms())
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
                .includeBreakfast(entity.getIncludeBreakfast())
                .breakfastFee(entity.getBreakfastFee())
                .includeSpa(entity.getIncludeSpa())
                .spaFee(entity.getSpaFee())
                .includeAirportTransfer(entity.getIncludeAirportTransfer())
                .airportTransferFee(entity.getAirportTransferFee())
                .specialRequests(entity.getSpecialRequests())
                .paymentMethod(entity.getPaymentMethod())
                .paymentStatus(entity.getPaymentStatus())
                .status(entity.getStatus())
                .bookingDate(entity.getBookingDate())
                .confirmedAt(entity.getConfirmedAt())
                .checkedInAt(entity.getCheckedInAt())
                .checkedOutAt(entity.getCheckedOutAt())
                .cancelledAt(entity.getCancelledAt())
                .cancellationReason(entity.getCancellationReason())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private PaymentDTO convertToPaymentDTO(PaymentEntity entity) {
        return PaymentDTO.builder()
                .id(entity.getId())
                .transactionId(entity.getTransactionId())
                .amount(entity.getAmount())
                .method(entity.getMethod())
                .status(entity.getStatus())
                .processedAt(entity.getProcessedAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
