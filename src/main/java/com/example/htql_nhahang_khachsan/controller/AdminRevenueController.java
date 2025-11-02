package com.example.htql_nhahang_khachsan.controller;

import com.example.htql_nhahang_khachsan.dto.RevenueStatisticsDTO;
import com.example.htql_nhahang_khachsan.service.AdminRevenueService;
import com.example.htql_nhahang_khachsan.service.AuthService;
import com.example.htql_nhahang_khachsan.service.BranchService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller
@RequestMapping("/admin/revenue")
@RequiredArgsConstructor
public class AdminRevenueController {

    private final AuthService authService;
    private final AdminRevenueService revenueService;
    private final BranchService branchService;

    @GetMapping
    public String revenueStatistics(Model model, HttpSession session,
                                    @RequestParam(required = false) Long branchId,
                                    @RequestParam(required = false, defaultValue = "MONTH") String period,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {

        if (!authService.isLoggedIn(session) || !authService.isAdmin(session)) {
            return "redirect:/admin/login";
        }

        // Lấy thống kê doanh thu (tất cả chi nhánh hoặc 1 chi nhánh cụ thể)
        RevenueStatisticsDTO statistics = revenueService.getRevenueStatistics(branchId, period, fromDate, toDate);

        // Lấy danh sách chi nhánh cho dropdown
        model.addAttribute("branches", branchService.getAllBranches());
        model.addAttribute("statistics", statistics);
        model.addAttribute("selectedBranchId", branchId);
        model.addAttribute("selectedPeriod", period);
        model.addAttribute("fromDate", statistics.getFromDate());
        model.addAttribute("toDate", statistics.getToDate());

        return "admin/revenue/statistics";
    }
}