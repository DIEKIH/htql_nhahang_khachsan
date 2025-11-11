package com.example.htql_nhahang_khachsan.dto;

import com.example.htql_nhahang_khachsan.enums.ShiftStatus;
import com.example.htql_nhahang_khachsan.enums.ShiftType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkWeekResponse {
    private Long id;
    private Long staffId;
    private String staffName;
    private String staffPhone;
    private Long branchId;
    private String branchName;
    private LocalDate weekStart;
    private LocalDate weekEnd;
    private String formattedWeekRange;
    private List<ShiftResponse> shifts;
    private LocalDateTime createdAt;
    private String createdByName;
    private Map<DayOfWeek, ShiftResponse> shiftsByDay;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ShiftResponse {
        private Long id;
        private DayOfWeek dayOfWeek;
        private String dayDisplayName;
        private LocalDate date;
        private String formattedDate;
        private ShiftType type;
        private String typeDisplayName;
        private String typeBadgeClass;
        private LocalTime startTime;
        private LocalTime endTime;
        private String formattedTime;
        private ShiftStatus status;
        private String statusDisplayName;
        private String statusBadgeClass;
        private AttendanceInfo attendance;
        private String notes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AttendanceInfo {
        private Long id;
        private LocalDateTime checkIn;
        private LocalDateTime checkOut;
        private String formattedCheckIn;
        private String formattedCheckOut;
        private String attendanceStatus;
        private String attendanceStatusDisplay;
        private String attendanceStatusBadge;
        private Boolean isLate;
        private Boolean isLeftEarly;
        private String notes;
        private String scheduledTime; // ví dụ "08:00 - 16:00"

        // ✅ Thêm 2 trường này:
        private String typeDisplayName;
        private String typeBadgeClass;
    }
}