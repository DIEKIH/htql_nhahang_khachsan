package com.example.htql_nhahang_khachsan.controller;

import com.example.htql_nhahang_khachsan.dto.RevenueStatisticsDTO;
import com.example.htql_nhahang_khachsan.service.AuthService;
import com.example.htql_nhahang_khachsan.service.ManagerRevenueService;
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
@RequestMapping("/manager/revenue")
@RequiredArgsConstructor
public class ManagerRevenueController {

    private final AuthService authService;
    private final ManagerRevenueService revenueService;

    @GetMapping
    public String revenueStatistics(Model model, HttpSession session,
                                    @RequestParam(required = false, defaultValue = "MONTH") String period,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {

        if (!authService.isLoggedIn(session) || !authService.isManager(session)) {
            return "redirect:/manager/login";
        }

        Long branchId = authService.getCurrentUserBranchId(session);

        RevenueStatisticsDTO statistics = revenueService.getRevenueStatistics(branchId, period, fromDate, toDate);

        model.addAttribute("statistics", statistics);
        model.addAttribute("selectedPeriod", period);
        model.addAttribute("fromDate", statistics.getFromDate());
        model.addAttribute("toDate", statistics.getToDate());

        return "manager/revenue/statistics";
    }

    @GetMapping("/hotel")
    public String hotelRevenue(Model model, HttpSession session,
                               @RequestParam(required = false, defaultValue = "MONTH") String period,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {

        if (!authService.isLoggedIn(session) || !authService.isManager(session)) {
            return "redirect:/manager/login";
        }

        Long branchId = authService.getCurrentUserBranchId(session);

        RevenueStatisticsDTO statistics = revenueService.getRevenueStatistics(branchId, period, fromDate, toDate);

        model.addAttribute("statistics", statistics);
        model.addAttribute("selectedPeriod", period);
        model.addAttribute("fromDate", statistics.getFromDate());
        model.addAttribute("toDate", statistics.getToDate());

        return "manager/revenue/hotel";
    }

    @GetMapping("/restaurant")
    public String restaurantRevenue(Model model, HttpSession session,
                                    @RequestParam(required = false, defaultValue = "MONTH") String period,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {

        if (!authService.isLoggedIn(session) || !authService.isManager(session)) {
            return "redirect:/manager/login";
        }

        Long branchId = authService.getCurrentUserBranchId(session);

        RevenueStatisticsDTO statistics = revenueService.getRevenueStatistics(branchId, period, fromDate, toDate);

        model.addAttribute("statistics", statistics);
        model.addAttribute("selectedPeriod", period);
        model.addAttribute("fromDate", statistics.getFromDate());
        model.addAttribute("toDate", statistics.getToDate());

        return "manager/revenue/restaurant";
    }
}