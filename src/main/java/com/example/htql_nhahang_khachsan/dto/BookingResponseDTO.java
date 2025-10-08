package com.example.htql_nhahang_khachsan.dto;

import com.example.htql_nhahang_khachsan.enums.BookingStatus;
import com.example.htql_nhahang_khachsan.enums.PaymentMethod;
import com.example.htql_nhahang_khachsan.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponseDTO {
    private Long id;
    private String bookingCode;
    private Long roomTypeId;
    private String roomTypeName;
    private String branchName;
    private String checkInDate;
    private String checkOutDate;
    private Integer numberOfRooms;
    private Integer numberOfAdults;
    private Integer numberOfChildren;
    private Integer totalNights;

    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String specialRequests;

    private BigDecimal basePrice;
    private BigDecimal roomsTotal;
    private BigDecimal serviceFee;
    private BigDecimal vat;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private BigDecimal depositAmount;
    private BigDecimal remainingAmount;

    private String formattedBasePrice;
    private String formattedRoomsTotal;
    private String formattedServiceFee;
    private String formattedVat;
    private String formattedTotalAmount;
    private String formattedDepositAmount;
    private String formattedRemainingAmount;

    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private Boolean isDepositPaid;
    private BookingStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime confirmedAt;
}
