package com.example.htql_nhahang_khachsan.entity;


import com.example.htql_nhahang_khachsan.enums.ChatStatus;
import com.example.htql_nhahang_khachsan.enums.ChatType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.Id;
import java.time.LocalDateTime;



@Entity
@Table(name = "chat_conversations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatConversationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private UserEntity customer;

    @ManyToOne
    @JoinColumn(name = "staff_id")
    private UserEntity staff; // null nếu là AI chat

    @ManyToOne
    @JoinColumn(name = "branch_id")
    private BranchEntity branch;

    @Column(name = "session_id", unique = true)
    private String sessionId;

    @Enumerated(EnumType.STRING)
    private ChatType type; // AI_CHAT, HUMAN_CHAT

    @Enumerated(EnumType.STRING)
    private ChatStatus status; // ACTIVE, TRANSFERRED_TO_HUMAN, CLOSED

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @Column(name = "customer_satisfaction")
    private Integer customerSatisfaction; // 1-5 rating sau chat

    @Column(name = "satisfaction_comment", columnDefinition = "TEXT")
    private String satisfactionComment;

    @PrePersist
    protected void onCreate() {
        startedAt = LocalDateTime.now();
        lastMessageAt = LocalDateTime.now();
        if (sessionId == null) {
            sessionId = generateSessionId();
        }
    }

    private String generateSessionId() {
        return "CHAT" + System.currentTimeMillis();
    }
}