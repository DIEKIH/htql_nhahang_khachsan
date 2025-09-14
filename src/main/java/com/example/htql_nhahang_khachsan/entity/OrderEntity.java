package com.example.htql_nhahang_khachsan.entity;

import com.example.htql_nhahang_khachsan.enums.OrderStatus;
import com.example.htql_nhahang_khachsan.enums.PaymentStatus;
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
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_code", unique = true, nullable = false)
    private String orderCode;

    @ManyToOne
    @JoinColumn(name = "table_booking_id")
    private TableBookingEntity tableBooking;

    @ManyToOne
    @JoinColumn(name = "table_id", nullable = false)
    private RestaurantTableEntity table;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private UserEntity customer; // Có thể null cho walk-in

    @ManyToOne
    @JoinColumn(name = "staff_id", nullable = false)
    private UserEntity staff; // Nhân viên nhận order

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "discount_amount")
    private BigDecimal discountAmount;

    @Column(name = "final_amount")
    private BigDecimal finalAmount;

    @Enumerated(EnumType.STRING)
    private OrderStatus status; // PENDING, CONFIRMED, PREPARING, READY, SERVED, COMPLETED, CANCELLED

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus; // PENDING, PAID, PARTIALLY_PAID

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "order_time")
    private LocalDateTime orderTime;

    @Column(name = "served_time")
    private LocalDateTime servedTime;

    @Column(name = "completed_time")
    private LocalDateTime completedTime;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        orderTime = LocalDateTime.now();
        orderCode = generateOrderCode();
    }

    private String generateOrderCode() {
        return "ORD" + System.currentTimeMillis();
    }
}