package com.example.htql_nhahang_khachsan.entity;

// ✅ THÊM MỚI - Entity lưu booking tạm từ chatbot
import com.example.htql_nhahang_khachsan.enums.BookingDraftStep;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "chatbot_booking_drafts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatbotBookingDraftEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "draft_code", unique = true, nullable = false)
    private String draftCode; // Mã draft để tracking

    @Column(name = "session_id")
    private String sessionId; // Link với chatbot session

    // Thông tin phòng
    @ManyToOne
    @JoinColumn(name = "room_type_id")
    private RoomTypeEntity roomType;

    @ManyToOne
    @JoinColumn(name = "branch_id")
    private BranchEntity branch;

    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer numberOfRooms;
    private Integer adults;
    private Integer children;

    // Thông tin khách (thu thập dần qua chat)
    private String guestName;
    private String guestEmail;
    private String guestPhone;
    private String guestIdNumber;

    // Dịch vụ bổ sung
    private Boolean includeBreakfast = false;
    private Boolean includeSpa = false;
    private Boolean includeAirportTransfer = false;

    private String specialRequests;

    // Trạng thái hoàn thành các bước
    @Enumerated(EnumType.STRING)
    private BookingDraftStep currentStep; // ROOM_SELECTED, INFO_COLLECTED, SERVICES_SELECTED, READY_TO_PAY

    // Giá tính toán
    private BigDecimal totalAmount;
    private BigDecimal depositAmount;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt; // Draft hết hạn sau 30 phút

    @Column(name = "created_at")
    private LocalDateTime createdAt;




//    @PrePersist
//    protected void onCreate() {
//        createdAt = LocalDateTime.now();
//        expiresAt = LocalDateTime.now().plusMinutes(30);
//        if (draftCode == null) {
//            draftCode = "DRAFT" + System.currentTimeMillis();
//        }
//    }

    // ================================
    //      FIX: DUY NHẤT 1 @PrePersist
    // ================================
    @PrePersist
    protected void onCreate() {

        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }

        if (expiresAt == null) {
            expiresAt = LocalDateTime.now().plusMinutes(30);
        }

        if (draftCode == null) {
            draftCode = "DRAFT" + System.currentTimeMillis();
        }
    }
}

