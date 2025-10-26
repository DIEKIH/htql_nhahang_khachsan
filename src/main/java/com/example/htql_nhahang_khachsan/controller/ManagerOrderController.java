package com.example.htql_nhahang_khachsan.controller;

import com.example.htql_nhahang_khachsan.entity.OrderEntity;
import com.example.htql_nhahang_khachsan.enums.OrderStatus;
import com.example.htql_nhahang_khachsan.enums.OrderType;
import com.example.htql_nhahang_khachsan.enums.PaymentStatus;
import com.example.htql_nhahang_khachsan.service.AuthService;
import com.example.htql_nhahang_khachsan.service.ManagerOrderService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/manager/orders")
@RequiredArgsConstructor
public class ManagerOrderController {

    private final AuthService authService;
    private final ManagerOrderService orderService;

    @GetMapping
    public String orderList(Model model,
                            HttpSession session,
                            @RequestParam(required = false) String orderCode,
                            @RequestParam(required = false) OrderStatus status,
                            @RequestParam(required = false) OrderType orderType,
                            @RequestParam(required = false) PaymentStatus paymentStatus,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {

        if (!authService.isLoggedIn(session) || !authService.isManager(session)) {
            return "redirect:/manager/login";
        }

        Long branchId = authService.getCurrentUserBranchId(session);

        List<OrderEntity> orders;
        if (orderCode != null || status != null || orderType != null || paymentStatus != null || fromDate != null || toDate != null) {
            orders = orderService.searchOrders(branchId, orderCode, status, orderType, paymentStatus, fromDate, toDate);
        } else {
            orders = orderService.getAllOrdersByBranch(branchId);
        }

        model.addAttribute("orders", orders);
        model.addAttribute("orderStatuses", OrderStatus.values());
        model.addAttribute("orderTypes", OrderType.values());
        model.addAttribute("paymentStatuses", PaymentStatus.values());
        model.addAttribute("searchOrderCode", orderCode);
        model.addAttribute("searchStatus", status);
        model.addAttribute("searchOrderType", orderType);
        model.addAttribute("searchPaymentStatus", paymentStatus);
        model.addAttribute("searchFromDate", fromDate);
        model.addAttribute("searchToDate", toDate);

        return "manager/orders/list";
    }

    @GetMapping("/{id}")
    public String viewOrderDetail(@PathVariable Long id,
                                  HttpSession session,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        if (!authService.isLoggedIn(session) || !authService.isManager(session)) {
            return "redirect:/manager/login";
        }

        try {
            Long branchId = authService.getCurrentUserBranchId(session);
            OrderEntity order = orderService.getOrderById(id, branchId);

            model.addAttribute("order", order);
            model.addAttribute("orderStatuses", OrderStatus.values());
            model.addAttribute("paymentStatuses", PaymentStatus.values());

            return "manager/orders/detail";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/manager/orders";
        }
    }

    @PostMapping("/{id}/update-status")
    public String updateOrderStatus(@PathVariable Long id,
                                    @RequestParam OrderStatus status,
                                    HttpSession session,
                                    RedirectAttributes redirectAttributes) {
        if (!authService.isLoggedIn(session) || !authService.isManager(session)) {
            return "redirect:/manager/login";
        }

        try {
            Long branchId = authService.getCurrentUserBranchId(session);
            orderService.updateOrderStatus(id, branchId, status);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái đơn hàng thành công");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/manager/orders/" + id;
    }

    @PostMapping("/{id}/update-payment-status")
    public String updatePaymentStatus(@PathVariable Long id,
                                      @RequestParam PaymentStatus paymentStatus,
                                      HttpSession session,
                                      RedirectAttributes redirectAttributes) {
        if (!authService.isLoggedIn(session) || !authService.isManager(session)) {
            return "redirect:/manager/login";
        }

        try {
            Long branchId = authService.getCurrentUserBranchId(session);
            orderService.updatePaymentStatus(id, branchId, paymentStatus);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái thanh toán thành công");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/manager/orders/" + id;
    }

    @PostMapping("/{id}/confirm-payment")
    public String confirmPayment(@PathVariable Long id,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        if (!authService.isLoggedIn(session) || !authService.isManager(session)) {
            return "redirect:/manager/login";
        }

        try {
            Long branchId = authService.getCurrentUserBranchId(session);
            orderService.confirmPayment(id, branchId);
            redirectAttributes.addFlashAttribute("successMessage", "Xác nhận thanh toán thành công");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/manager/orders/" + id;
    }

    @PostMapping("/{id}/cancel")
    public String cancelOrder(@PathVariable Long id,
                              @RequestParam(required = false) String cancelReason,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        if (!authService.isLoggedIn(session) || !authService.isManager(session)) {
            return "redirect:/manager/login";
        }

        try {
            Long branchId = authService.getCurrentUserBranchId(session);
            orderService.cancelOrder(id, branchId, cancelReason);
            redirectAttributes.addFlashAttribute("successMessage", "Hủy đơn hàng thành công");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/manager/orders/" + id;
    }
}