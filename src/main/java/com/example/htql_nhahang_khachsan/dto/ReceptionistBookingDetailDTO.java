package com.example.htql_nhahang_khachsan.dto;

import com.example.htql_nhahang_khachsan.enums.PaymentMethod;
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
public class ReceptionistBookingDetailDTO {
    private Long id;
    private String bookingCode;
    private String guestName;
    private String guestEmail;
    private String guestPhone;
    private String guestIdNumber;
    private Long roomTypeId;
    private String roomTypeName;
    private Long roomId;
    private String roomNumber;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer numberOfNights;
    private Integer adults;
    private Integer children;
    private BigDecimal roomPrice;
    private BigDecimal totalRoomPrice;
    private BigDecimal serviceFee;
    private BigDecimal vat;
    private BigDecimal totalAmount;
    private BigDecimal depositAmount;
    private BigDecimal remainingAmount;
    private String specialRequests;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private BookingStatus status;
    private LocalDateTime bookingDate;
    private LocalDateTime checkedInAt;
    private LocalDateTime checkedOutAt;

    public String getFormattedAmount(BigDecimal amount) {
        return amount != null ? String.format("%,d đ", amount.longValue()) : "0 đ";
    }
}
