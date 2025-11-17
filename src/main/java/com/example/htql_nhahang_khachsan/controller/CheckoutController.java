package com.example.htql_nhahang_khachsan.controller;

import com.example.htql_nhahang_khachsan.dto.*;
import com.example.htql_nhahang_khachsan.entity.OrderEntity;
import com.example.htql_nhahang_khachsan.enums.OrderStatus;
import com.example.htql_nhahang_khachsan.enums.PaymentMethod;
import com.example.htql_nhahang_khachsan.enums.PaymentStatus;
import com.example.htql_nhahang_khachsan.repository.OrderRepository;
import com.example.htql_nhahang_khachsan.repository.UserRepository;
import com.example.htql_nhahang_khachsan.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Controller
@RequestMapping("/checkout")
@RequiredArgsConstructor
@Slf4j
public class CheckoutController {

    private final CartService cartService;
    private final OrderService orderService;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

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


    // ===== THÊM VÀO CheckoutController.java =====
// ===== THÊM VÀO CheckoutController.java =====

    /**
     * Hiển thị trang customer-info cho Quick Order
     * Tái sử dụng trang customer-info.html hiện tại
     */
    @GetMapping("/quick-order/{quickOrderId}")
    public String showQuickOrderCustomerInfo(@PathVariable String quickOrderId,
                                             HttpSession session,
                                             Model model,
                                             RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");

        if (userId == null) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/customer/login";
        }

        // Lấy thông tin quick order từ session
        QuickOrderSessionData quickOrderData = (QuickOrderSessionData) session.getAttribute("quickOrderData");

        if (quickOrderData == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy thông tin đặt món");
            return "redirect:/";
        }

        // Validate data integrity
        if (quickOrderData.getUnitPrice() == null) {
            log.error("Unit price is null in session data");
            redirectAttributes.addFlashAttribute("error", "Dữ liệu đơn hàng không hợp lệ");
            session.removeAttribute("quickOrderData");
            return "redirect:/";
        }

        log.info("=== QUICK ORDER CHECKOUT ===");
        log.info("Quick Order ID: {}", quickOrderId);
        log.info("Menu Item: {}, Quantity: {}, Unit Price: {}",
                quickOrderData.getMenuItemName(),
                quickOrderData.getQuantity(),
                quickOrderData.getUnitPrice());

        // Tính toán tổng tiền (giống logic trong OrderService)
        BigDecimal subtotal = quickOrderData.getUnitPrice()
                .multiply(BigDecimal.valueOf(quickOrderData.getQuantity()));

        BigDecimal serviceCharge = subtotal.multiply(new BigDecimal("0.10"))
                .setScale(0, RoundingMode.HALF_UP);

        BigDecimal vat = subtotal.add(serviceCharge).multiply(new BigDecimal("0.08"))
                .setScale(0, RoundingMode.HALF_UP);

        BigDecimal totalAmount = subtotal.add(serviceCharge).add(vat);

        // Tạo CartSummaryResponse giả để tái sử dụng view
        CartSummaryResponse cartSummary = CartSummaryResponse.builder()
                .branchId(quickOrderData.getBranchId())
                .branchName(quickOrderData.getBranchName())
                .totalItems(quickOrderData.getQuantity())
                .subtotal(subtotal)
                .serviceCharge(serviceCharge)
                .vat(vat)
                .totalAmount(totalAmount)
                .formattedSubtotal(formatCurrency(subtotal))
                .formattedServiceCharge(formatCurrency(serviceCharge))
                .formattedVat(formatCurrency(vat))
                .formattedTotalAmount(formatCurrency(totalAmount))
                .build();

        // Lấy thông tin user để điền sẵn
        userRepository.findById(userId).ifPresent(user -> {
            model.addAttribute("user", user);
        });

        model.addAttribute("cart", cartSummary);
        model.addAttribute("isQuickOrder", true); // Đánh dấu là quick order
        model.addAttribute("quickOrderData", quickOrderData); // Để hiển thị thông tin món

        log.info("Calculated total: {}", totalAmount);

        // Tái sử dụng trang customer-info.html
        return "customer/checkout/customer-info";
    }

    /**
     * Xử lý submit form customer-info cho Quick Order
     * Tạo order trực tiếp từ quick order data
     */
    @PostMapping("/quick-order/submit")
    public String submitQuickOrderCustomerInfo(@RequestParam String customerName,
                                               @RequestParam String customerPhone,
                                               @RequestParam String customerAddress,
                                               @RequestParam(required = false) String orderNotes,
                                               HttpSession session,
                                               RedirectAttributes redirectAttributes) {

        log.info("=== SUBMIT QUICK ORDER CUSTOMER INFO ===");

        Long userId = (Long) session.getAttribute("userId");

        if (userId == null) {
            return "redirect:/customer/login";
        }

        // Validate input
        if (customerName == null || customerName.trim().length() < 2) {
            redirectAttributes.addFlashAttribute("error", "Họ tên không hợp lệ");
            return "redirect:/checkout/quick-order/current";
        }

        if (customerPhone == null || !customerPhone.matches("^0\\d{9,10}$")) {
            redirectAttributes.addFlashAttribute("error", "Số điện thoại không hợp lệ");
            return "redirect:/checkout/quick-order/current";
        }

        if (customerAddress == null || customerAddress.trim().length() < 10) {
            redirectAttributes.addFlashAttribute("error", "Địa chỉ quá ngắn");
            return "redirect:/checkout/quick-order/current";
        }

        // Lưu thông tin customer vào session để dùng ở bước payment
        CheckoutRequest checkoutRequest = CheckoutRequest.builder()
                .customerName(customerName.trim())
                .customerPhone(customerPhone.trim())
                .customerAddress(customerAddress.trim())
                .orderNotes(orderNotes != null ? orderNotes.trim() : null)
                .build();

        session.setAttribute("checkoutRequest", checkoutRequest);

        log.info("Customer info saved to session");

        return "redirect:/checkout/quick-order/payment";
    }

    /**
     * Hiển thị trang payment cho Quick Order
     */
    @GetMapping("/quick-order/payment")
    public String showQuickOrderPayment(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");

        if (userId == null) {
            return "redirect:/customer/login";
        }

        CheckoutRequest customerInfo = (CheckoutRequest) session.getAttribute("checkoutRequest");
        if (customerInfo == null) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng nhập thông tin khách hàng");
            return "redirect:/checkout/quick-order/current";
        }

        QuickOrderSessionData quickOrderData = (QuickOrderSessionData) session.getAttribute("quickOrderData");
        if (quickOrderData == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy thông tin đặt món");
            return "redirect:/";
        }

        // Tính lại tổng tiền
        BigDecimal subtotal = quickOrderData.getUnitPrice()
                .multiply(BigDecimal.valueOf(quickOrderData.getQuantity()));

        BigDecimal serviceCharge = subtotal.multiply(new BigDecimal("0.10"))
                .setScale(0, RoundingMode.HALF_UP);

        BigDecimal vat = subtotal.add(serviceCharge).multiply(new BigDecimal("0.08"))
                .setScale(0, RoundingMode.HALF_UP);

        BigDecimal totalAmount = subtotal.add(serviceCharge).add(vat);

        CartSummaryResponse cartSummary = CartSummaryResponse.builder()
                .branchId(quickOrderData.getBranchId())
                .branchName(quickOrderData.getBranchName())
                .totalItems(quickOrderData.getQuantity())
                .subtotal(subtotal)
                .serviceCharge(serviceCharge)
                .vat(vat)
                .totalAmount(totalAmount)
                .formattedSubtotal(formatCurrency(subtotal))
                .formattedServiceCharge(formatCurrency(serviceCharge))
                .formattedVat(formatCurrency(vat))
                .formattedTotalAmount(formatCurrency(totalAmount))
                .build();

        model.addAttribute("cart", cartSummary);
        model.addAttribute("customerInfo", customerInfo);
        model.addAttribute("isQuickOrder", true);
        model.addAttribute("quickOrderData", quickOrderData);

        // Tái sử dụng trang payment.html
        return "customer/checkout/payment";
    }

    /**
     * Xử lý thanh toán cho Quick Order
     */
    @PostMapping("/quick-order/payment")
    public String processQuickOrderPayment(@RequestParam(name = "paymentMethod") String paymentMethodStr,
                                           HttpSession session,
                                           RedirectAttributes redirectAttributes) {

        log.info("=== PROCESS QUICK ORDER PAYMENT ===");
        log.info("Payment method: {}", paymentMethodStr);

        Long userId = (Long) session.getAttribute("userId");

        if (userId == null) {
            log.warn("User not logged in");
            redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập lại");
            return "redirect:/customer/login";
        }

        CheckoutRequest checkoutRequest = (CheckoutRequest) session.getAttribute("checkoutRequest");
        if (checkoutRequest == null) {
            log.warn("Checkout request not found");
            redirectAttributes.addFlashAttribute("error", "Vui lòng nhập thông tin khách hàng");
            return "redirect:/checkout/quick-order/current";
        }

        QuickOrderSessionData quickOrderData = (QuickOrderSessionData) session.getAttribute("quickOrderData");
        if (quickOrderData == null) {
            log.warn("Quick order data not found");
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy thông tin đặt món");
            return "redirect:/";
        }

        // Validate payment method
        if (paymentMethodStr == null || paymentMethodStr.trim().isEmpty()) {
            log.error("Payment method is null or empty");
            redirectAttributes.addFlashAttribute("error", "Vui lòng chọn phương thức thanh toán");
            return "redirect:/checkout/quick-order/payment";
        }

        PaymentMethod paymentMethod;
        try {
            paymentMethod = PaymentMethod.valueOf(paymentMethodStr.trim().toUpperCase());
            log.info("Payment method validated: {}", paymentMethod);
        } catch (IllegalArgumentException e) {
            log.error("Invalid payment method: {}", paymentMethodStr, e);
            redirectAttributes.addFlashAttribute("error", "Phương thức thanh toán không hợp lệ");
            return "redirect:/checkout/quick-order/payment";
        }

        try {
            // Tạo QuickOrderRequest từ session data
            QuickOrderRequest quickOrderRequest = QuickOrderRequest.builder()
                    .menuItemId(quickOrderData.getMenuItemId())
                    .branchId(quickOrderData.getBranchId())
                    .quantity(quickOrderData.getQuantity())
                    .unitPrice(quickOrderData.getUnitPrice())
                    .menuItemName(quickOrderData.getMenuItemName())
                    .menuItemImage(quickOrderData.getMenuItemImage())
                    .customerName(checkoutRequest.getCustomerName())
                    .customerPhone(checkoutRequest.getCustomerPhone())
                    .customerAddress(checkoutRequest.getCustomerAddress())
                    .orderNotes(checkoutRequest.getOrderNotes())
                    .paymentMethod(paymentMethod.name())
                    .build();

            // Tạo order từ quick order
            Long orderId = orderService.createQuickOrder(userId, quickOrderRequest);
            log.info("Quick order created successfully with ID: {}", orderId);

            // Xóa session data
            session.removeAttribute("quickOrderData");
            session.removeAttribute("checkoutRequest");
            log.info("Session cleaned");

            // Redirect theo payment method
            String redirectUrl;
            switch (paymentMethod) {
                case VNPAY:
                    log.info("Redirecting to VNPay for order: {}", orderId);
                    redirectUrl = "redirect:/payment/vnpay/" + orderId;
                    break;

                case MOMO:
                    log.info("Redirecting to MoMo for order: {}", orderId);
                    redirectUrl = "redirect:/payment/momo/" + orderId;
                    break;

                case CASH_ON_DELIVERY:
                    log.info("COD order created");
                    redirectAttributes.addFlashAttribute("success",
                            "Đặt món thành công! Vui lòng chuẩn bị tiền mặt khi nhận hàng.");
                    redirectUrl = "redirect:/orders/" + orderId;
                    break;

                case BANK_TRANSFER:
                    log.info("Bank transfer order created");
                    redirectAttributes.addFlashAttribute("success",
                            "Đặt món thành công! Vui lòng chuyển khoản theo thông tin đã cung cấp.");
                    redirectUrl = "redirect:/orders/" + orderId;
                    break;

                default:
                    redirectAttributes.addFlashAttribute("success", "Đặt món thành công!");
                    redirectUrl = "redirect:/orders/" + orderId;
            }

            log.info("Final redirect URL: {}", redirectUrl);
            return redirectUrl;

        } catch (Exception e) {
            log.error("Error processing quick order payment: ", e);
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/checkout/quick-order/payment";
        }
    }



    // ✅ THÊM vào CheckoutController.java

    /**
     * ✅ Hiển thị trang chọn phương thức thanh toán cho Quick Order
     * Gọi từ chatbot qua button
     */
    @GetMapping("/quick-order/payment/{orderId}")
    public String showQuickOrderPayment(@PathVariable Long orderId,
                                        Model model,
                                        RedirectAttributes redirectAttributes) {
        log.info("=== SHOW QUICK ORDER PAYMENT ===");
        log.info("Order ID: {}", orderId);

        try {
            OrderEntity order = orderService.getOrderById(orderId);

            if (order == null) {
                log.error("Order not found: {}", orderId);
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy đơn hàng");
                return "redirect:/";
            }

            // ✅ Kiểm tra đã thanh toán chưa
            if (order.getPaymentStatus() != PaymentStatus.PENDING) {
                log.warn("Order already processed: {}", orderId);
                redirectAttributes.addFlashAttribute("error", "Đơn hàng đã được xử lý");
                return "redirect:/orders/" + orderId;
            }

            log.info("Order found - Total: {}, Customer: {}",
                    order.getTotalAmount(), order.getCustomerName());

            // ✅ Tạo CartSummaryResponse giả để tái sử dụng view payment.html
            BigDecimal totalAmount = order.getTotalAmount();

            // Tính ngược lại các khoản phí
            BigDecimal subtotal = totalAmount
                    .divide(new BigDecimal("1.188"), 2, RoundingMode.HALF_UP); // Bỏ VAT 8% và service 10%

            BigDecimal serviceCharge = subtotal.multiply(new BigDecimal("0.10"))
                    .setScale(0, RoundingMode.HALF_UP);

            BigDecimal vat = subtotal.add(serviceCharge).multiply(new BigDecimal("0.08"))
                    .setScale(0, RoundingMode.HALF_UP);

            CartSummaryResponse cartSummary = CartSummaryResponse.builder()
                    .branchId(order.getBranch().getId())
                    .branchName(order.getBranch().getName())
                    .totalItems(order.getItems().size())
                    .subtotal(subtotal)
                    .serviceCharge(serviceCharge)
                    .vat(vat)
                    .totalAmount(totalAmount)
                    .formattedSubtotal(formatCurrency(subtotal))
                    .formattedServiceCharge(formatCurrency(serviceCharge))
                    .formattedVat(formatCurrency(vat))
                    .formattedTotalAmount(formatCurrency(totalAmount))
                    .build();

            // ✅ Thông tin khách hàng
            CheckoutRequest customerInfo = CheckoutRequest.builder()
                    .customerName(order.getCustomerName())
                    .customerPhone(order.getCustomerPhone())
                    .customerAddress(order.getCustomerAddress())
                    .orderNotes(order.getNotes())
                    .build();

            model.addAttribute("cart", cartSummary);
            model.addAttribute("customerInfo", customerInfo);
            model.addAttribute("orderId", orderId); // ← QUAN TRỌNG
            model.addAttribute("isQuickOrder", true); // ← Đánh dấu quick order
            model.addAttribute("quickOrderData", order.getItems().get(0)); // ← Để hiển thị món

            log.info("Rendering payment page for order: {}", orderId);

            return "customer/checkout/payment"; // ← Tái sử dụng view hiện tại

        } catch (Exception e) {
            log.error("Error showing quick order payment", e);
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/";
        }
    }

    /**
     * ✅ Xử lý thanh toán Quick Order
     */
    @PostMapping("/quick-order/payment/{orderId}")
    public String processQuickOrderPayment(@PathVariable Long orderId,
                                           @RequestParam(name = "paymentMethod") String paymentMethodStr,
                                           RedirectAttributes redirectAttributes) {
        log.info("=== PROCESS QUICK ORDER PAYMENT ===");
        log.info("Order ID: {}, Payment method: {}", orderId, paymentMethodStr);

        try {
            OrderEntity order = orderService.getOrderById(orderId);

            if (order == null) {
                log.error("Order not found: {}", orderId);
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy đơn hàng");
                return "redirect:/";
            }

            // ✅ Validate payment method
            PaymentMethod paymentMethod;
            try {
                paymentMethod = PaymentMethod.valueOf(paymentMethodStr.trim().toUpperCase());
                log.info("Payment method validated: {}", paymentMethod);
            } catch (IllegalArgumentException e) {
                log.error("Invalid payment method: {}", paymentMethodStr, e);
                redirectAttributes.addFlashAttribute("error", "Phương thức thanh toán không hợp lệ");
                return "redirect:/checkout/quick-order/payment/" + orderId;
            }

            // ✅ CẬP NHẬT payment method cho order
            order.setPaymentMethod(paymentMethod);
            orderRepository.save(order);

            log.info("Updated payment method for order {}: {}", orderId, paymentMethod);

            // ✅ Redirect theo payment method
            switch (paymentMethod) {
                case VNPAY:
                    log.info("Redirecting to VNPay for order: {}", orderId);
                    return "redirect:/payment/vnpay/" + orderId;

                case MOMO:
                    log.info("Redirecting to MoMo for order: {}", orderId);
                    return "redirect:/payment/momo/" + orderId;

                case CASH_ON_DELIVERY:
                    log.info("COD order confirmed: {}", orderId);

                    // ✅ Cập nhật trạng thái
                    order.setStatus(OrderStatus.CONFIRMED);
                    orderRepository.save(order);

                    redirectAttributes.addFlashAttribute("success",
                            "✅ Đặt món thành công! Vui lòng chuẩn bị tiền mặt khi nhận hàng.");
                    return "redirect:/orders/" + orderId;

                case BANK_TRANSFER:
                    log.info("Bank transfer order confirmed: {}", orderId);

                    redirectAttributes.addFlashAttribute("success",
                            "✅ Đặt món thành công! Vui lòng chuyển khoản theo thông tin đã cung cấp.");
                    return "redirect:/orders/" + orderId;

                default:
                    log.warn("Unknown payment method: {}", paymentMethod);
                    redirectAttributes.addFlashAttribute("success", "Đặt món thành công!");
                    return "redirect:/orders/" + orderId;
            }

        } catch (Exception e) {
            log.error("Error processing quick order payment", e);
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/checkout/quick-order/payment/" + orderId;
        }
    }


    /**
     * Helper method format tiền
     */
    private String formatCurrency(BigDecimal amount) {
        return String.format("%,d VNĐ", amount.longValue());
    }
}