package com.example.htql_nhahang_khachsan.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "branch_view_history",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "branch_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchViewHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    private UserEntity user; // null nếu chưa đăng nhập

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private BranchEntity branch;

    @Column(name = "session_id")
    private String sessionId; // Dùng cho người chưa đăng nhập

    @Column(name = "view_count")
    private Integer viewCount = 1;

    @Column(name = "last_viewed_at", nullable = false)
    private LocalDateTime lastViewedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (lastViewedAt == null) {
            lastViewedAt = LocalDateTime.now();
        }
    }

    public void incrementViewCount() {
        this.viewCount++;
        this.lastViewedAt = LocalDateTime.now();
    }
}