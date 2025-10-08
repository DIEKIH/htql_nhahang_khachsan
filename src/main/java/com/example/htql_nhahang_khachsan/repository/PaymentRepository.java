package com.example.htql_nhahang_khachsan.repository;

import com.example.htql_nhahang_khachsan.entity.PaymentEntity;
import com.example.htql_nhahang_khachsan.enums.PaymentMethod;
import com.example.htql_nhahang_khachsan.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {

    Optional<PaymentEntity> findByTransactionId(String transactionId);

    List<PaymentEntity> findByRoomBookingId(Long bookingId);

    List<PaymentEntity> findByOrderId(Long orderId);

    List<PaymentEntity> findByStatus(PaymentStatus status);

    List<PaymentEntity> findByMethodAndStatus(PaymentMethod method, PaymentStatus status);

    @Query("SELECT SUM(p.amount) FROM PaymentEntity p " +
            "WHERE p.status = 'SUCCESS' " +
            "AND p.processedAt BETWEEN :startDate AND :endDate")
    BigDecimal getTotalSuccessfulPayments(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT p.method, COUNT(p), SUM(p.amount) FROM PaymentEntity p " +
            "WHERE p.status = 'SUCCESS' " +
            "AND p.processedAt BETWEEN :startDate AND :endDate " +
            "GROUP BY p.method")
    List<Object[]> getPaymentStatsByMethod(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}
