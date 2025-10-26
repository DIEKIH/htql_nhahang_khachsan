package com.example.htql_nhahang_khachsan.repository;


import com.example.htql_nhahang_khachsan.entity.*;
import com.example.htql_nhahang_khachsan.enums.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    List<OrderEntity> findByCustomerIdOrderByOrderTimeDesc(Long customerId);

    List<OrderEntity> findByBranchIdOrderByOrderTimeDesc(Long branchId);

    List<OrderEntity> findByStatus(OrderStatus status);

    List<OrderEntity> findByStatusAndBranchId(OrderStatus status, Long branchId);

    @Query("SELECT o FROM OrderEntity o WHERE o.customer.id = :customerId " +
            "AND o.status = :status ORDER BY o.orderTime DESC")
    List<OrderEntity> findByCustomerIdAndStatus(Long customerId, OrderStatus status);

    @Query("SELECT o FROM OrderEntity o WHERE o.orderTime BETWEEN :startDate AND :endDate " +
            "AND o.branch.id = :branchId ORDER BY o.orderTime DESC")
    List<OrderEntity> findByBranchIdAndOrderTimeBetween(Long branchId,
                                                        LocalDateTime startDate,
                                                        LocalDateTime endDate);

    Optional<OrderEntity> findByOrderCode(String orderCode);




    List<OrderEntity> findByBranchIdAndOrderCodeContaining(Long branchId, String orderCode);

    @Query("SELECT o FROM OrderEntity o WHERE o.branch.id = :branchId " +
            "AND (:status IS NULL OR o.status = :status) " +
            "AND (:orderType IS NULL OR o.orderType = :orderType) " +
            "AND (:paymentStatus IS NULL OR o.paymentStatus = :paymentStatus) " +
            "AND (:fromDate IS NULL OR o.orderTime >= :fromDate) " +
            "AND (:toDate IS NULL OR o.orderTime <= :toDate) " +
            "ORDER BY o.orderTime DESC")
    List<OrderEntity> findByBranchIdAndFilters(
            @Param("branchId") Long branchId,
            @Param("status") OrderStatus status,
            @Param("orderType") OrderType orderType,
            @Param("paymentStatus") PaymentStatus paymentStatus,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate
    );


    List<OrderEntity> findByBranchIdAndStatus(Long branchId, OrderStatus status);
    List<OrderEntity> findByBranchId(Long branchId);
    Optional<OrderEntity> findByIdAndBranchId(Long id, Long branchId);
    long countByBranchIdAndOrderTimeBetween(Long branchId, LocalDateTime startDate, LocalDateTime endDate);
    long countByBranchIdAndStatus(Long branchId, OrderStatus status);

    @Query("SELECT COALESCE(SUM(o.finalAmount), 0) FROM OrderEntity o WHERE o.branch.id = :branchId AND o.status = 'COMPLETED' AND o.orderTime BETWEEN :startDate AND :endDate")
    BigDecimal sumRevenueByBranchAndDate(@Param("branchId") Long branchId,
                                         @Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);
}
