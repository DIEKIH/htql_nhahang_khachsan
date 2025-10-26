package com.example.htql_nhahang_khachsan.controller;

import com.example.htql_nhahang_khachsan.config.Hotel_VNPayConfig;
import com.example.htql_nhahang_khachsan.entity.OrderEntity;
import com.example.htql_nhahang_khachsan.entity.OrderItemEntity;
import com.example.htql_nhahang_khachsan.service.Hotel_VnPayService;
import com.example.htql_nhahang_khachsan.service.OrderService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final Hotel_VnPayService hotelVnPayService;
    private final OrderService orderService;

    /**
     * Thanh toán VNPay cho đặt món (Order)
     */
    @GetMapping("/payment/vnpay/{orderId}")
    public String payWithVNPay(@PathVariable Long orderId,
                               HttpServletRequest request,
                               RedirectAttributes redirectAttributes) {
        try {
            log.info("Creating VNPay payment for order: {}", orderId);

            OrderEntity order = orderService.getOrderById(orderId);

            if (order == null) {
                log.error("Order not found: {}", orderId);
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy đơn hàng!");
                return "redirect:/cart";
            }

            long totalAmount = order.getTotalAmount().longValue();
            log.info("Order total amount: {} VND", totalAmount);

            // VNPay yêu cầu nhân 100
            long amountForVnPay = totalAmount * 100;

            String orderInfo = "Thanh toan don hang #" + orderId;
            String orderCode = String.valueOf(orderId);

            // Sử dụng Hotel_VnPayService cho đặt món
            String paymentUrl = hotelVnPayService.createPaymentUrl(
                    amountForVnPay,
                    orderInfo,
                    orderCode,
                    request
            );

            log.info("VNPay payment URL created successfully");
            return "redirect:" + paymentUrl;

        } catch (Exception e) {
            log.error("Error creating VNPay payment: ", e);
            redirectAttributes.addFlashAttribute("error", "Có lỗi khi tạo thanh toán: " + e.getMessage());
            return "redirect:/cart";
        }
    }

    /**
     * Xử lý callback từ VNPay cho đặt món
     */
    @GetMapping("/checkout/payment/vnpay-return")
    public String vnPayReturnForOrder(HttpServletRequest request,
                                      Model model,
                                      RedirectAttributes redirectAttributes) {
        try {
            log.info("VNPay callback received for ORDER payment");

            Map<String, String> params = new HashMap<>();
            for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
                params.put(entry.getKey(), entry.getValue()[0]);
                log.debug("VNPay param: {} = {}", entry.getKey(), entry.getValue()[0]);
            }

            String orderId = params.get("vnp_TxnRef");
            String responseCode = params.get("vnp_ResponseCode");
            String vnp_SecureHash = params.remove("vnp_SecureHash");

            log.info("Order ID: {}, Response Code: {}", orderId, responseCode);

            // Verify signature using Hotel_VNPayConfig
            params.remove("vnp_SecureHash");
            params.remove("vnp_SecureHashType");
            String computedHash = Hotel_VNPayConfig.hashAllFields(params);

            if (!computedHash.equals(vnp_SecureHash)) {
                log.error("Invalid signature! Computed: {}, Received: {}", computedHash, vnp_SecureHash);
                model.addAttribute("error", "Chữ ký không hợp lệ! Giao dịch có thể bị giả mạo.");
                model.addAttribute("orderId", orderId);
                return "customer/orders/payment_failed";
            }

            // Check response code
            if ("00".equals(responseCode)) {
                log.info("Payment successful for order: {}", orderId);

                // Cập nhật trạng thái thanh toán
                orderService.updatePaymentStatus(Long.parseLong(orderId), true);

                // Lấy thông tin đơn hàng
                OrderEntity order = orderService.getOrderById(Long.parseLong(orderId));
                List<OrderItemEntity> orderItems = order.getItems();

                model.addAttribute("order", order);
                model.addAttribute("orderItems", orderItems);
                model.addAttribute("paymentMethod", "VNPay");

                return "customer/orders/confirmation";

            } else {
                log.warn("Payment failed for order: {}, code: {}", orderId, responseCode);

                String errorMessage = getVNPayErrorMessage(responseCode);
                model.addAttribute("error", errorMessage);
                model.addAttribute("orderId", orderId);
                model.addAttribute("responseCode", responseCode);

                return "customer/orders/payment_failed";
            }

        } catch (Exception e) {
            log.error("Error processing VNPay callback: ", e);
            model.addAttribute("error", "Có lỗi xảy ra khi xử lý thanh toán: " + e.getMessage());
            return "customer/orders/payment_failed";
        }
    }

    /**
     * Lấy thông báo lỗi từ mã response code của VNPay
     */
    private String getVNPayErrorMessage(String responseCode) {
        switch (responseCode) {
            case "07":
                return "Giao dịch bị nghi ngờ gian lận";
            case "09":
                return "Thẻ/Tài khoản của khách hàng chưa đăng ký dịch vụ InternetBanking";
            case "10":
                return "Khách hàng xác thực thông tin thẻ/tài khoản không đúng quá 3 lần";
            case "11":
                return "Đã hết hạn chờ thanh toán";
            case "12":
                return "Thẻ/Tài khoản của khách hàng bị khóa";
            case "13":
                return "Quý khách nhập sai mật khẩu xác thực giao dịch";
            case "24":
                return "Khách hàng hủy giao dịch";
            case "51":
                return "Tài khoản của quý khách không đủ số dư để thực hiện giao dịch";
            case "65":
                return "Tài khoản của Quý khách đã vượt quá hạn mức giao dịch trong ngày";
            case "75":
                return "Ngân hàng thanh toán đang bảo trì";
            case "79":
                return "Giao dịch vượt quá số tiền cho phép";
            default:
                return "Giao dịch thất bại. Mã lỗi: " + responseCode;
        }
    }

    /**
     * MoMo payment - Placeholder
     */
    @GetMapping("/payment/momo/{orderId}")
    public String payWithMoMo(@PathVariable Long orderId,
                              RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("error",
                "Tính năng thanh toán MoMo đang được phát triển. Vui lòng chọn phương thức khác.");
        return "redirect:/checkout/payment";
    }
}