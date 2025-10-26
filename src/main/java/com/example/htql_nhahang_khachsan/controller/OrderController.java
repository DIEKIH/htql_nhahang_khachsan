package com.example.htql_nhahang_khachsan.controller;

import com.example.htql_nhahang_khachsan.entity.*;
import com.example.htql_nhahang_khachsan.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import java.util.List;

@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/{orderId}")
    public String viewOrderDetail(@PathVariable Long orderId,
                                  HttpSession session,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");

        if (userId == null) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/customer/login";
        }

        try {
            OrderEntity order = orderService.getOrderById(orderId);

            // Check if user owns this order
            if (!order.getCustomer().getId().equals(userId)) {
                redirectAttributes.addFlashAttribute("error", "Bạn không có quyền xem đơn hàng này");
                return "redirect:/";
            }

            List<OrderItemEntity> orderItems = order.getItems();

            model.addAttribute("order", order);
            model.addAttribute("orderItems", orderItems);

            return "customer/orders/confirmation";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy đơn hàng");
            return "redirect:/";
        }
    }

    @GetMapping("/my-orders")
    public String viewMyOrders(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");

        if (userId == null) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/customer/login";
        }

        List<OrderEntity> orders = orderService.getUserOrders(userId);
        model.addAttribute("orders", orders);

            return "customer/orders/list";
    }
}