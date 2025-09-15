package com.example.htql_nhahang_khachsan.repository;

import com.example.htql_nhahang_khachsan.entity.MenuCategoryEntity;
import com.example.htql_nhahang_khachsan.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MenuCategoryRepository extends JpaRepository<MenuCategoryEntity, Long> {

    // Tìm theo branch và sắp xếp theo displayOrder rồi createdAt
    List<MenuCategoryEntity > findByBranchIdOrderByDisplayOrderAscCreatedAtDesc(Long branchId);

    // Tìm theo branch và status
    List<MenuCategoryEntity > findByBranchIdAndStatusOrderByDisplayOrderAsc(Long branchId, Status status);

    List<MenuCategoryEntity> findByBranchIdAndStatusOrderByDisplayOrderAscCreatedAtDesc(Long branchId, Status status);

    // Tìm theo branch và tên
    List<MenuCategoryEntity> findByBranchIdAndNameContainingIgnoreCaseOrderByDisplayOrderAscCreatedAtDesc(
            Long branchId, String name);

    // Tìm theo branch, tên và status
    List<MenuCategoryEntity> findByBranchIdAndNameContainingIgnoreCaseAndStatusOrderByDisplayOrderAscCreatedAtDesc(
            Long branchId, String name, Status status);

    // Tìm theo id và branch
    Optional<MenuCategoryEntity> findByIdAndBranchId(Long id, Long branchId);

    // Kiểm tra trùng tên trong cùng chi nhánh
    boolean existsByBranchIdAndNameIgnoreCase(Long branchId, String name);

    // Kiểm tra trùng tên trong cùng chi nhánh (trừ chính nó)
    boolean existsByBranchIdAndNameIgnoreCaseAndIdNot(Long branchId, String name, Long id);

    // Lấy displayOrder lớn nhất trong chi nhánh
    @Query("SELECT MAX(m.displayOrder) FROM MenuCategoryEntity m WHERE m.branch.id = :branchId")
    Integer findMaxDisplayOrderByBranchId(@Param("branchId") Long branchId);

    // Lấy danh sách category theo branchId và status, sắp xếp theo displayOrder tăng dần
    List<MenuCategoryEntity> findByBranchIdAndStatusOrderByDisplayOrder(Long branchId, Status status);

    // Đếm số lượng category theo branch
    long countByBranchId(Long branchId);

    // Đếm số lượng category active theo branch
    long countByBranchIdAndStatus(Long branchId, Status status);

}
