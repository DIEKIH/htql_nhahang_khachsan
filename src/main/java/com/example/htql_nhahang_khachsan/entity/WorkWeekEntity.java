package com.example.htql_nhahang_khachsan.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "work_weeks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkWeekEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private UserEntity staff; // nhân viên

    @ManyToOne
    private BranchEntity branch; // chi nhánh

    private LocalDate weekStart; // ngày bắt đầu tuần (vd: 2025-11-03)
    private LocalDate weekEnd;   // ngày kết thúc tuần (vd: 2025-11-09)

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private Long createdBy; // người tạo (manager)

    @OneToMany(mappedBy = "workWeek", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkShiftEntity> shifts;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

