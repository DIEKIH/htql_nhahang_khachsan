package com.example.htql_nhahang_khachsan.dto;

import com.example.htql_nhahang_khachsan.enums.RoomStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomResponse {
    private Long id;
    private Long roomTypeId;
    private String roomTypeName;
    private String roomNumber;
    private Integer floor;
    private RoomStatus status;
    private String notes;
    private LocalDateTime lastCleaned;
    private LocalDateTime createdAt;

    // Helper methods
    public String getStatusDisplay() {
        switch (status) {
            case AVAILABLE: return "Có thể thuê";
            case OCCUPIED: return "Đang thuê";
            case MAINTENANCE: return "Bảo trì";
            case OUT_OF_ORDER: return "Không hoạt động";
            case CLEANING: return "Đang dọn dẹp";
            default: return status.name();
        }
    }

    public String getStatusColor() {
        switch (status) {
            case AVAILABLE: return "success";
            case OCCUPIED: return "danger";
            case MAINTENANCE: return "warning";
            case OUT_OF_ORDER: return "secondary";
            case CLEANING: return "info";
            default: return "secondary";
        }
    }

    public boolean isActive() {
        return status != RoomStatus.OUT_OF_ORDER;
    }
}