package com.example.htql_nhahang_khachsan.entity;

import com.example.htql_nhahang_khachsan.enums.MessageSender;
import com.example.htql_nhahang_khachsan.enums.MessageType;
import com.example.htql_nhahang_khachsan.enums.ShiftStatus;
import com.example.htql_nhahang_khachsan.enums.ShiftType;
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
@Table(name = "chat_messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "conversation_id", nullable = false)
    private ChatConversationEntity conversation;

    @ManyToOne
    @JoinColumn(name = "sender_id")
    private UserEntity sender; // null nếu là AI

    @Enumerated(EnumType.STRING)
    private MessageSender senderType; // CUSTOMER, STAFF, AI

    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;

    @Enumerated(EnumType.STRING)
    private MessageType messageType; // TEXT, IMAGE, FILE, QUICK_REPLY, BOOKING_LINK

    @Column(name = "file_url")
    private String fileUrl; // Cho attachment

    @Column(name = "is_read")
    private Boolean isRead = false;

    @Column(name = "ai_intent")
    private String aiIntent; // Ý định được AI phân tích

    @Column(name = "ai_confidence")
    private Double aiConfidence; // Độ tin cậy của AI

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
