package com.example.htql_nhahang_khachsan.repository;




import com.example.htql_nhahang_khachsan.entity.RestaurantTableEntity;
import com.example.htql_nhahang_khachsan.enums.TableStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RestaurantTableRepository extends JpaRepository<RestaurantTableEntity, Long> {

    List<RestaurantTableEntity> findByBranchIdOrderByTableNumberAsc(Long branchId);

    Optional<RestaurantTableEntity> findByIdAndBranchId(Long id, Long branchId);

    boolean existsByTableNumberAndBranchId(String tableNumber, Long branchId);

    boolean existsByTableNumberAndBranchIdAndIdNot(String tableNumber, Long branchId, Long id);

    @Query("SELECT t FROM RestaurantTableEntity t WHERE t.branch.id = :branchId " +
            "AND (:tableNumber IS NULL OR LOWER(t.tableNumber) LIKE LOWER(CONCAT('%', :tableNumber, '%'))) " +
            "AND (:status IS NULL OR t.status = :status) " +
            "AND (:capacity IS NULL OR t.capacity = :capacity) " +
            "ORDER BY t.tableNumber ASC")
    List<RestaurantTableEntity> searchTables(@Param("branchId") Long branchId,
                                             @Param("tableNumber") String tableNumber,
                                             @Param("status") TableStatus status,
                                             @Param("capacity") Integer capacity);

    List<RestaurantTableEntity> findByBranchIdOrderByTableNumber(Long branchId);
    List<RestaurantTableEntity> findByBranchIdAndStatus(Long branchId, TableStatus status);

    @Query("SELECT rt FROM RestaurantTableEntity rt WHERE rt.branch.id = :branchId AND rt.status IN :statuses ORDER BY rt.tableNumber")
    List<RestaurantTableEntity> findByBranchIdAndStatusIn(@Param("branchId") Long branchId,
                                                          @Param("statuses") List<TableStatus> statuses);

    @Query("SELECT rt FROM RestaurantTableEntity rt WHERE rt.branch.id = :branchId AND rt.capacity >= :minCapacity ORDER BY rt.capacity, rt.tableNumber")
    List<RestaurantTableEntity> findAvailableTablesByCapacity(@Param("branchId") Long branchId,
                                                              @Param("minCapacity") Integer minCapacity);
}