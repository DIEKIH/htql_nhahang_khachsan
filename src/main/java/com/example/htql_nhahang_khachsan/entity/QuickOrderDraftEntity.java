package com.example.htql_nhahang_khachsan.entity;

import com.example.htql_nhahang_khachsan.enums.QuickOrderStep;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "chatbot_quick_order_drafts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuickOrderDraftEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "draft_code", nullable = false, unique = true)
    private String draftCode;

    @Column(name = "session_id", nullable = false)
    private String sessionId;

    // ===== THÔNG TIN MÓN ĂN =====
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_item_id", nullable = false)
    private MenuItemEntity menuItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private BranchEntity branch;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "notes")
    private String notes; // Ghi chú món ăn

    // ===== THÔNG TIN KHÁCH HÀNG (thu thập qua chat) =====
    @Column(name = "guest_name", length = 100)
    private String guestName;

    @Column(name = "guest_phone", length = 20)
    private String guestPhone;

    @Column(name = "guest_address", length = 500)
    private String guestAddress;

    @Column(name = "order_notes")
    private String orderNotes; // Ghi chú đơn hàng

    // ===== TRẠNG THÁI =====
    @Enumerated(EnumType.STRING)
    @Column(name = "current_step", nullable = false)
    private QuickOrderStep currentStep;

    // ===== TỔNG TIỀN (tính sẵn) =====
    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount;

    // ===== METADATA =====
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.expiresAt == null) {
            this.expiresAt = LocalDateTime.now().plusHours(1); // Hết hạn sau 1 giờ
        }
        if (this.draftCode == null) {
            this.draftCode = "QOD" + System.currentTimeMillis();
        }
    }

    // ===== HELPER METHODS =====
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }



    // ✅ SỬA: isInfoComplete() - CHECK ĐỦ THÔNG TIN
    public boolean isInfoComplete() {
        boolean nameOk = guestName != null && !guestName.trim().isEmpty() && guestName.length() >= 2;
        boolean phoneOk = guestPhone != null && guestPhone.matches("^0\\d{9,10}$");
        boolean addressOk = guestAddress != null && !guestAddress.trim().isEmpty() && guestAddress.length() >= 10;

        // ✅ THÊM: Log để debug
        System.out.println("=== isInfoComplete CHECK ===");
        System.out.println("Name: '" + guestName + "' -> " + nameOk);
        System.out.println("Phone: '" + guestPhone + "' -> " + phoneOk);
        System.out.println("Address: '" + guestAddress + "' -> " + addressOk);
        System.out.println("Result: " + (nameOk && phoneOk && addressOk));

        return nameOk && phoneOk && addressOk;
    }
}
