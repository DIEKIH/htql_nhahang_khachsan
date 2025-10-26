package com.example.htql_nhahang_khachsan.repository;

import com.example.htql_nhahang_khachsan.entity.OrderItemEntity;
import com.example.htql_nhahang_khachsan.enums.OrderItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItemEntity, Long> {

    List<OrderItemEntity> findByOrderId(Long orderId);

    List<OrderItemEntity> findByStatus(OrderItemStatus status);

    @Query("SELECT oi FROM OrderItemEntity oi WHERE oi.order.id = :orderId " +
            "AND oi.status = :status")
    List<OrderItemEntity> findByOrderIdAndStatus(Long orderId, OrderItemStatus status);

    @Query("SELECT oi FROM OrderItemEntity oi " +
            "WHERE oi.order.branch.id = :branchId " +
            "AND oi.status = :status " +
            "ORDER BY oi.order.orderTime ASC")
    List<OrderItemEntity> findByBranchIdAndStatus(Long branchId, OrderItemStatus status);

}
