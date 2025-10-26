package com.example.htql_nhahang_khachsan.entity;

import com.example.htql_nhahang_khachsan.enums.BookingStatus;
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
@Table(name = "table_bookings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TableBookingEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_code", unique = true, nullable = false)
    private String bookingCode;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private UserEntity customer;


    @Column(name = "customer_name", nullable = false)
    private String customerName; // 👈 Thêm tên khách

    @Column(name = "customer_email", nullable = false)
    private String customerEmail; // 👈 Thêm email khách

//    @ManyToOne
//    @JoinColumn(name = "table_id")
//    private RestaurantTableEntity table; // Có thể null nếu chưa assign

    @ManyToMany
    @JoinTable(
            name = "booking_tables",
            joinColumns = @JoinColumn(name = "booking_id"),
            inverseJoinColumns = @JoinColumn(name = "table_id")
    )
    private List<RestaurantTableEntity> tables = new ArrayList<>();


    @ManyToOne
    @JoinColumn(name = "branch_id", nullable = false)
    private BranchEntity branch;

    @Column(name = "booking_date", nullable = false)
    private LocalDate bookingDate;

    @Column(name = "booking_time", nullable = false)
    private LocalTime bookingTime;

    @Column(name = "party_size", nullable = false)
    private Integer partySize;

    @Column(name = "actual_arrival")
    private LocalDateTime actualArrival;

    @Enumerated(EnumType.STRING)
    private BookingStatus status; // PENDING, CONFIRMED, SEATED, COMPLETED, CANCELLED

    @Column(name = "special_requests", columnDefinition = "TEXT")
    private String specialRequests;

    @Column(name = "contact_phone")
    private String contactPhone;

    @Column(name = "pre_order_total")
    private BigDecimal preOrderTotal;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        bookingCode = generateBookingCode();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    private String generateBookingCode() {
        return "TB" + System.currentTimeMillis();
    }
}