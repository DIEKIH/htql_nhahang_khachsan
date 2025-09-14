package com.example.htql_nhahang_khachsan.entity;

import com.example.htql_nhahang_khachsan.enums.VoucherStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import jakarta.persistence.Id;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;


@Entity
@Table(name = "vouchers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoucherEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "voucher_code", unique = true, nullable = false)
    private String voucherCode;

    @ManyToOne
    @JoinColumn(name = "promotion_id", nullable = false)
    private PromotionEntity promotion;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private UserEntity customer; // null nếu là public voucher

    @Enumerated(EnumType.STRING)
    private VoucherStatus status; // ACTIVE, USED, EXPIRED, REVOKED

    @Column(name = "issued_date")
    private LocalDateTime issuedDate;

    @Column(name = "used_date")
    private LocalDateTime usedDate;

    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;

    @ManyToOne
    @JoinColumn(name = "used_in_booking")
    private RoomBookingEntity usedInRoomBooking;

    @ManyToOne
    @JoinColumn(name = "used_in_order")
    private OrderEntity usedInOrder;

    @PrePersist
    protected void onCreate() {
        issuedDate = LocalDateTime.now();
        if (voucherCode == null) {
            voucherCode = generateVoucherCode();
        }
    }

    private String generateVoucherCode() {
        return "VC" + System.currentTimeMillis();
    }
}