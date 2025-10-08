package com.example.htql_nhahang_khachsan.dto;

import com.example.htql_nhahang_khachsan.enums.BookingStatus;
import com.example.htql_nhahang_khachsan.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingListDTO {
    private Long id;
    private String bookingCode;
    private String guestName;
    private String guestEmail;
    private String guestPhone;
    private String branchName;
    private String roomTypeName;
    private String roomNumber;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer numberOfNights;
    private BigDecimal totalAmount;
    private PaymentStatus paymentStatus;
    private BookingStatus status;
    private LocalDateTime createdAt;

    public String getFormattedTotalAmount() {
        return String.format("%,d đ", totalAmount.longValue());
    }
}
