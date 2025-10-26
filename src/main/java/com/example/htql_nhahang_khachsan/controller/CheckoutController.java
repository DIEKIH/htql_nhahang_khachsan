package com.example.htql_nhahang_khachsan.controller;

import com.example.htql_nhahang_khachsan.dto.*;
import com.example.htql_nhahang_khachsan.enums.PaymentMethod;
import com.example.htql_nhahang_khachsan.repository.UserRepository;
import com.example.htql_nhahang_khachsan.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/checkout")
@RequiredArgsConstructor
@Slf4j
public class CheckoutController {

    private final CartService cartService;
    private final OrderService orderService;
    private final UserRepository userRepository;

    @GetMapping("/customer-info")
    public String showCustomerInfoPage(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");

        if (userId == null) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/customer/login";
        }

        CartSummaryResponse cart = cartService.getCartSummary(userId);

        if (cart.getItems().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Giỏ hàng trống");
            return "redirect:/cart";
        }

        userRepository.findById(userId).ifPresent(user -> {
            model.addAttribute("user", user);
        });

        model.addAttribute("cart", cart);
        return "customer/checkout/customer-info";
    }

    @PostMapping("/customer-info")
    public String saveCustomerInfo(@RequestParam String customerName,
                                   @RequestParam String customerPhone,
                                   @RequestParam String customerAddress,
                                   @RequestParam(required = false) String orderNotes,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");

        if (userId == null) {
            return "redirect:/customer/login";
        }

        if (customerName == null || customerName.trim().length() < 2) {
            redirectAttributes.addFlashAttribute("error", "Họ tên không hợp lệ");
            return "redirect:/checkout/customer-info";
        }

        if (customerPhone == null || !customerPhone.matches("^0\\d{9,10}$")) {
            redirectAttributes.addFlashAttribute("error", "Số điện thoại không hợp lệ");
            return "redirect:/checkout/customer-info";
        }

        if (customerAddress == null || customerAddress.trim().length() < 10) {
            redirectAttributes.addFlashAttribute("error", "Địa chỉ quá ngắn");
            return "redirect:/checkout/customer-info";
        }

        CheckoutRequest checkoutRequest = CheckoutRequest.builder()
                .customerName(customerName.trim())
                .customerPhone(customerPhone.trim())
                .customerAddress(customerAddress.trim())
                .orderNotes(orderNotes != null ? orderNotes.trim() : null)
                .build();

        session.setAttribute("checkoutRequest", checkoutRequest);

        return "redirect:/checkout/payment";
    }

    @GetMapping("/payment")
    public String showPaymentPage(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");

        if (userId == null) {
            return "redirect:/customer/login";
        }

        CheckoutRequest customerInfo = (CheckoutRequest) session.getAttribute("checkoutRequest");
        if (customerInfo == null) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng nhập thông tin khách hàng");
            return "redirect:/checkout/customer-info";
        }

        CartSummaryResponse cart = cartService.getCartSummary(userId);

        if (cart.getItems().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Giỏ hàng trống");
            return "redirect:/cart";
        }

        model.addAttribute("cart", cart);
        model.addAttribute("customerInfo", customerInfo);

        return "customer/checkout/payment";
    }

    @PostMapping("/payment")
    public String processPayment(@RequestParam(name = "paymentMethod") String paymentMethodStr,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {

        log.info("=== PAYMENT REQUEST RECEIVED ===");
        log.info("Payment method param: {}", paymentMethodStr);

        Long userId = (Long) session.getAttribute("userId");
        log.info("User ID from session: {}", userId);

        if (userId == null) {
            log.warn("User not logged in");
            redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập lại");
            return "redirect:/customer/login";
        }

        CheckoutRequest checkoutRequest = (CheckoutRequest) session.getAttribute("checkoutRequest");
        if (checkoutRequest == null) {
            log.warn("Checkout request not found in session");
            redirectAttributes.addFlashAttribute("error", "Vui lòng nhập thông tin khách hàng");
            return "redirect:/checkout/customer-info";
        }
        log.info("Checkout request found: {}", checkoutRequest);

        // Validate payment method
        if (paymentMethodStr == null || paymentMethodStr.trim().isEmpty()) {
            log.error("Payment method is null or empty");
            redirectAttributes.addFlashAttribute("error", "Vui lòng chọn phương thức thanh toán");
            return "redirect:/checkout/payment";
        }

        // Verify payment method is valid enum value
        PaymentMethod paymentMethod;
        try {
            paymentMethod = PaymentMethod.valueOf(paymentMethodStr.trim().toUpperCase());
            log.info("Payment method validated: {}", paymentMethod);
        } catch (IllegalArgumentException e) {
            log.error("Invalid payment method: {}", paymentMethodStr, e);
            redirectAttributes.addFlashAttribute("error", "Phương thức thanh toán không hợp lệ: " + paymentMethodStr);
            return "redirect:/checkout/payment";
        }

        checkoutRequest.setPaymentMethod(paymentMethod.name());

        try {
            log.info("Creating order with payment method: {}", paymentMethod);

            // Create order
            Long orderId = orderService.createOrderFromCart(userId, checkoutRequest);
            log.info("Order created successfully with ID: {}", orderId);

            // Clear cart and session
            cartService.clearCart(userId);
            session.removeAttribute("checkoutRequest");
            log.info("Cart cleared and session cleaned");

            // Redirect based on payment method
            String redirectUrl;
            switch (paymentMethod) {
                case VNPAY:
                    log.info("Redirecting to VNPay payment gateway for order: {}", orderId);
                    redirectUrl = "redirect:/payment/vnpay/" + orderId;
                    break;

                case MOMO:
                    log.info("Redirecting to MoMo payment gateway for order: {}", orderId);
                    redirectUrl = "redirect:/payment/momo/" + orderId;
                    break;

                case CASH_ON_DELIVERY:
                    log.info("COD order created, redirecting to confirmation");
                    redirectAttributes.addFlashAttribute("success", "Đặt hàng thành công! Vui lòng chuẩn bị tiền mặt khi nhận hàng.");
                    redirectUrl = "redirect:/orders/" + orderId;
                    break;

                case BANK_TRANSFER:
                    log.info("Bank transfer order created, redirecting to confirmation");
                    redirectAttributes.addFlashAttribute("success", "Đặt hàng thành công! Vui lòng chuyển khoản theo thông tin đã cung cấp.");
                    redirectUrl = "redirect:/orders/" + orderId;
                    break;

                default:
                    log.warn("Unknown payment method: {}", paymentMethod);
                    redirectAttributes.addFlashAttribute("success", "Đặt hàng thành công!");
                    redirectUrl = "redirect:/orders/" + orderId;
            }

            log.info("Final redirect URL: {}", redirectUrl);
            return redirectUrl;

        } catch (Exception e) {
            log.error("Error processing payment - Exception type: {}", e.getClass().getName());
            log.error("Error message: {}", e.getMessage());
            log.error("Full stack trace: ", e);
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/checkout/payment";
        }
    }
}