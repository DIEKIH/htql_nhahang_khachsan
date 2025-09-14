package com.example.htql_nhahang_khachsan.entity;

import com.example.htql_nhahang_khachsan.enums.BackupStatus;
import com.example.htql_nhahang_khachsan.enums.BackupType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.Id;

import java.time.LocalDateTime;



@Entity
@Table(name = "system_backups")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemBackupEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "backup_name", nullable = false)
    private String backupName;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "file_size")
    private Long fileSize; // bytes

    @Enumerated(EnumType.STRING)
    private BackupType type; // FULL, INCREMENTAL, DIFFERENTIAL

    @Enumerated(EnumType.STRING)
    private BackupStatus status; // IN_PROGRESS, COMPLETED, FAILED

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "created_by")
    private Long createdBy; // Admin ID

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @PrePersist
    protected void onCreate() {
        startTime = LocalDateTime.now();
    }
}