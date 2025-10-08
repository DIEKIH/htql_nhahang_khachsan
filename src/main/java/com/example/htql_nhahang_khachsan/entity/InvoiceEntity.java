package com.example.htql_nhahang_khachsan.entity;

import com.example.htql_nhahang_khachsan.enums.PaymentMethod;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoices")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_code", unique = true, nullable = false)
    private String invoiceCode;

    @ManyToOne
    @JoinColumn(name = "room_booking_id", nullable = false)
    private RoomBookingEntity roomBooking;

    @ManyToOne
    @JoinColumn(name = "payment_id")
    private PaymentEntity payment;

    private BigDecimal subtotal;   // Tổng tiền phòng
    private BigDecimal vat;        // Thuế
    private BigDecimal serviceFee; // Phí dịch vụ
    private BigDecimal total;      // Tổng cộng

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    private LocalDateTime issuedAt;

    private String issuedBy; // Tên nhân viên lập hóa đơn

    @PrePersist
    protected void onCreate() {
        issuedAt = LocalDateTime.now();
        if (invoiceCode == null) {
            invoiceCode = "INV" + System.currentTimeMillis();
        }
    }
}

