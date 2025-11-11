package com.example.htql_nhahang_khachsan.controller;

import com.example.htql_nhahang_khachsan.dto.ProfileUpdateRequest;
import com.example.htql_nhahang_khachsan.dto.WorkWeekResponse;
import com.example.htql_nhahang_khachsan.entity.UserEntity;
import com.example.htql_nhahang_khachsan.enums.AttendanceStatus;
import com.example.htql_nhahang_khachsan.service.AuthService;
import com.example.htql_nhahang_khachsan.service.StaffWorkScheduleService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/staff")
@RequiredArgsConstructor
public class StaffController {

    private final AuthService authService;
    private final StaffWorkScheduleService staffWorkScheduleService;

    @GetMapping("/dashboard")
    public String staffDashboard(HttpSession session, Model model) {
        if (!authService.isLoggedIn(session) || !authService.isStaff(session)) {
            return "redirect:/staff/login";
        }

        Long staffId = authService.getCurrentUserId(session);
        UserEntity user = authService.getCurrentUser(session);

        // Thống kê tuần này
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(java.time.DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(6);

        Map<String, Object> stats = staffWorkScheduleService.getWeeklyStats(staffId, weekStart, weekEnd);

        // Ca làm hôm nay
        List<WorkWeekResponse.ShiftResponse> todayShifts = staffWorkScheduleService.getShiftsForDate(staffId, today);

        model.addAttribute("user", user);
        model.addAttribute("stats", stats);
        model.addAttribute("todayShifts", todayShifts);
        model.addAttribute("today", today);

        return "staff/staff/dashboard";
    }

    @GetMapping("/profile")
    public String viewProfile(HttpSession session, Model model) {
        if (!authService.isLoggedIn(session) || !authService.isStaff(session)) {
            return "redirect:/staff/login";
        }

        UserEntity user = authService.getCurrentUser(session);
        model.addAttribute("user", user);

        return "staff/staff/profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@ModelAttribute ProfileUpdateRequest request,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        if (!authService.isLoggedIn(session) || !authService.isStaff(session)) {
            return "redirect:/staff/login";
        }

        try {
            Long staffId = authService.getCurrentUserId(session);
            staffWorkScheduleService.updateProfile(staffId, request);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thông tin thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }

        return "redirect:/staff/profile";
    }

    @GetMapping("/work-schedule")
    public String viewWorkSchedule(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            HttpSession session,
            Model model) {
        if (!authService.isLoggedIn(session) || !authService.isStaff(session)) {
            return "redirect:/staff/login";
        }

        Long staffId = authService.getCurrentUserId(session);
        UserEntity user = authService.getCurrentUser(session);

        // Mặc định là tuần hiện tại
        if (startDate == null) {
            startDate = LocalDate.now().with(java.time.DayOfWeek.MONDAY);
        }

        LocalDate endDate = startDate.plusDays(6);

        List<WorkWeekResponse> schedules = staffWorkScheduleService.getWorkSchedules(staffId, startDate, endDate);

        model.addAttribute("user", user);
        model.addAttribute("schedules", schedules);
        model.addAttribute("weekStart", startDate);
        model.addAttribute("weekEnd", endDate);

        return "staff/staff/work_schedule";
    }

    // API Endpoints
    @GetMapping("/api/work-schedule")
    @ResponseBody
    public ResponseEntity<List<WorkWeekResponse>> getWorkScheduleApi(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            HttpSession session) {
        if (!authService.isLoggedIn(session) || !authService.isStaff(session)) {
            return ResponseEntity.status(401).build();
        }

        Long staffId = authService.getCurrentUserId(session);
        List<WorkWeekResponse> schedules = staffWorkScheduleService.getWorkSchedules(staffId, startDate, endDate);

        return ResponseEntity.ok(schedules);
    }

    @PostMapping("/api/attendance/check-in")
    @ResponseBody
    public ResponseEntity<?> checkIn(@RequestBody CheckInRequest request, HttpSession session) {
        if (!authService.isLoggedIn(session) || !authService.isStaff(session)) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        try {
            Long staffId = authService.getCurrentUserId(session);
            LocalDateTime time = LocalDateTime.now();

            staffWorkScheduleService.checkIn(request.getShiftId(), staffId, time, request.getNotes());

            return ResponseEntity.ok(Map.of("success", true, "message", "Check-in thành công!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/api/attendance/check-out")
    @ResponseBody
    public ResponseEntity<?> checkOut(@RequestBody CheckOutRequest request, HttpSession session) {
        if (!authService.isLoggedIn(session) || !authService.isStaff(session)) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        try {
            Long staffId = authService.getCurrentUserId(session);
            LocalDateTime time = LocalDateTime.now();

            staffWorkScheduleService.checkOut(request.getShiftId(), staffId, time, request.getNotes());

            return ResponseEntity.ok(Map.of("success", true, "message", "Check-out thành công!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/attendance-history")
    public String attendanceHistory(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            HttpSession session,
            Model model) {
        if (!authService.isLoggedIn(session) || !authService.isStaff(session)) {
            return "redirect:/staff/login";
        }

        Long staffId = authService.getCurrentUserId(session);
        UserEntity user = authService.getCurrentUser(session);

        // Mặc định là tháng này
        if (startDate == null) {
            startDate = LocalDate.now().withDayOfMonth(1);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        List<WorkWeekResponse.AttendanceInfo> attendances =
                staffWorkScheduleService.getAttendanceHistory(staffId, startDate, endDate);

        model.addAttribute("user", user);
        model.addAttribute("attendances", attendances);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);


        return "staff/staff/attendance_history";
    }

    // Request DTOs
    @lombok.Data
    public static class CheckInRequest {
        private Long shiftId;
        private String notes;
    }

    @lombok.Data
    public static class CheckOutRequest {
        private Long shiftId;
        private String notes;
    }
}

