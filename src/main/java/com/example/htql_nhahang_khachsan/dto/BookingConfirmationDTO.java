package com.example.htql_nhahang_khachsan.dto;

import com.example.htql_nhahang_khachsan.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingConfirmationDTO {
    private String bookingCode;
    private String roomTypeName;
    private String branchName;
    private String branchAddress;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer numberOfNights;
    private Integer numberOfRooms;
    private Integer adults;
    private Integer children;
    private String guestName;
    private String guestEmail;
    private String guestPhone;
    private BigDecimal totalAmount;
    private BigDecimal depositAmount;
    private BigDecimal remainingAmount;
    private String paymentMethod;
    private PaymentStatus paymentStatus;
    private Boolean includeBreakfast;
    private Boolean includeSpa;
    private Boolean includeAirportTransfer;
    private String specialRequests;
    private String cancellationPolicy;
    private String checkInPolicy;
}
