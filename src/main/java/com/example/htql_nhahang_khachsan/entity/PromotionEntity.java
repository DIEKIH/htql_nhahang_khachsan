package com.example.htql_nhahang_khachsan.entity;

import com.example.htql_nhahang_khachsan.enums.PromotionApplicability;
import com.example.htql_nhahang_khachsan.enums.PromotionScope;
import com.example.htql_nhahang_khachsan.enums.PromotionType;
import com.example.htql_nhahang_khachsan.enums.Status;
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
@Table(name = "promotions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromotionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    private PromotionType type; // PERCENTAGE, FIXED_AMOUNT, BOGO

    @Column(name = "discount_value", nullable = false)
    private BigDecimal discountValue;

    @Column(name = "min_amount")
    private BigDecimal minAmount; // Số tiền tối thiểu để áp dụng

    @Column(name = "max_discount")
    private BigDecimal maxDiscount; // Giảm tối đa

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(name = "usage_limit")
    private Integer usageLimit; // Số lần sử dụng tối đa

    @Column(name = "used_count")
    private Integer usedCount = 0;

    @Enumerated(EnumType.STRING)
    private PromotionScope scope; // SYSTEM_WIDE, BRANCH_SPECIFIC

    @ManyToOne
    @JoinColumn(name = "branch_id")
    private BranchEntity branch; // null nếu system-wide

    @Enumerated(EnumType.STRING)
    private PromotionApplicability applicability; // ROOM, RESTAURANT, BOTH

    @Enumerated(EnumType.STRING)
    private Status status; // ACTIVE, INACTIVE, EXPIRED

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private Long createdBy; // User ID

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}