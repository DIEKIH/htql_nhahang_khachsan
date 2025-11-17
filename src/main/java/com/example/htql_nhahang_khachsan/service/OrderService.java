package com.example.htql_nhahang_khachsan.service;

import com.example.htql_nhahang_khachsan.dto.CheckoutRequest;
import com.example.htql_nhahang_khachsan.dto.QuickOrderRequest;
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

import java.time.format.DateTimeFormatter;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final MenuItemRepository menuItemRepository;
    private final BranchRepository branchRepository;

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

    // Thêm vào OrderService.java

    /**
     * Tạo đơn hàng nhanh từ 1 món ăn (bỏ qua giỏ hàng)
     * CHÚ Ý: Fix theo entity thực tế của bạn
     */
    @Transactional
    public Long createQuickOrder(Long userId, QuickOrderRequest request) {
        log.info("Creating quick order for user: {}, menu item: {}", userId, request.getMenuItemId());

        // 1. Lấy thông tin user
        UserEntity customer = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        // 2. Lấy thông tin món ăn
        MenuItemEntity menuItem = menuItemRepository.findById(request.getMenuItemId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy món ăn"));

        if (!menuItem.getIsAvailable()) {
            throw new RuntimeException("Món ăn hiện không khả dụng");
        }

        // 3. Lấy thông tin chi nhánh từ category
        BranchEntity branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chi nhánh"));

        // 4. Tính toán giá
        BigDecimal unitPrice = menuItem.getPrice(); // Giá món ăn
        BigDecimal quantity = BigDecimal.valueOf(request.getQuantity());
        BigDecimal subtotal = unitPrice.multiply(quantity);

        // Phí phục vụ 10%
        BigDecimal serviceCharge = subtotal.multiply(SERVICE_RATE)
                .setScale(0, RoundingMode.HALF_UP);

        // VAT 8% (tính trên subtotal + service charge)
        BigDecimal vat = subtotal.add(serviceCharge).multiply(VAT_RATE)
                .setScale(0, RoundingMode.HALF_UP);

        // Tổng tiền
        BigDecimal totalAmount = subtotal.add(serviceCharge).add(vat);

        log.info("Quick Order calculation - Subtotal: {}, Service: {}, VAT: {}, Total: {}",
                subtotal, serviceCharge, vat, totalAmount);

        // 5. Parse payment method
        PaymentMethod paymentMethod;
        try {
            paymentMethod = PaymentMethod.valueOf(request.getPaymentMethod());
            log.info("Payment method parsed: {}", paymentMethod);
        } catch (Exception e) {
            log.error("Failed to parse payment method: {}", request.getPaymentMethod(), e);
            throw new IllegalArgumentException("Phương thức thanh toán không hợp lệ: " + request.getPaymentMethod());
        }

        // 6. Tạo đơn hàng (theo entity thực tế của bạn)
        OrderEntity order = OrderEntity.builder()
                .customer(customer)
                .branch(branch)
                .orderCode(generateOrderCode()) // Tạo mã đơn hàng
                .customerName(request.getCustomerName())
                .customerPhone(request.getCustomerPhone())
                .customerAddress(request.getCustomerAddress())
                .notes(request.getOrderNotes()) // Ghi chú chung
                .totalAmount(totalAmount)
                .discountAmount(BigDecimal.ZERO)
                .finalAmount(totalAmount)
                .paymentMethod(paymentMethod)
                .paymentStatus(PaymentStatus.PENDING) // Chờ thanh toán
                .status(OrderStatus.PENDING) // Chờ xác nhận
                .orderType(OrderType.ONLINE) // Đặt online
                .orderTime(LocalDateTime.now())
                .build();

        order = orderRepository.save(order);
        log.info("Quick order created with ID: {}, code: {}", order.getId(), order.getOrderCode());

        // 7. Tạo order item (theo entity thực tế của bạn)
        OrderItemEntity orderItem = OrderItemEntity.builder()
                .order(order)
                .menuItem(menuItem)
                .quantity(request.getQuantity())
                .unitPrice(unitPrice)
                .totalPrice(subtotal) // = unitPrice * quantity
                .status(OrderItemStatus.PENDING) // Chờ xử lý
                .notes(request.getNotes()) // Ghi chú món ăn (VD: không hành, ít cay)
                .build();

        orderItemRepository.save(orderItem);
        log.info("Order item created for menu item: {}", menuItem.getId());

        return order.getId();
    }

    /**
     * Tạo mã đơn hàng duy nhất
     * Format: QO20250115123456 (QO + ngày + random 6 số)
     */
    private String generateOrderCode() {
        String datePrefix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomSuffix = String.format("%06d", new Random().nextInt(999999));
        return "QO" + datePrefix + randomSuffix;
    }

    /**
     * ✅ TẠO ORDER TỪ CHATBOT (không cần session)
     */
    @Transactional
    public Long createQuickOrderFromChatbot(QuickOrderRequest request) {
        log.info("=== CREATE QUICK ORDER FROM CHATBOT ===");
        log.info("Menu: {}, Qty: {}, Customer: {}",
                request.getMenuItemId(), request.getQuantity(), request.getCustomerName());

        // Validate
        MenuItemEntity menuItem = menuItemRepository.findById(request.getMenuItemId())
                .orElseThrow(() -> new IllegalArgumentException("Món ăn không tồn tại"));

        if (!menuItem.getIsAvailable()) {
            throw new IllegalArgumentException("Món ăn không còn phục vụ");
        }

        BranchEntity branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new IllegalArgumentException("Chi nhánh không tồn tại"));

        // ✅ Tính toán giá
        BigDecimal unitPrice = request.getUnitPrice();
        BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(request.getQuantity()));

        BigDecimal serviceCharge = subtotal.multiply(new BigDecimal("0.10"))
                .setScale(0, RoundingMode.HALF_UP);

        BigDecimal vat = subtotal.add(serviceCharge).multiply(new BigDecimal("0.08"))
                .setScale(0, RoundingMode.HALF_UP);

        BigDecimal totalAmount = subtotal.add(serviceCharge).add(vat);

        // ✅ Tạo order
        OrderEntity order = OrderEntity.builder()
                .branch(branch)
                .orderType(OrderType.ONLINE)
                .customerName(request.getCustomerName())
                .customerPhone(request.getCustomerPhone())
                .customerAddress(request.getCustomerAddress())
                .notes(request.getOrderNotes())
                .paymentStatus(PaymentStatus.PENDING)
                .paymentMethod(null) // Chưa chọn
                .status(OrderStatus.PENDING)
                .totalAmount(totalAmount)
                .finalAmount(totalAmount)
                .build();

        // ✅ Tạo order item
        OrderItemEntity orderItem = OrderItemEntity.builder()
                .order(order)
                .menuItem(menuItem)
                .quantity(request.getQuantity())
                .unitPrice(unitPrice)
                .totalPrice(subtotal)
                .status(OrderItemStatus.PENDING)
                .notes(request.getNotes())
                .build();

        order.setItems(List.of(orderItem));

        // ✅ Lưu
        OrderEntity savedOrder = orderRepository.save(order);

        log.info("✅ Quick order created: {}", savedOrder.getId());

        return savedOrder.getId();
    }
}