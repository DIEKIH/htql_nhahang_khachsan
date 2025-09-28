package com.example.htql_nhahang_khachsan.dto;

import com.example.htql_nhahang_khachsan.enums.RoomStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomDetailResponse {
    private Long id;
    private String roomNumber;
    private Integer floor;
    private RoomStatus status;
    private String statusDisplay;
    private String notes;
    private LocalDateTime lastCleaned;
    private LocalDateTime createdAt;

    // Room type information
    private RoomTypeResponse roomType;

    // Current booking information (if occupied)
    private CurrentBookingInfo currentBooking;

    // Maintenance history
    private List<MaintenanceRecord> maintenanceHistory;
}
