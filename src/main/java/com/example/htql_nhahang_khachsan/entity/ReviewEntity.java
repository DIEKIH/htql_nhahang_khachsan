package com.example.htql_nhahang_khachsan.entity;

import com.example.htql_nhahang_khachsan.enums.ReviewStatus;
import com.example.htql_nhahang_khachsan.enums.ReviewType;
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
@Table(name = "reviews")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private UserEntity customer;

    @ManyToOne
    @JoinColumn(name = "branch_id", nullable = false)
    private BranchEntity branch;

    @ManyToOne
    @JoinColumn(name = "room_booking_id")
    private RoomBookingEntity roomBooking; // Đánh giá phòng

    @ManyToOne
    @JoinColumn(name = "order_id")
    private OrderEntity order; // Đánh giá nhà hàng

    @Enumerated(EnumType.STRING)
    private ReviewType type; // ROOM, RESTAURANT, SERVICE

    @Column(nullable = false)
    private Integer rating; // 1-5 sao

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "staff_response", columnDefinition = "TEXT")
    private String staffResponse;

    @ManyToOne
    @JoinColumn(name = "responded_by")
    private UserEntity respondedBy; // Staff trả lời

    @Column(name = "response_date")
    private LocalDateTime responseDate;

    @Enumerated(EnumType.STRING)
    private ReviewStatus status; // PENDING, APPROVED, REJECTED

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}