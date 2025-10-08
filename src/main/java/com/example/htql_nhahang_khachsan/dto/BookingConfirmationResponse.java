package com.example.htql_nhahang_khachsan.dto;

import com.example.htql_nhahang_khachsan.enums.BookingStatus;
import com.example.htql_nhahang_khachsan.enums.PaymentMethod;
import com.example.htql_nhahang_khachsan.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingConfirmationResponse {
    private String bookingCode;
    private BookingStatus status;
    private PaymentStatus paymentStatus;

    // Thông tin phòng
    private String roomTypeName;
    private String branchName;
    private String branchAddress;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer numberOfNights;
    private Integer numberOfRooms;
    private Integer adults;
    private Integer children;

    // Thông tin khách
    private String guestName;
    private String guestEmail;
    private String guestPhone;

    // Chi phí
    private BigDecimal totalAmount;
    private BigDecimal depositAmount;
    private BigDecimal remainingAmount;
    private PaymentMethod paymentMethod;

    // Dịch vụ
    private Boolean includeBreakfast;
    private Boolean includeSpa;
    private Boolean includeAirportTransfer;
    private String specialRequests;

    // Thời gian
    private LocalDateTime bookingDate;
    private LocalDateTime confirmedAt;

    // Chính sách
    private String cancellationPolicy;
    private String checkInPolicy;
}