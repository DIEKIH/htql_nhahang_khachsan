package com.example.htql_nhahang_khachsan.repository;

import com.example.htql_nhahang_khachsan.entity.MenuCategoryEntity;
import com.example.htql_nhahang_khachsan.entity.MenuItemEntity;
import com.example.htql_nhahang_khachsan.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItemEntity, Long> {

    // Find by branch
    List<MenuItemEntity> findByCategoryBranchIdOrderByCreatedAtDesc(Long branchId);

    // Find by branch and status
    List<MenuItemEntity> findByCategoryBranchIdAndStatusOrderByCreatedAtDesc(Long branchId, Status status);

    // Find by branch and category
    List<MenuItemEntity> findByCategoryBranchIdAndCategoryIdOrderByCreatedAtDesc(Long branchId, Long categoryId);

    // Find by branch, category and status
    List<MenuItemEntity> findByCategoryBranchIdAndCategoryIdAndStatusOrderByCreatedAtDesc(Long branchId, Long categoryId, Status status);

    // Search by name
    List<MenuItemEntity> findByCategoryBranchIdAndNameContainingIgnoreCaseOrderByCreatedAtDesc(Long branchId, String name);

    // Search by name and status
    List<MenuItemEntity> findByCategoryBranchIdAndNameContainingIgnoreCaseAndStatusOrderByCreatedAtDesc(Long branchId, String name, Status status);

    // Search by name and category
    List<MenuItemEntity> findByCategoryBranchIdAndNameContainingIgnoreCaseAndCategoryIdOrderByCreatedAtDesc(Long branchId, String name, Long categoryId);

    // Search by name, category and status
    List<MenuItemEntity> findByCategoryBranchIdAndNameContainingIgnoreCaseAndCategoryIdAndStatusOrderByCreatedAtDesc(Long branchId, String name, Long categoryId, Status status);

    // Find by id and branch
    Optional<MenuItemEntity> findByIdAndCategoryBranchId(Long id, Long branchId);

    // Check existence by name and branch
    boolean existsByCategoryBranchIdAndNameIgnoreCase(Long branchId, String name);

    // Check existence by name and branch (excluding current item)
    boolean existsByCategoryBranchIdAndNameIgnoreCaseAndIdNot(Long branchId, String name, Long id);

    // Find by category
    List<MenuItemEntity> findByCategory(MenuCategoryEntity category);

    // Count by category and status
    long countByCategoryAndStatus(MenuCategoryEntity category, Status status);


    List<MenuItemEntity> findByCategoryIdAndStatusAndIsAvailable(Long categoryId, Status status, Boolean isAvailable);
    List<MenuItemEntity> findByCategoryBranchIdAndStatusAndIsAvailable(Long branchId, Status status, Boolean isAvailable);
    long countByCategoryIdAndStatus(Long categoryId, Status status);

    @Query("SELECT mi FROM MenuItemEntity mi WHERE mi.category.branch.id = :branchId AND mi.status = 'ACTIVE' AND mi.isAvailable = true ORDER BY mi.category.displayOrder, mi.name")
    List<MenuItemEntity> findAvailableMenuItemsByBranch(@Param("branchId") Long branchId);

    @Query("SELECT mi FROM MenuItemEntity mi WHERE mi.category.id = :categoryId AND mi.status = 'ACTIVE' AND mi.isAvailable = true ORDER BY mi.name")
    List<MenuItemEntity> findAvailableMenuItemsByCategory(@Param("categoryId") Long categoryId);

    List<MenuItemEntity> findByStatusAndIsAvailable(Status status, Boolean isAvailable);

}
