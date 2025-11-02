package com.example.htql_nhahang_khachsan.controller;

import com.example.htql_nhahang_khachsan.dto.*;
import com.example.htql_nhahang_khachsan.enums.*;
import com.example.htql_nhahang_khachsan.service.AuthService;
import com.example.htql_nhahang_khachsan.service.CashierRestaurantService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/cashier-restaurant")
@RequiredArgsConstructor
public class CashierRestaurantController {

    private final CashierRestaurantService cashierService;
    private final AuthService authService;

    @GetMapping("/login")
    public String loginPage(Model model, HttpSession session) {
        if (authService.isLoggedIn(session) && authService.isCashierRestaurant(session)) {
            return "redirect:/cashier-restaurant/dashboard";
        }
        model.addAttribute("loginRequest", new LoginRequest());
        return "staff/cashier_restaurant/login";
    }

    @PostMapping("/login")
    public String login(@Valid @ModelAttribute LoginRequest request,
                        BindingResult result,
                        HttpSession session,
                        RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "staff/cashier_restaurant/login";
        }

        try {
            UserResponse user = authService.login(request, session);
            if (user.getRole() != UserRole.CASHIER_RESTAURANT) {
                redirectAttributes.addFlashAttribute("error", "Bạn không có quyền truy cập");
                return "redirect:/cashier-restaurant/login";
            }
            return "redirect:/cashier-restaurant/dashboard";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/cashier-restaurant/login";
        }
    }

//    @GetMapping("/dashboard")
//    public String dashboard(Model model, HttpSession session) {
//        if (!authService.isLoggedIn(session) || !authService.isCashierRestaurant(session)) {
//            return "redirect:/cashier-restaurant/login";
//        }
//
//        Long branchId = authService.getCurrentUserBranchId(session);
//        Map<String, Object> stats = cashierService.getDashboardStats(branchId);
//
//        model.addAllAttributes(stats);
//        return "staff/cashier_restaurant/dashboard";
//    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, HttpSession session) {
        if (!authService.isLoggedIn(session) || !authService.isCashierRestaurant(session)) {
            return "redirect:/cashier-restaurant/login";
        }

        Long branchId = authService.getCurrentUserBranchId(session);
        Map<String, Object> stats = cashierService.getDashboardStats(branchId);

        // Lấy danh sách đơn chờ xác nhận
        List<OrderDTO> pendingOrdersList = cashierService.getOrdersByBranch(branchId, OrderStatus.PENDING);
        model.addAttribute("pendingOrdersList", pendingOrdersList);

        model.addAllAttributes(stats);
        return "staff/cashier_restaurant/dashboard";
    }

    @GetMapping("/orders")
    public String orderList(Model model,
                            HttpSession session,
                            @RequestParam(required = false) OrderStatus status) {
        if (!authService.isLoggedIn(session) || !authService.isCashierRestaurant(session)) {
            return "redirect:/cashier-restaurant/login";
        }

        Long branchId = authService.getCurrentUserBranchId(session);
        List<OrderDTO> orders = cashierService.getOrdersByBranch(branchId, status);

        model.addAttribute("orders", orders);
        model.addAttribute("orderStatuses", OrderStatus.values());
        model.addAttribute("currentStatus", status);
        return "staff/cashier_restaurant/orders/list";
    }

    @GetMapping("/orders/{id}")
    public String orderDetail(@PathVariable Long id,
                              Model model,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        if (!authService.isLoggedIn(session) || !authService.isCashierRestaurant(session)) {
            return "redirect:/cashier-restaurant/login";
        }

        try {
            Long branchId = authService.getCurrentUserBranchId(session);
            OrderDTO order = cashierService.getOrderDetail(id, branchId);

            model.addAttribute("order", order);
            model.addAttribute("orderStatuses", OrderStatus.values());
            model.addAttribute("paymentStatuses", PaymentStatus.values());
            model.addAttribute("paymentMethods", PaymentMethod.values());
            return "staff/cashier_restaurant/orders/detail";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/cashier-restaurant/orders";
        }
    }

    @GetMapping("/create-order")
    public String createOrderPage(Model model, HttpSession session) {
        if (!authService.isLoggedIn(session) || !authService.isCashierRestaurant(session)) {
            return "redirect:/cashier-restaurant/login";
        }

        Long branchId = authService.getCurrentUserBranchId(session);
        List<RestaurantTableDTO> tables = cashierService.getAvailableTables(branchId);
        List<MenuCategoryDTO> categories = cashierService.getMenuCategories(branchId);
        List<MenuItemDTO> menuItems = cashierService.getMenuItems(branchId, null);

        model.addAttribute("tables", tables);
        model.addAttribute("categories", categories);
        model.addAttribute("menuItems", menuItems);
        model.addAttribute("branchId", branchId);
        return "staff/cashier_restaurant/orders/create";
    }

    @GetMapping("/invoice/{orderId}")
    public String invoice(@PathVariable Long orderId,
                          Model model,
                          HttpSession session,
                          RedirectAttributes redirectAttributes) {
        if (!authService.isLoggedIn(session) || !authService.isCashierRestaurant(session)) {
            return "redirect:/cashier-restaurant/login";
        }

        try {
            Long branchId = authService.getCurrentUserBranchId(session);
            InvoiceDTO invoice = cashierService.generateInvoice(orderId, branchId);

            model.addAttribute("invoice", invoice);
            return "staff/cashier_restaurant/orders/invoice";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/cashier-restaurant/orders";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/cashier-restaurant/login";
    }

    // ===== API ENDPOINTS =====

    @PostMapping("/api/orders/create")
    public String createOrder(@ModelAttribute CreateOrderAtCounterRequest request,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        try {
            if (!authService.isLoggedIn(session) || !authService.isCashierRestaurant(session)) {
                redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập");
                return "redirect:/cashier-restaurant/login";
            }

            Long branchId = authService.getCurrentUserBranchId(session);
            OrderDTO order = cashierService.createOrderAtCounter(request, branchId);

            redirectAttributes.addFlashAttribute("successMessage", "Tạo đơn hàng thành công");
            return "redirect:/cashier-restaurant/orders/" + order.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/cashier-restaurant/create-order";
        }
    }

    @PostMapping("/api/orders/{id}/confirm")
    public String confirmOrder(@PathVariable Long id,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        try {
            if (!authService.isLoggedIn(session) || !authService.isCashierRestaurant(session)) {
                redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập");
                return "redirect:/cashier-restaurant/login";
            }

            Long branchId = authService.getCurrentUserBranchId(session);
            cashierService.confirmOrder(id, branchId);

            redirectAttributes.addFlashAttribute("successMessage", "Xác nhận đơn hàng thành công");
            return "redirect:/cashier-restaurant/orders/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/cashier-restaurant/orders/" + id;
        }
    }

    @PostMapping("/api/orders/{id}/confirm-payment")
    public String confirmPayment(@PathVariable Long id,
                                 @RequestParam PaymentMethod paymentMethod,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        try {
            if (!authService.isLoggedIn(session) || !authService.isCashierRestaurant(session)) {
                redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập");
                return "redirect:/cashier-restaurant/login";
            }

            Long branchId = authService.getCurrentUserBranchId(session);
            cashierService.confirmPayment(id, branchId, paymentMethod);

            redirectAttributes.addFlashAttribute("successMessage", "Xác nhận thanh toán thành công");
            return "redirect:/cashier-restaurant/invoice/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/cashier-restaurant/orders/" + id;
        }
    }

    @PostMapping("/api/orders/{id}/cancel")
    public String cancelOrder(@PathVariable Long id,
                              @RequestParam(required = false) String reason,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        try {
            if (!authService.isLoggedIn(session) || !authService.isCashierRestaurant(session)) {
                redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập");
                return "redirect:/cashier-restaurant/login";
            }

            Long branchId = authService.getCurrentUserBranchId(session);
            cashierService.cancelOrder(id, branchId, reason);

            redirectAttributes.addFlashAttribute("successMessage", "Hủy đơn hàng thành công");
            return "redirect:/cashier-restaurant/orders/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/cashier-restaurant/orders/" + id;
        }
    }

    @GetMapping("/api/menu-items")
    @ResponseBody
    public ResponseEntity<?> getMenuItems(@RequestParam Long branchId,
                                          @RequestParam(required = false) Long categoryId,
                                          HttpSession session) {
        try {
            if (!authService.isLoggedIn(session)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
            }

            List<MenuItemDTO> items = cashierService.getMenuItems(branchId, categoryId);
            return ResponseEntity.ok(items);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping("/api/tables")
    @ResponseBody
    public ResponseEntity<?> getTables(@RequestParam Long branchId, HttpSession session) {
        try {
            if (!authService.isLoggedIn(session)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
            }

            List<RestaurantTableDTO> tables = cashierService.getAvailableTables(branchId);
            return ResponseEntity.ok(tables);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }


    // Thêm vào CashierRestaurantController.java

    @GetMapping("/table-bookings")
    public String tableBookingsList(Model model,
                                    HttpSession session,
                                    @RequestParam(required = false) BookingStatus status) {
        if (!authService.isLoggedIn(session) || !authService.isCashierRestaurant(session)) {
            return "redirect:/cashier-restaurant/login";
        }

        Long branchId = authService.getCurrentUserBranchId(session);
        List<TableBookingResponse> bookings = cashierService.getTodayBookings(branchId, status);

        model.addAttribute("bookings", bookings);
        model.addAttribute("statuses", BookingStatus.values());
        model.addAttribute("currentStatus", status);

        // Đếm số lượng theo trạng thái
        Map<BookingStatus, Long> counts = bookings.stream()
                .collect(Collectors.groupingBy(TableBookingResponse::getStatus, Collectors.counting()));

        model.addAttribute("confirmedCount", counts.getOrDefault(BookingStatus.CONFIRMED, 0L));
        model.addAttribute("checkedInCount", counts.getOrDefault(BookingStatus.CHECKED_IN, 0L));
        model.addAttribute("checkedOutCount", counts.getOrDefault(BookingStatus.CHECKED_OUT, 0L));

        return "staff/cashier_restaurant/table_bookings/list";
    }

    @GetMapping("/table-bookings/{id}")
    public String tableBookingDetail(@PathVariable Long id,
                                     Model model,
                                     HttpSession session,
                                     RedirectAttributes redirectAttributes) {
        if (!authService.isLoggedIn(session) || !authService.isCashierRestaurant(session)) {
            return "redirect:/cashier-restaurant/login";
        }

        try {
            Long branchId = authService.getCurrentUserBranchId(session);
            TableBookingResponse booking = cashierService.getBookingDetail(id, branchId);

            model.addAttribute("booking", booking);
            model.addAttribute("statuses", BookingStatus.values());
            return "staff/cashier_restaurant/table_bookings/detail";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/cashier-restaurant/table-bookings";
        }
    }

    @PostMapping("/api/table-bookings/{id}/check-in")
    public String checkInBooking(@PathVariable Long id,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        try {
            if (!authService.isLoggedIn(session) || !authService.isCashierRestaurant(session)) {
                redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập");
                return "redirect:/cashier-restaurant/login";
            }

            Long branchId = authService.getCurrentUserBranchId(session);
            cashierService.checkInBooking(id, branchId);

            redirectAttributes.addFlashAttribute("successMessage", "Check-in thành công");
            return "redirect:/cashier-restaurant/table-bookings/" + id;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/cashier-restaurant/table-bookings/" + id;
        }
    }

    @PostMapping("/api/table-bookings/{id}/check-out")
    public String checkOutBooking(@PathVariable Long id,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {
        try {
            if (!authService.isLoggedIn(session) || !authService.isCashierRestaurant(session)) {
                redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập");
                return "redirect:/cashier-restaurant/login";
            }

            Long branchId = authService.getCurrentUserBranchId(session);
            cashierService.checkOutBooking(id, branchId);

            redirectAttributes.addFlashAttribute("successMessage", "Check-out thành công");
            return "redirect:/cashier-restaurant/table-bookings";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/cashier-restaurant/table-bookings/" + id;
        }
    }

    @PostMapping("/api/table-bookings/{id}/no-show")
    public String markNoShow(@PathVariable Long id,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        try {
            if (!authService.isLoggedIn(session) || !authService.isCashierRestaurant(session)) {
                redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập");
                return "redirect:/cashier-restaurant/login";
            }

            Long branchId = authService.getCurrentUserBranchId(session);
            cashierService.markNoShow(id, branchId);

            redirectAttributes.addFlashAttribute("successMessage", "Đã đánh dấu No Show");
            return "redirect:/cashier-restaurant/table-bookings";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/cashier-restaurant/table-bookings/" + id;
        }
    }
}