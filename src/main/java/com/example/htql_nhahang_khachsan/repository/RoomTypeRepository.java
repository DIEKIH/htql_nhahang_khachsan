package com.example.htql_nhahang_khachsan.repository;

import com.example.htql_nhahang_khachsan.entity.RoomTypeEntity;
import com.example.htql_nhahang_khachsan.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoomTypeRepository extends JpaRepository<RoomTypeEntity, Long> {

    List<RoomTypeEntity> findByBranchIdOrderByCreatedAtDesc(Long branchId);

    List<RoomTypeEntity> findByBranchIdAndStatusOrderByCreatedAtDesc(Long branchId, Status status);

    List<RoomTypeEntity> findByBranchIdAndNameContainingIgnoreCaseOrderByCreatedAtDesc(Long branchId, String name);

    List<RoomTypeEntity> findByBranchIdAndNameContainingIgnoreCaseAndStatusOrderByCreatedAtDesc(
            Long branchId, String name, Status status);

    Optional<RoomTypeEntity> findByIdAndBranchId(Long id, Long branchId);

    boolean existsByBranchIdAndNameIgnoreCase(Long branchId, String name);

    boolean existsByBranchIdAndNameIgnoreCaseAndIdNot(Long branchId, String name, Long id);

    long countByBranchIdAndStatus(Long branchId, Status status);

    List<RoomTypeEntity> findByBranchId(Long branchId);

    List<RoomTypeEntity> findByBranchIdAndStatus(Long branchId, Status status);
    List<RoomTypeEntity> findByBranchIdOrderByPriceAsc(Long branchId);

    @Query("SELECT rt FROM RoomTypeEntity rt WHERE rt.branch.id = :branchId AND rt.status = 'ACTIVE'")
    List<RoomTypeEntity> findActiveRoomTypesByBranch(@Param("branchId") Long branchId);


    //chi tiết loại phòng
    /**
     * Tìm các loại phòng trong chi nhánh, loại trừ một loại phòng cụ thể, sắp xếp theo giá
     */
    List<RoomTypeEntity> findByBranchIdAndIdNotOrderByPriceAsc(Long branchId, Long excludeId);

    /**
     * Tìm các loại phòng theo chi nhánh
     */



    /**
     * Tìm các loại phòng đang có sẵn
     */
    List<RoomTypeEntity> findByStatusTrueOrderByPriceAsc();


    /**
     * Tìm các loại phòng theo khoảng giá
     */
    List<RoomTypeEntity> findByPriceBetweenOrderByPriceAsc(BigDecimal minPrice, BigDecimal maxPrice);

    /**
     * Tìm các loại phòng theo số người tối đa
     */
    List<RoomTypeEntity> findByMaxOccupancyGreaterThanEqualOrderByPriceAsc(Integer minOccupancy);

    /**
     * Tìm các loại phòng theo loại giường
     */
    List<RoomTypeEntity> findByBedTypeContainingIgnoreCaseOrderByPriceAsc(String bedType);
}
