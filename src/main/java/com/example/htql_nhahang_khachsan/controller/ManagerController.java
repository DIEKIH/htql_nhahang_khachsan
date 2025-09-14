package com.example.htql_nhahang_khachsan.controller;

import com.example.htql_nhahang_khachsan.dto.UserResponse;
import com.example.htql_nhahang_khachsan.service.AuthService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

//@Controller
//@RequestMapping("/manager")
//@RequiredArgsConstructor
//public class ManagerController {
//
//    private final AuthService authService;
//
//    @GetMapping("/dashboard")
//    public String managerDashboard(HttpSession session, Model model) {
//        if (!authService.isLoggedIn(session) || !authService.isManager(session)) {
//            return "redirect:/manager/login";
//        }
//
//
//        model.addAttribute("currentPage", "dashboard");
//        return "manager/dashboard";
//    }
//}