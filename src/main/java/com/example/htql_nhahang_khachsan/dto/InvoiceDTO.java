package com.example.htql_nhahang_khachsan.dto;

import com.example.htql_nhahang_khachsan.enums.PaymentMethod;
import com.example.htql_nhahang_khachsan.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceDTO {
    private Long id;
    private String invoiceCode;
    private String bookingCode;
    private String guestName;
    private String guestPhone;
    private String roomNumber;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer numberOfNights;
    private BigDecimal subtotal;
    private BigDecimal serviceFee;
    private BigDecimal vat;
    private BigDecimal total;
    private PaymentMethod paymentMethod;
    private LocalDateTime issuedAt;
    private String issuedBy;

    private String orderCode;
    private String tableName;
    private String cashierName;
    private String staffName;
    private LocalDateTime orderTime;
    private List<InvoiceItemDTO> items;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private PaymentStatus paymentStatus;
//    private PaymentMethod paymentMethod;
    private String notes;

    public String getFormattedAmount(BigDecimal amount) {
        return amount != null ? String.format("%,d đ", amount.longValue()) : "0 đ";
    }

    public BigDecimal getPricePerNight() {
        if (subtotal != null && numberOfNights != null && numberOfNights > 0) {
            return subtotal.divide(BigDecimal.valueOf(numberOfNights), 2, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO;
    }

    public String getPaymentMethodDisplay() {
        if (paymentMethod == null) return "-";
        return switch (paymentMethod) {
            case CASH -> "Tiền mặt";
            case CREDIT_CARD -> "Thẻ";
            case VNPAY -> "VNPAY";
            default -> "Khác";
        };
    }


}
