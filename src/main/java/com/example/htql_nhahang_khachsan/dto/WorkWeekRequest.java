package com.example.htql_nhahang_khachsan.dto;

import com.example.htql_nhahang_khachsan.enums.ShiftType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkWeekRequest {

    @NotNull(message = "Vui lòng chọn nhân viên")
    private Long staffId;

    @NotNull(message = "Vui lòng chọn ngày bắt đầu tuần")
    private LocalDate weekStart;

    // Map: DayOfWeek -> ShiftType (MONDAY -> MORNING, TUESDAY -> AFTERNOON, etc.)
    private Map<DayOfWeek, ShiftType> weekSchedule;

    // Giờ làm việc cho từng loại ca
    private Map<ShiftType, TimeRange> shiftTimes;

    private String notes;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TimeRange {
        private LocalTime startTime;
        private LocalTime endTime;
    }
}