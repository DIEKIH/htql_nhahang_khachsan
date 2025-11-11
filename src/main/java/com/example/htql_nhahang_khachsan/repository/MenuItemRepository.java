package com.example.htql_nhahang_khachsan.repository;

import com.example.htql_nhahang_khachsan.entity.MenuCategoryEntity;
import com.example.htql_nhahang_khachsan.entity.MenuItemEntity;
import com.example.htql_nhahang_khachsan.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
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

    @Query("SELECT m FROM MenuItemEntity m WHERE m.id = :id")
    Optional<MenuItemEntity> findById(@Param("id") Long id);

    // ✅ THÊM - Method cần thiết cho ReviewRecommendationService
    List<MenuItemEntity> findByIsAvailableTrue();

    List<MenuItemEntity> findByIsAvailableTrueOrderByCreatedAtDesc();

    @Query("SELECT m FROM MenuItemEntity m WHERE m.category.branch.id = :branchId AND m.status = :status")
    List<MenuItemEntity> findByBranchIdAndStatus(@Param("branchId") Long branchId,
                                                 @Param("status") Status status);

    @Query("SELECT m FROM MenuItemEntity m WHERE m.category.branch.id = :branchId")
    List<MenuItemEntity> findByBranchId(@Param("branchId") Long branchId);

    boolean existsByCategoryIdAndNameIgnoreCase(Long categoryId, String name);

    boolean existsByCategoryIdAndNameIgnoreCaseAndIdNot(Long categoryId, String name, Long id);


    // Tìm món ăn theo từ khóa
    @Query("SELECT m FROM MenuItemEntity m WHERE " +
            "LOWER(m.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(m.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<MenuItemEntity> searchByKeyword(@Param("keyword") String keyword);

    // Tìm món ăn theo khoảng giá
    List<MenuItemEntity> findByPriceBetweenAndStatusOrderByPriceAsc(
            BigDecimal minPrice, BigDecimal maxPrice, Status status);

    // Tìm món ăn có calories thấp (cho menu ăn kiêng)
    @Query("SELECT m FROM MenuItemEntity m WHERE m.calories <= :maxCalories AND m.status = :status ORDER BY m.calories ASC")
    List<MenuItemEntity> findLowCalorieItems(@Param("maxCalories") Integer maxCalories,
                                             @Param("status") Status status);

    // Tìm món ăn không chứa allergen cụ thể
    @Query("SELECT m FROM MenuItemEntity m WHERE " +
            "m.status = :status AND " +
            "(m.allergens IS NULL OR LOWER(m.allergens) NOT LIKE LOWER(CONCAT('%', :allergen, '%')))")
    List<MenuItemEntity> findItemsWithoutAllergen(@Param("allergen") String allergen,
                                                  @Param("status") Status status);

    // Đếm tổng số món ăn active
    long countByStatus(Status status);

    // Lấy món ăn mới nhất
    List<MenuItemEntity> findTop10ByStatusOrderByCreatedAtDesc(Status status);

}
