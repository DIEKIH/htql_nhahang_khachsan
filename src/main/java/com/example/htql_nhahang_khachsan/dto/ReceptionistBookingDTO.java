package com.example.htql_nhahang_khachsan.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.example.htql_nhahang_khachsan.enums.PaymentStatus;
import com.example.htql_nhahang_khachsan.enums.BookingStatus;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceptionistBookingDTO {
    private Long id;
    private String bookingCode;
    private String guestName;
    private String guestPhone;
    private String roomTypeName;
    private String roomNumber;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer adults;
    private Integer children;
    private BigDecimal totalAmount;
    private BigDecimal depositAmount;
    private BookingStatus status;
    private PaymentStatus paymentStatus;
    private LocalDateTime createdAt;

    public String getFormattedTotalAmount() {
        return String.format("%,d đ", totalAmount.longValue());
    }

    public String getFormattedDepositAmount() {
        return depositAmount != null ? String.format("%,d đ", depositAmount.longValue()) : "0 đ";
    }
}
