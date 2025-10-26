package com.example.htql_nhahang_khachsan.service;

import com.example.htql_nhahang_khachsan.entity.OrderEntity;
import com.example.htql_nhahang_khachsan.enums.OrderStatus;
import com.example.htql_nhahang_khachsan.enums.OrderType;
import com.example.htql_nhahang_khachsan.enums.PaymentStatus;
import com.example.htql_nhahang_khachsan.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManagerOrderService {

    private final OrderRepository orderRepository;

    public List<OrderEntity> getAllOrdersByBranch(Long branchId) {
        return orderRepository.findByBranchIdOrderByOrderTimeDesc(branchId);
    }

    public List<OrderEntity> searchOrders(Long branchId,
                                          String orderCode,
                                          OrderStatus status,
                                          OrderType orderType,
                                          PaymentStatus paymentStatus,
                                          LocalDate fromDate,
                                          LocalDate toDate) {

        LocalDateTime fromDateTime = fromDate != null ? fromDate.atStartOfDay() : null;
        LocalDateTime toDateTime = toDate != null ? toDate.atTime(LocalTime.MAX) : null;

        if (orderCode != null && !orderCode.trim().isEmpty()) {
            return orderRepository.findByBranchIdAndOrderCodeContaining(branchId, orderCode.trim());
        }

        return orderRepository.findByBranchIdAndFilters(branchId, status, orderType, paymentStatus, fromDateTime, toDateTime);
    }

    public OrderEntity getOrderById(Long orderId, Long branchId) {
        OrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        if (!order.getBranch().getId().equals(branchId)) {
            throw new RuntimeException("Đơn hàng không thuộc chi nhánh của bạn");
        }

        return order;
    }

    @Transactional
    public void updateOrderStatus(Long orderId, Long branchId, OrderStatus newStatus) {
        OrderEntity order = getOrderById(orderId, branchId);

        // Validate status transition
        validateStatusTransition(order.getStatus(), newStatus);

        order.setStatus(newStatus);

        if (newStatus == OrderStatus.COMPLETED) {
            order.setCompletedTime(LocalDateTime.now());
        }

        orderRepository.save(order);
        log.info("Updated order {} status to {}", orderId, newStatus);
    }

    @Transactional
    public void updatePaymentStatus(Long orderId, Long branchId, PaymentStatus newStatus) {
        OrderEntity order = getOrderById(orderId, branchId);
        order.setPaymentStatus(newStatus);
        orderRepository.save(order);
        log.info("Updated order {} payment status to {}", orderId, newStatus);
    }

    @Transactional
    public void confirmPayment(Long orderId, Long branchId) {
        OrderEntity order = getOrderById(orderId, branchId);

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new RuntimeException("Đơn hàng đã được thanh toán");
        }

        order.setPaymentStatus(PaymentStatus.PAID);
        orderRepository.save(order);
        log.info("Confirmed payment for order {}", orderId);
    }

    @Transactional
    public void cancelOrder(Long orderId, Long branchId, String reason) {
        OrderEntity order = getOrderById(orderId, branchId);

        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw new RuntimeException("Không thể hủy đơn hàng đã hoàn thành");
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new RuntimeException("Đơn hàng đã bị hủy");
        }

        order.setStatus(OrderStatus.CANCELLED);
        if (reason != null && !reason.trim().isEmpty()) {
            order.setNotes(order.getNotes() != null ?
                    order.getNotes() + "\nLý do hủy: " + reason :
                    "Lý do hủy: " + reason);
        }

        orderRepository.save(order);
        log.info("Cancelled order {}", orderId);
    }

    private void validateStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        if (currentStatus == OrderStatus.CANCELLED) {
            throw new RuntimeException("Không thể thay đổi trạng thái đơn hàng đã hủy");
        }

        if (currentStatus == OrderStatus.COMPLETED && newStatus != OrderStatus.COMPLETED) {
            throw new RuntimeException("Không thể thay đổi trạng thái đơn hàng đã hoàn thành");
        }
    }
}