package com.example.htql_nhahang_khachsan.controller;

import com.example.htql_nhahang_khachsan.dto.LoginRequest;
import com.example.htql_nhahang_khachsan.dto.RegisterRequest;
import com.example.htql_nhahang_khachsan.dto.UserResponse;
import com.example.htql_nhahang_khachsan.enums.UserRole;
import com.example.htql_nhahang_khachsan.service.AuthService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    // Trang chủ - redirect theo role
//    @GetMapping("/index")
//    public String home(HttpSession session) {
//        if (authService.isLoggedIn(session)) {
//            UserRole role = (UserRole) session.getAttribute("userRole");
//            switch (role) {
//                case ADMIN:
//                    return "redirect:/admin/dashboard";
//                case MANAGER:
//                    return "redirect:/manager/dashboard";
//                case STAFF:
//                    return "redirect:/staff/dashboard";
//                case CASHIER_RESTAURANT:
//                    return "redirect:/cashier-restaurant/dashboard";
//                case CASHIER_HOTEL:
//                    return "redirect:/cashier-hotel/dashboard";
//                case CUSTOMER:
//                    return "redirect:/customer/dashboard";
//            }
//        }
//        return "index";
//    }

    // === ADMIN LOGIN/REGISTER ===
    @GetMapping("/admin/login")
    public String adminLoginPage(Model model, HttpSession session) {
        if (authService.isLoggedIn(session) && authService.isAdmin(session)) {
            return "redirect:/admin/dashboard";
        }
        model.addAttribute("loginRequest", new LoginRequest());
        return "admin/login";
    }

    @PostMapping("/admin/login")
    public String adminLogin(@Valid @ModelAttribute LoginRequest request,
                             BindingResult result,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "admin/login";
        }

        try {
            UserResponse user = authService.login(request, session);
            if (user.getRole() != UserRole.ADMIN) {
                redirectAttributes.addFlashAttribute("error", "Bạn không có quyền truy cập trang Admin");
                return "redirect:/admin/login";
            }
            return "redirect:/admin/dashboard";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/login";
        }
    }

    @GetMapping("/admin/register")
    public String adminRegisterPage(Model model, HttpSession session) {
        if (authService.isLoggedIn(session) && authService.isAdmin(session)) {
            return "redirect:/admin/dashboard";
        }
        model.addAttribute("registerRequest", new RegisterRequest());
        return "admin/register";
    }

    @PostMapping("/admin/register")
    public String adminRegister(@Valid @ModelAttribute RegisterRequest request,
                                BindingResult result,
                                RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "admin/register";
        }

        try {
            authService.register(request, UserRole.ADMIN);
            redirectAttributes.addFlashAttribute("success", "Đăng ký thành công! Vui lòng đăng nhập.");
            return "redirect:/admin/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/register";
        }
    }

    // === MANAGER LOGIN ===
    @GetMapping("/manager/login")
    public String managerLoginPage(Model model, HttpSession session) {
        if (authService.isLoggedIn(session) && authService.isManager(session)) {
            return "redirect:/manager/dashboard";
        }
        model.addAttribute("loginRequest", new LoginRequest());
        return "manager/login";
    }

    @PostMapping("/manager/login")
    public String managerLogin(@Valid @ModelAttribute LoginRequest request,
                               BindingResult result,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "manager/login";
        }

        try {
            UserResponse user = authService.login(request, session);
            if (user.getRole() != UserRole.MANAGER) {
                redirectAttributes.addFlashAttribute("error", "Bạn không có quyền truy cập trang Manager");
                return "redirect:/manager/login";
            }
            return "redirect:/manager/dashboard";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/manager/login";
        }
    }

    // === STAFF LOGIN ===
    @GetMapping("/staff/login")
    public String staffLoginPage(Model model, HttpSession session) {
        if (authService.isLoggedIn(session) && authService.isStaff(session)) {
            return "redirect:/staff/dashboard";
        }
        model.addAttribute("loginRequest", new LoginRequest());
        return "staff/login";
    }

    @PostMapping("/staff/login")
    public String staffLogin(@Valid @ModelAttribute LoginRequest request,
                             BindingResult result,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "staff/login";
        }

        try {
            UserResponse user = authService.login(request, session);
            if (user.getRole() != UserRole.STAFF) {
                redirectAttributes.addFlashAttribute("error", "Bạn không có quyền truy cập trang Staff");
                return "redirect:/staff/login";
            }
            return "redirect:/staff/dashboard";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/staff/login";
        }
    }

    // === CASHIER RESTAURANT LOGIN ===
//    @GetMapping("/cashier-restaurant/login")
//    public String cashierRestaurantLoginPage(Model model, HttpSession session) {
//        if (authService.isLoggedIn(session) && authService.isCashierRestaurant(session)) {
//            return "redirect:/cashier-restaurant/dashboard";
//        }
//        model.addAttribute("loginRequest", new LoginRequest());
//        return "staff/cashier_hotel/login";
//    }
//
//    @PostMapping("/cashier-restaurant/login")
//    public String cashierRestaurantLogin(@Valid @ModelAttribute LoginRequest request,
//                                         BindingResult result,
//                                         HttpSession session,
//                                         RedirectAttributes redirectAttributes) {
//        if (result.hasErrors()) {
//            return "staff/cashier_hotel/login";
//        }
//
//        try {
//            UserResponse user = authService.login(request, session);
//            if (user.getRole() != UserRole.CASHIER_RESTAURANT) {
//                redirectAttributes.addFlashAttribute("error", "Bạn không có quyền truy cập trang Thu ngân nhà hàng");
//                return "redirect:/cashier-restaurant/login";
//            }
//            return "redirect:/cashier-restaurant/dashboard";
//        } catch (Exception e) {
//            redirectAttributes.addFlashAttribute("error", e.getMessage());
//            return "redirect:/cashier-restaurant/login";
//        }
//    }

    // === CASHIER HOTEL LOGIN ===
    @GetMapping("/cashier-hotel/login")
    public String cashierHotelLoginPage(Model model, HttpSession session) {
        if (authService.isLoggedIn(session) && authService.isCashierHotel(session)) {
            return "redirect:/cashier-hotel/dashboard";
        }
        model.addAttribute("loginRequest", new LoginRequest());
        return "staff/cashier_hotel/login";
    }

    @PostMapping("/cashier-hotel/login")
    public String cashierHotelLogin(@Valid @ModelAttribute LoginRequest request,
                                    BindingResult result,
                                    HttpSession session,
                                    RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "staff/cashier_hotel/login";
        }

        try {
            UserResponse user = authService.login(request, session);
            if (user.getRole() != UserRole.CASHIER_HOTEL) {
                redirectAttributes.addFlashAttribute("error", "Bạn không có quyền truy cập trang Thu ngân khách sạn");
                return "redirect:/cashier-hotel/login";
            }
            return "redirect:/cashier-hotel/dashboard";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/cashier-hotel/login";
        }
    }

    // === CUSTOMER LOGIN/REGISTER ===
    @GetMapping("/customer/login")
    public String customerLoginPage(Model model, HttpSession session) {
        if (authService.isLoggedIn(session) && authService.isCustomer(session)) {
            return "redirect:/";
        }
        model.addAttribute("loginRequest", new LoginRequest());
        return "customer/login";
    }

    @PostMapping("/customer/login")
    public String customerLogin(@Valid @ModelAttribute LoginRequest request,
                                BindingResult result,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "customer/login";
        }

        try {
            UserResponse user = authService.login(request, session);
            if (user.getRole() != UserRole.CUSTOMER) {
                redirectAttributes.addFlashAttribute("error", "Bạn không có quyền truy cập trang Customer");
                return "redirect:/customer/login";
            }
            return "redirect:/";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/customer/login";
        }
    }

    @GetMapping("/customer/register")
    public String customerRegisterPage(Model model, HttpSession session) {
        if (authService.isLoggedIn(session) && authService.isCustomer(session)) {
            return "redirect:/";
        }
        model.addAttribute("registerRequest", new RegisterRequest());
        return "customer/register";
    }

    @PostMapping("/customer/register")
    public String customerRegister(@Valid @ModelAttribute RegisterRequest request,
                                   BindingResult result,
                                   RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "customer/register";
        }

        try {
            authService.register(request, UserRole.CUSTOMER);
            redirectAttributes.addFlashAttribute("success", "Đăng ký thành công! Vui lòng đăng nhập.");
            return "redirect:/customer/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/customer/register";
        }
    }

    // === LOGOUT ===
    @PostMapping("/logout")
    public String logout(HttpSession session) {
        authService.logout(session);
        return "redirect:/";
    }

    // === DASHBOARDS ===
    @GetMapping("/admin/dashboard")
    public String adminDashboard(HttpSession session, Model model) {
        if (!authService.isLoggedIn(session) || !authService.isAdmin(session)) {
            return "redirect:/admin/login";
        }
        model.addAttribute("user", authService.getCurrentUser(session));
        return "admin/dashboard";
    }

    @GetMapping("/manager/dashboard")
    public String managerDashboard(HttpSession session, Model model) {
        if (!authService.isLoggedIn(session) || !authService.isManager(session)) {
            return "redirect:/manager/login";
        }
        model.addAttribute("user", authService.getCurrentUser(session));
        return "manager/dashboard";
    }

    @GetMapping("/staff/dashboard")
    public String staffDashboard(HttpSession session, Model model) {
        if (!authService.isLoggedIn(session) || !authService.isStaff(session)) {
            return "redirect:/staff/login";
        }
        model.addAttribute("user", authService.getCurrentUser(session));
        return "staff/dashboard";
    }

    @GetMapping("/cashier-restaurant/dashboard")
    public String cashierRestaurantDashboard(HttpSession session, Model model) {
        if (!authService.isLoggedIn(session) || !authService.isCashierRestaurant(session)) {
            return "redirect:/cashier-restaurant/login";
        }
        model.addAttribute("user", authService.getCurrentUser(session));
        return "cashier-restaurant/dashboard";
    }

    @GetMapping("/cashier-hotel/dashboard")
    public String cashierHotelDashboard(HttpSession session, Model model) {
        if (!authService.isLoggedIn(session) || !authService.isCashierHotel(session)) {
            return "redirect:/cashier-hotel/login";
        }
        model.addAttribute("user", authService.getCurrentUser(session));
        return "staff/cashier_hotel/dashboard";
    }
}
