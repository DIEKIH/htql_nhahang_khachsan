package com.example.htql_nhahang_khachsan.service;

import com.example.htql_nhahang_khachsan.dto.CheckoutRequest;
import com.example.htql_nhahang_khachsan.entity.*;
import com.example.htql_nhahang_khachsan.enums.*;
import com.example.htql_nhahang_khachsan.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final UserRepository userRepository;

    private static final BigDecimal SERVICE_RATE = new BigDecimal("0.10");
    private static final BigDecimal VAT_RATE = new BigDecimal("0.08");

    @Transactional
    public Long createOrderFromCart(Long userId, CheckoutRequest request) {
        log.info("=== CREATE ORDER FROM CART ===");
        log.info("User ID: {}", userId);
        log.info("Payment Method: {}", request.getPaymentMethod());

        try {
            // Lấy user
            UserEntity user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
            log.info("User found: {}", user.getFullName());

            // Lấy giỏ hàng
            CartEntity cart = cartRepository.findByUserIdWithItems(userId)
                    .orElseThrow(() -> new RuntimeException("Cart not found for user: " + userId));
            log.info("Cart found with {} items", cart.getItems().size());

            if (cart.getItems().isEmpty()) {
                throw new RuntimeException("Giỏ hàng trống");
            }

            // Tính tổng
            BigDecimal subtotal = cart.getItems().stream()
                    .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal serviceCharge = subtotal.multiply(SERVICE_RATE)
                    .setScale(0, RoundingMode.HALF_UP);
            BigDecimal vat = subtotal.add(serviceCharge).multiply(VAT_RATE)
                    .setScale(0, RoundingMode.HALF_UP);
            BigDecimal totalAmount = subtotal.add(serviceCharge).add(vat);

            log.info("Order calculation - Subtotal: {}, Service: {}, VAT: {}, Total: {}",
                    subtotal, serviceCharge, vat, totalAmount);

            // Parse payment method
            PaymentMethod paymentMethod;
            try {
                paymentMethod = PaymentMethod.valueOf(request.getPaymentMethod());
                log.info("Payment method parsed: {}", paymentMethod);
            } catch (Exception e) {
                log.error("Failed to parse payment method: {}", request.getPaymentMethod(), e);
                throw new IllegalArgumentException("Phương thức thanh toán không hợp lệ: " + request.getPaymentMethod());
            }

            // Tạo đơn hàng
            OrderEntity order = OrderEntity.builder()
                    .branch(cart.getBranch())
                    .customer(user)
                    .customerName(request.getCustomerName())
                    .customerPhone(request.getCustomerPhone())
                    .customerAddress(request.getCustomerAddress())
                    .orderType(OrderType.ONLINE)
                    .paymentStatus(PaymentStatus.PENDING)
                    .paymentMethod(paymentMethod)
                    .notes(request.getOrderNotes())
                    .totalAmount(totalAmount)
                    .discountAmount(BigDecimal.ZERO)
                    .finalAmount(totalAmount)
                    .status(OrderStatus.PENDING)
                    .orderTime(LocalDateTime.now())
                    .build();

            order = orderRepository.save(order);
            log.info("Order saved with ID: {} and code: {}", order.getId(), order.getOrderCode());

            // Tạo chi tiết đơn hàng
            int itemCount = 0;
            for (CartItemEntity cartItem : cart.getItems()) {
                OrderItemEntity orderItem = OrderItemEntity.builder()
                        .order(order)
                        .menuItem(cartItem.getMenuItem())
                        .quantity(cartItem.getQuantity())
                        .unitPrice(cartItem.getUnitPrice())
                        .totalPrice(cartItem.getSubtotal())
                        .status(OrderItemStatus.PENDING)
                        .notes(cartItem.getNotes())
                        .build();
                orderItemRepository.save(orderItem);
                itemCount++;
            }
            log.info("Created {} order items", itemCount);

            return order.getId();

        } catch (Exception e) {
            log.error("Error creating order from cart", e);
            throw e;
        }
    }

    public OrderEntity getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with ID: " + orderId));
    }

    public List<OrderEntity> getUserOrders(Long userId) {
        return orderRepository.findByCustomerIdOrderByOrderTimeDesc(userId);
    }

    @Transactional
    public void updateOrderStatus(Long orderId, OrderStatus status) {
        OrderEntity order = getOrderById(orderId);
        order.setStatus(status);

        if (status == OrderStatus.COMPLETED) {
            order.setCompletedTime(LocalDateTime.now());
        }

        orderRepository.save(order);
    }

    @Transactional
    public void updatePaymentStatus(Long orderId, PaymentStatus status) {
        OrderEntity order = getOrderById(orderId);
        order.setPaymentStatus(status);
        orderRepository.save(order);
    }

    @Transactional
    public void updatePaymentStatus(Long orderId, boolean paid) {
        OrderEntity order = getOrderById(orderId);
        order.setPaymentStatus(paid ? PaymentStatus.PAID : PaymentStatus.PENDING);
        orderRepository.save(order);
        log.info("Updated payment status for order {}: {}", orderId, paid ? "PAID" : "PENDING");
    }

    public long getTotalAmount(Long orderId) {
        OrderEntity order = getOrderById(orderId);
        return order.getTotalAmount().longValue();
    }
}