package com.example.htql_nhahang_khachsan.controller;

import com.example.htql_nhahang_khachsan.dto.*;
import com.example.htql_nhahang_khachsan.enums.PaymentMethod;
import com.example.htql_nhahang_khachsan.repository.UserRepository;
import com.example.htql_nhahang_khachsan.service.*;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller xử lý đặt món nhanh (Quick Order) - Bỏ qua giỏ hàng
 */
@Controller
@RequestMapping("/quick-order")
@RequiredArgsConstructor
@Slf4j
public class QuickOrderController {

    private final MenuService menuService;
    private final BranchService branchService; // ← THÊM DÒng NÀY
    private final OrderService orderService;
    private final UserRepository userRepository;
    private final AuthService authService;

    /**
     * ===== API TẠO QUICK ORDER (được gọi từ JavaScript) =====
     */

    /**
     * API tạo quick order từ trang chi tiết chi nhánh
     * Trả về quickOrderId để chuyển đến checkout
     */
    @PostMapping("/create")
    @ResponseBody
    public Map<String, Object> createQuickOrder(@RequestBody QuickOrderCreateRequest request,
                                                HttpSession session) {
        log.info("=== CREATE QUICK ORDER API ===");
        log.info("Menu Item: {}, Branch: {}, Quantity: {}",
                request.getMenuItemId(), request.getBranchId(), request.getQuantity());

        Map<String, Object> response = new HashMap<>();

        // Kiểm tra đăng nhập
        if (!authService.isLoggedIn(session)) {
            log.warn("User not logged in");
            response.put("success", false);
            response.put("error", "Vui lòng đăng nhập");
            return response;
        }

        try {
            Long userId = authService.getCurrentUserId(session);

            // Lấy thông tin món ăn
            MenuItemResponse menuItem = menuService.getMenuItemById(request.getMenuItemId());

            if (menuItem == null || !menuItem.getIsAvailable()) {
                response.put("success", false);
                response.put("error", "Món ăn không khả dụng");
                return response;
            }

            // Lấy giá - ưu tiên currentPrice, fallback về price
            BigDecimal unitPrice = menuItem.getCurrentPrice() != null
                    ? menuItem.getCurrentPrice()
                    : menuItem.getPrice();

            if (unitPrice == null) {
                log.error("Menu item price is null for ID: {}", request.getMenuItemId());
                response.put("success", false);
                response.put("error", "Món ăn chưa có giá");
                return response;
            }

            // Lấy thông tin chi nhánh
            BranchResponse branch = branchService.getBranchById(request.getBranchId());

            if (branch == null) {
                response.put("success", false);
                response.put("error", "Không tìm thấy chi nhánh");
                return response;
            }

            // Tạo quick order session data
            QuickOrderSessionData sessionData = QuickOrderSessionData.builder()
                    .menuItemId(request.getMenuItemId())
                    .branchId(request.getBranchId())
                    .quantity(request.getQuantity())
                    .unitPrice(unitPrice) // Đã check null ở trên
                    .menuItemName(menuItem.getName())
                    .menuItemImage(menuItem.getImageUrl())
                    .branchName(branch.getName())
                    .build();

            log.info("Session data - Unit Price: {}, Menu: {}", unitPrice, menuItem.getName());

            // Lưu vào session
            session.setAttribute("quickOrderData", sessionData);

            // Tạo ID tạm để tracking
            String quickOrderId = "QO" + System.currentTimeMillis();

            log.info("Quick order session created: {}", quickOrderId);

            response.put("success", true);
            response.put("quickOrderId", quickOrderId);
            response.put("message", "Đã tạo đơn hàng tạm thời");

            return response;

        } catch (Exception e) {
            log.error("Error creating quick order: ", e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return response;
        }
    }

    // DTO đã được tách ra file riêng trong package dto:
    // - QuickOrderCreateRequest.java
    // - QuickOrderSessionData.java

    /**
     * BƯỚC 1: Hiển thị trang nhập thông tin đặt món nhanh
     * Được gọi từ nút "Đặt ngay" trên trang chi tiết chi nhánh
     */
    @GetMapping("/menu-item/{menuItemId}")
    public String showQuickOrderForm(@PathVariable Long menuItemId,
                                     @RequestParam Long branchId,
                                     @RequestParam(defaultValue = "1") Integer quantity,
                                     HttpSession session,
                                     Model model,
                                     RedirectAttributes redirectAttributes) {

        // Kiểm tra đăng nhập
        if (!authService.isLoggedIn(session)) {
            log.warn("User not logged in, redirecting to login");
            session.setAttribute("redirectAfterLogin", "/quick-order/menu-item/" + menuItemId + "?branchId=" + branchId + "&quantity=" + quantity);
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng đăng nhập để đặt món");
            return "redirect:/login";
        }

        try {
            Long userId = authService.getCurrentUserId(session);

            // Lấy thông tin món ăn
            MenuItemResponse menuItem = menuService.getMenuItemById(menuItemId);

            if (menuItem == null || !menuItem.getIsAvailable()) {
                log.error("Menu item not available: {}", menuItemId);
                redirectAttributes.addFlashAttribute("errorMessage", "Món ăn không khả dụng");
                return "redirect:/branches/" + branchId;
            }

            // Lấy thông tin chi nhánh
            BranchResponse branch = branchService.getBranchById(branchId);

            // Tạo QuickOrderRequest với thông tin món ăn
            QuickOrderRequest quickOrderRequest = QuickOrderRequest.builder()
                    .menuItemId(menuItemId)
                    .branchId(branchId)
                    .quantity(quantity)
                    .unitPrice(menuItem.getCurrentPrice())
                    .menuItemName(menuItem.getName())
                    .menuItemImage(menuItem.getImageUrl())
                    .isTakeaway(false)
                    .build();

            // Lấy thông tin user để điền sẵn
            userRepository.findById(userId).ifPresent(user -> {
                quickOrderRequest.setCustomerName(user.getFullName());
                quickOrderRequest.setCustomerPhone(user.getPhoneNumber());
                model.addAttribute("user", user);
            });

            model.addAttribute("quickOrderRequest", quickOrderRequest);
            model.addAttribute("menuItem", menuItem);
            model.addAttribute("branch", branch);

            log.info("Showing quick order form for menu item: {}, quantity: {}", menuItemId, quantity);

            return "customer/quick-order/form";

        } catch (Exception e) {
            log.error("Error showing quick order form: ", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/branches/" + branchId;
        }
    }

    /**
     * BƯỚC 2: Xử lý submit form thông tin khách hàng
     */
    @PostMapping("/submit")
    public String submitQuickOrder(@Valid @ModelAttribute("quickOrderRequest") QuickOrderRequest request,
                                   BindingResult bindingResult,
                                   HttpSession session,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {

        log.info("=== QUICK ORDER SUBMIT ===");
        log.info("Menu Item ID: {}, Quantity: {}, Payment: {}",
                request.getMenuItemId(), request.getQuantity(), request.getPaymentMethod());

        // Kiểm tra đăng nhập
        if (!authService.isLoggedIn(session)) {
            return "redirect:/login";
        }

        Long userId = authService.getCurrentUserId(session);

        // Kiểm tra validation
        if (bindingResult.hasErrors()) {
            log.error("Validation errors: {}", bindingResult.getAllErrors());

            try {
                // Load lại thông tin để hiển thị form với lỗi
                MenuItemResponse menuItem = menuService.getMenuItemById(request.getMenuItemId());
                BranchResponse branch = branchService.getBranchById(request.getBranchId());

                model.addAttribute("menuItem", menuItem);
                model.addAttribute("branch", branch);
                model.addAttribute("errorMessage", "Vui lòng kiểm tra lại thông tin");

                return "customer/quick-order/form";
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("errorMessage", "Có lỗi xảy ra");
                return "redirect:/branches/" + request.getBranchId();
            }
        }

        // Validate payment method
        PaymentMethod paymentMethod;
        try {
            paymentMethod = PaymentMethod.valueOf(request.getPaymentMethod().toUpperCase());
            log.info("Payment method validated: {}", paymentMethod);
        } catch (IllegalArgumentException e) {
            log.error("Invalid payment method: {}", request.getPaymentMethod());
            redirectAttributes.addFlashAttribute("errorMessage", "Phương thức thanh toán không hợp lệ");
            return "redirect:/quick-order/menu-item/" + request.getMenuItemId() + "?branchId=" + request.getBranchId();
        }

        try {
            // Tạo đơn hàng từ quick order
            Long orderId = orderService.createQuickOrder(userId, request);
            log.info("Quick order created successfully with ID: {}", orderId);

            // Xử lý theo phương thức thanh toán
            switch (paymentMethod) {
                case VNPAY:
                    log.info("Redirecting to VNPay for order: {}", orderId);
                    return "redirect:/payment/vnpay/" + orderId;

                case MOMO:
                    log.info("Redirecting to MoMo for order: {}", orderId);
                    return "redirect:/payment/momo/" + orderId;

                case CASH_ON_DELIVERY:
                    log.info("COD order created");
                    redirectAttributes.addFlashAttribute("successMessage",
                            "Đặt món thành công! Vui lòng chuẩn bị tiền mặt khi nhận hàng.");
                    return "redirect:/orders/" + orderId;

                case BANK_TRANSFER:
                    log.info("Bank transfer order created");
                    redirectAttributes.addFlashAttribute("successMessage",
                            "Đặt món thành công! Vui lòng chuyển khoản theo thông tin đã cung cấp.");
                    return "redirect:/orders/" + orderId;

                default:
                    redirectAttributes.addFlashAttribute("successMessage", "Đặt món thành công!");
                    return "redirect:/orders/" + orderId;
            }

        } catch (Exception e) {
            log.error("Error creating quick order: ", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Đặt món thất bại: " + e.getMessage());
            return "redirect:/quick-order/menu-item/" + request.getMenuItemId() + "?branchId=" + request.getBranchId();
        }
    }
}