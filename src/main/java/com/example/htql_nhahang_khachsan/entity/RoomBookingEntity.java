package com.example.htql_nhahang_khachsan.entity;

import com.example.htql_nhahang_khachsan.enums.BookingStatus;
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

@Entity
@Table(name = "room_bookings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomBookingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_code", unique = true, nullable = false)
    private String bookingCode;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private UserEntity customer;

    @ManyToOne
    @JoinColumn(name = "room_type_id", nullable = false)
    private RoomTypeEntity roomType;

    @ManyToOne
    @JoinColumn(name = "room_id")
    private RoomEntity room; // Phòng cụ thể được assign

    @ManyToOne
    @JoinColumn(name = "branch_id", nullable = false)
    private BranchEntity branch;

    @Column(name = "check_in_date", nullable = false)
    private LocalDate checkInDate;

    @Column(name = "check_out_date", nullable = false)
    private LocalDate checkOutDate;

    @Column(name = "number_of_rooms", nullable = false)
    private Integer numberOfRooms = 1;

    @Column(name = "adults", nullable = false)
    private Integer adults;

    @Column(name = "children")
    private Integer children = 0;

    // Thông tin khách hàng
    @Column(name = "guest_name", nullable = false)
    private String guestName;

    @Column(name = "guest_email", nullable = false)
    private String guestEmail;

    @Column(name = "guest_phone", nullable = false)
    private String guestPhone;

    @Column(name = "guest_id_number")
    private String guestIdNumber;

    // Giá và thanh toán
    @Column(name = "room_price", nullable = false)
    private BigDecimal roomPrice; // Giá phòng/đêm

    @Column(name = "base_price")
    private BigDecimal basePrice;

    @Column(name = "number_of_nights", nullable = false)
    private Integer numberOfNights;

    @Column(name = "total_room_price", nullable = false)
    private BigDecimal totalRoomPrice;

    @Column(name = "service_fee")
    private BigDecimal serviceFee = BigDecimal.ZERO;

    @Column(name = "vat")
    private BigDecimal vat = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "deposit_amount")
    private BigDecimal depositAmount; // Tiền đặt cọc 50%

    @Column(name = "remaining_amount")
    private BigDecimal remainingAmount;

    // Dịch vụ bổ sung
    @Column(name = "include_breakfast")
    private Boolean includeBreakfast = false;

    @Column(name = "breakfast_fee")
    private BigDecimal breakfastFee = BigDecimal.ZERO;

    @Column(name = "include_spa")
    private Boolean includeSpa = false;

    @Column(name = "spa_fee")
    private BigDecimal spaFee = BigDecimal.ZERO;

    @Column(name = "include_airport_transfer")
    private Boolean includeAirportTransfer = false;

    @Column(name = "airport_transfer_fee")
    private BigDecimal airportTransferFee = BigDecimal.ZERO;

    @Column(name = "special_requests", columnDefinition = "TEXT")
    private String specialRequests;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    private PaymentMethod paymentMethod;

    @Column(name = "booking_date")
    private LocalDateTime bookingDate;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "checked_in_at")
    private LocalDateTime checkedInAt;

    @Column(name = "checked_out_at")
    private LocalDateTime checkedOutAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        bookingDate = LocalDateTime.now();
        if (bookingCode == null) {
            bookingCode = generateBookingCode();
        }
        if (status == null) {
            status = BookingStatus.PENDING;
        }
        if (paymentStatus == null) {
            paymentStatus = PaymentStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    private String generateBookingCode() {
        return "RB" + System.currentTimeMillis();
    }
}
