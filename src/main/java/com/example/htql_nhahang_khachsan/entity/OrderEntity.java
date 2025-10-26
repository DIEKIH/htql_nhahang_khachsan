package com.example.htql_nhahang_khachsan.entity;

import com.example.htql_nhahang_khachsan.enums.OrderStatus;
import com.example.htql_nhahang_khachsan.enums.OrderType;
import com.example.htql_nhahang_khachsan.enums.PaymentMethod;
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
import java.util.ArrayList;
import java.util.List;

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

    // Thêm để biết order thuộc chi nhánh nào
    @ManyToOne
    @JoinColumn(name = "branch_id")
    private BranchEntity branch;

    // Giữ nguyên phần đặt tại quầy
    @ManyToOne
    @JoinColumn(name = "table_id")
    private RestaurantTableEntity table;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<OrderItemEntity> items = new ArrayList<>();

    // Giữ nguyên: nếu khách hàng có tài khoản
    @ManyToOne
    @JoinColumn(name = "customer_id")
    private UserEntity customer;

    @ManyToOne(optional = true)
    @JoinColumn(name = "staff_id", nullable = true)
    private UserEntity staff;

    @ManyToOne
    @JoinColumn(name = "cashier_id")
    private UserEntity cashier; // người thu ngân



    // ❗️Thêm: hỗ trợ khách hàng không đăng nhập
    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "customer_phone")
    private String customerPhone;

    @Column(name = "customer_address")
    private String customerAddress;

    // Loại đơn: ONLINE hoặc AT_STORE
    @Enumerated(EnumType.STRING)
    @Column(name = "order_type")
    private OrderType orderType;  // enum { ONLINE, AT_STORE }

    // Giữ nguyên phần thanh toán
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus; // PENDING, PAID, PARTIALLY_PAID

    @Column(name = "payment_method")
    private PaymentMethod  paymentMethod; // CASH, VNPAY, MOMO, BANK_TRANSFER

    // Giữ nguyên
    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "discount_amount")
    private BigDecimal discountAmount;

    @Column(name = "final_amount")
    private BigDecimal finalAmount;

    @Enumerated(EnumType.STRING)
    private OrderStatus status; // PENDING, CONFIRMED, PREPARING, READY, SERVED, DELIVERING, COMPLETED, CANCELLED

    @Column(name = "order_time")
    private LocalDateTime orderTime;

    @Column(name = "completed_time")
    private LocalDateTime completedTime;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        orderTime = LocalDateTime.now();
        orderCode = "ORD" + System.currentTimeMillis();
    }
}
