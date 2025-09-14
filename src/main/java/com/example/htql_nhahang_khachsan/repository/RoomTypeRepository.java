package com.example.htql_nhahang_khachsan.repository;

import com.example.htql_nhahang_khachsan.entity.RoomTypeEntity;
import com.example.htql_nhahang_khachsan.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}
