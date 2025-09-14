package com.example.htql_nhahang_khachsan.repository;

import com.example.htql_nhahang_khachsan.entity.RoomEntity;
import com.example.htql_nhahang_khachsan.enums.RoomStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<RoomEntity, Long>, JpaSpecificationExecutor<RoomEntity> {

    List<RoomEntity> findByRoomTypeBranchIdOrderByFloorAscRoomNumberAsc(Long branchId);

    Optional<RoomEntity> findByIdAndRoomTypeBranchId(Long id, Long branchId);

    boolean existsByRoomNumberAndRoomTypeBranchId(String roomNumber, Long branchId);

    List<RoomEntity> findByRoomTypeIdOrderByFloorAscRoomNumberAsc(Long roomTypeId);

    List<RoomEntity> findByStatusAndRoomTypeBranchId(RoomStatus status, Long branchId);

    List<RoomEntity> findByFloorAndRoomTypeBranchId(Integer floor, Long branchId);

    @Query("SELECT DISTINCT r.floor FROM RoomEntity r WHERE r.roomType.branch.id = :branchId ORDER BY r.floor")
    List<Integer> findDistinctFloorsByBranchId(@Param("branchId") Long branchId);

    @Query("SELECT COUNT(r) FROM RoomEntity r WHERE r.roomType.branch.id = :branchId AND r.status = :status")
    long countByBranchIdAndStatus(@Param("branchId") Long branchId, @Param("status") RoomStatus status);
}
