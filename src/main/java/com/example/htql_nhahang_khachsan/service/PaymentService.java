package com.example.htql_nhahang_khachsan.service;

import com.example.htql_nhahang_khachsan.entity.PaymentEntity;
import com.example.htql_nhahang_khachsan.entity.RoomBookingEntity;
import com.example.htql_nhahang_khachsan.enums.PaymentMethod;
import com.example.htql_nhahang_khachsan.enums.PaymentStatus;
import com.example.htql_nhahang_khachsan.repository.PaymentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;

    /**
     * Xử lý thanh toán
     */
    public PaymentEntity processPayment(BigDecimal amount, PaymentMethod method) {
        PaymentEntity payment = PaymentEntity.builder()
                .amount(amount)
                .method(method)
                .status(PaymentStatus.PENDING)
                .build();

        try {
            // Xử lý thanh toán theo từng phương thức
            switch (method) {
                case VNPAY:
                    return processVNPayPayment(payment);
                case BANK_TRANSFER:
                    return processBankTransferPayment(payment);
                case CREDIT_CARD:
                    return processCreditCardPayment(payment);
                case MOMO:
                    return processMoMoPayment(payment);
                case ZALOPAY:
                    return processZaloPayPayment(payment);
                case CASH:
                    return processCashPayment(payment);
                default:
                    throw new IllegalArgumentException("Unsupported payment method");
            }
        } catch (Exception e) {
            log.error("Payment processing failed", e);
            payment.setStatus(PaymentStatus.FAILED);
            payment.setGatewayResponse(e.getMessage());
            return payment;
        }
    }

    /**
     * Xử lý thanh toán VNPay
     */
    private PaymentEntity processVNPayPayment(PaymentEntity payment) {
        // TODO: Implement VNPay integration
        // Tạm thời giả lập thành công
        payment.setStatus(PaymentStatus.PAID);
        payment.setProcessedAt(LocalDateTime.now());
        payment.setGatewayResponse("{\"success\": true, \"message\": \"Payment successful\"}");

        log.info("VNPay payment processed: {}", payment.getTransactionId());
        return payment;
    }

    /**
     * Xử lý chuyển khoản ngân hàng
     */
    private PaymentEntity processBankTransferPayment(PaymentEntity payment) {
        // Chuyển khoản cần xác nhận thủ công
        payment.setStatus(PaymentStatus.PENDING);
        payment.setGatewayResponse("{\"message\": \"Awaiting bank transfer confirmation\"}");

        log.info("Bank transfer payment created: {}", payment.getTransactionId());
        return payment;
    }

    /**
     * Xử lý thanh toán thẻ
     */
    private PaymentEntity processCreditCardPayment(PaymentEntity payment) {
        // TODO: Implement credit card gateway integration
        // Tạm thời giả lập thành công
        payment.setStatus(PaymentStatus.PAID);
        payment.setProcessedAt(LocalDateTime.now());
        payment.setGatewayResponse("{\"success\": true, \"cardType\": \"VISA\"}");

        log.info("Credit card payment processed: {}", payment.getTransactionId());
        return payment;
    }

    /**
     * Xử lý thanh toán MoMo
     */
    private PaymentEntity processMoMoPayment(PaymentEntity payment) {
        // TODO: Implement MoMo integration
        payment.setStatus(PaymentStatus.PAID);
        payment.setProcessedAt(LocalDateTime.now());
        payment.setGatewayResponse("{\"success\": true, \"message\": \"MoMo payment successful\"}");

        log.info("MoMo payment processed: {}", payment.getTransactionId());
        return payment;
    }

    /**
     * Xử lý thanh toán ZaloPay
     */
    private PaymentEntity processZaloPayPayment(PaymentEntity payment) {
        // TODO: Implement ZaloPay integration
        payment.setStatus(PaymentStatus.PAID);
        payment.setProcessedAt(LocalDateTime.now());
        payment.setGatewayResponse("{\"success\": true, \"message\": \"ZaloPay payment successful\"}");

        log.info("ZaloPay payment processed: {}", payment.getTransactionId());
        return payment;
    }

    /**
     * Xử lý thanh toán tiền mặt
     */
    private PaymentEntity processCashPayment(PaymentEntity payment) {
        // Tiền mặt thanh toán khi check-in
        payment.setStatus(PaymentStatus.PENDING);
        payment.setGatewayResponse("{\"message\": \"Cash payment at check-in\"}");

        log.info("Cash payment recorded: {}", payment.getTransactionId());
        return payment;
    }

    /**
     * Lưu payment
     */
    public PaymentEntity savePayment(PaymentEntity payment) {
        return paymentRepository.save(payment);
    }

    /**
     * Xác nhận thanh toán (dùng cho bank transfer)
     */
    @Transactional
    public PaymentEntity confirmPayment(String transactionId, Long staffId) {
        PaymentEntity payment = paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new EntityNotFoundException("Payment not found"));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new IllegalStateException("Payment is not in PENDING status");
        }

        payment.setStatus(PaymentStatus.PAID);
        payment.setProcessedBy(staffId);
        payment.setProcessedAt(LocalDateTime.now());

        // Cập nhật booking status nếu thanh toán full
        if (payment.getRoomBooking() != null) {
            RoomBookingEntity booking = payment.getRoomBooking();
            if (payment.getAmount().compareTo(booking.getTotalAmount()) >= 0) {
                booking.setPaymentStatus(PaymentStatus.PAID);
            }
        }

        return paymentRepository.save(payment);
    }

    /**
     * Hoàn tiền
     */
    @Transactional
    public PaymentEntity refundPayment(String transactionId, BigDecimal refundAmount, Long staffId) {
        PaymentEntity payment = paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new EntityNotFoundException("Payment not found"));

        if (payment.getStatus() != PaymentStatus.PAID) {
            throw new IllegalStateException("Cannot refund unsuccessful payment");
        }

        if (refundAmount.compareTo(payment.getAmount()) > 0) {
            throw new IllegalArgumentException("Refund amount cannot exceed payment amount");
        }

        // Tạo payment mới cho refund
        PaymentEntity refund = PaymentEntity.builder()
                .amount(refundAmount.negate()) // Số âm
                .method(payment.getMethod())
                .status(PaymentStatus.REFUNDED)
                .roomBooking(payment.getRoomBooking())
                .processedBy(staffId)
                .processedAt(LocalDateTime.now())
                .gatewayResponse("{\"type\": \"refund\", \"originalTransaction\": \"" + transactionId + "\"}")
                .build();

        paymentRepository.save(refund);

        // Cập nhật booking status
        if (payment.getRoomBooking() != null) {
            RoomBookingEntity booking = payment.getRoomBooking();
            booking.setPaymentStatus(PaymentStatus.REFUNDED);
        }

        log.info("Refund processed: {} for original transaction: {}", refund.getTransactionId(), transactionId);
        return refund;
    }

    /**
     * Lấy lịch sử thanh toán của booking
     */
    public List<PaymentEntity> getPaymentsByBooking(Long bookingId) {
        return paymentRepository.findByRoomBookingId(bookingId);
    }

    /**
     * Tính tổng đã thanh toán của booking
     */
    public BigDecimal getTotalPaidAmount(Long bookingId) {
        List<PaymentEntity> payments = getPaymentsByBooking(bookingId);
        return payments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.PAID)
                .map(PaymentEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}