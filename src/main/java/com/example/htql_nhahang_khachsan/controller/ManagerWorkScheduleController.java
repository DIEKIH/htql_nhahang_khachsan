package com.example.htql_nhahang_khachsan.controller;

import com.example.htql_nhahang_khachsan.dto.WorkWeekRequest;
import com.example.htql_nhahang_khachsan.dto.WorkWeekResponse;
import com.example.htql_nhahang_khachsan.entity.UserEntity;
import com.example.htql_nhahang_khachsan.enums.ShiftType;
import com.example.htql_nhahang_khachsan.service.AuthService;
import com.example.htql_nhahang_khachsan.service.ManagerWorkScheduleService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/manager")
@RequiredArgsConstructor
public class ManagerWorkScheduleController {

    private final AuthService authService;
    private final ManagerWorkScheduleService workScheduleService;

    @GetMapping("/work-schedules")
    public String workScheduleGanttView(Model model, HttpSession session) {
        if (!authService.isLoggedIn(session) || !authService.isManager(session)) {
            return "redirect:/manager/login";
        }

        Long branchId = authService.getCurrentUserBranchId(session);
        List<UserEntity> staffList = workScheduleService.getStaffByBranch(branchId);

        model.addAttribute("staffList", staffList);
        model.addAttribute("shiftTypes", ShiftType.values());

        return "manager/work_schedules/gantt_view";
    }

    // API Endpoints for Gantt view
    @GetMapping("/work-schedules/api/schedules")
    @ResponseBody
    public ResponseEntity<List<WorkWeekResponse>> getSchedules(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            HttpSession session) {

        if (!authService.isLoggedIn(session) || !authService.isManager(session)) {
            return ResponseEntity.status(401).build();
        }

        Long branchId = authService.getCurrentUserBranchId(session);
        List<WorkWeekResponse> schedules = workScheduleService.getWorkSchedulesByBranchAndDateRange(
                branchId, startDate, endDate
        );

        return ResponseEntity.ok(schedules);
    }

    // Thêm vào ManagerWorkScheduleController.java

    @GetMapping("/work-schedules/add")
    public String addWorkScheduleForm(Model model, HttpSession session) {
        if (!authService.isLoggedIn(session) || !authService.isManager(session)) {
            return "redirect:/manager/login";
        }

        Long branchId = authService.getCurrentUserBranchId(session);
        List<UserEntity> staffList = workScheduleService.getStaffByBranch(branchId);

        model.addAttribute("staffList", staffList);
        model.addAttribute("shiftTypes", ShiftType.values());

        return "manager/work_schedules/add_form";  // tên file HTML
    }

    @PostMapping("/work-schedules/create")
    @ResponseBody
    public ResponseEntity<?> createWorkSchedule(@RequestBody CreateScheduleRequest request, HttpSession session) {
        if (!authService.isLoggedIn(session) || !authService.isManager(session)) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        try {
            Long branchId = authService.getCurrentUserBranchId(session);
            Long managerId = authService.getCurrentUserId(session);

            // Parse the schedule
            WorkWeekRequest workWeekRequest = new WorkWeekRequest();
            workWeekRequest.setStaffId(request.getStaffId());
            workWeekRequest.setWeekStart(request.getWeekStart());

            Map<DayOfWeek, ShiftType> schedule = new HashMap<>();
            Map<ShiftType, WorkWeekRequest.TimeRange> shiftTimes = new HashMap<>();

            // Parse shifts from request
            if (request.getShifts() != null) {
                for (ShiftData shift : request.getShifts()) {
                    schedule.put(shift.getDayOfWeek(), shift.getType());

                    if (shift.getType() != ShiftType.OFF && shift.getStartTime() != null && shift.getEndTime() != null) {
                        shiftTimes.putIfAbsent(shift.getType(),
                                WorkWeekRequest.TimeRange.builder()
                                        .startTime(shift.getStartTime())
                                        .endTime(shift.getEndTime())
                                        .build()
                        );
                    }
                }
            }

            workWeekRequest.setWeekSchedule(schedule);
            workWeekRequest.setShiftTimes(shiftTimes);

            WorkWeekResponse response = workScheduleService.createWorkWeek(branchId, managerId, workWeekRequest);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/work-schedules/{id}/delete")
    @ResponseBody
    public ResponseEntity<?> deleteWorkSchedule(@PathVariable Long id, HttpSession session) {
        if (!authService.isLoggedIn(session) || !authService.isManager(session)) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        try {
            Long branchId = authService.getCurrentUserBranchId(session);
            workScheduleService.deleteWorkWeek(id, branchId);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/work-schedules/attendance")
    @ResponseBody
    public ResponseEntity<?> markAttendance(@RequestBody AttendanceRequest request, HttpSession session) {
        if (!authService.isLoggedIn(session) || !authService.isManager(session)) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        try {
            Long branchId = authService.getCurrentUserBranchId(session);
            LocalDateTime time = request.getTime() != null ? request.getTime() : LocalDateTime.now();

            workScheduleService.markAttendance(
                    request.getShiftId(),
                    branchId,
                    request.isCheckIn(),
                    time,
                    request.getNotes()
            );

            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Request DTOs
    @lombok.Data
    public static class CreateScheduleRequest {
        private Long staffId;
        private LocalDate weekStart;
        private List<ShiftData> shifts;
    }

    @lombok.Data
    public static class ShiftData {
        private DayOfWeek dayOfWeek;
        private ShiftType type;
        private LocalTime startTime;
        private LocalTime endTime;
    }

    @lombok.Data
    public static class AttendanceRequest {
        private Long shiftId;
        private boolean checkIn;
        private LocalDateTime time;
        private String notes;
    }
}