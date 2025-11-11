package com.example.htql_nhahang_khachsan.repository;

import com.example.htql_nhahang_khachsan.entity.ReviewEntity;
import com.example.htql_nhahang_khachsan.enums.ReviewStatus;
import com.example.htql_nhahang_khachsan.enums.ReviewType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<ReviewEntity, Long> {

    // ==================== LOẠI PHÒNG ====================

    /**
     * ✅ Lấy review theo LOẠI PHÒNG (không phải branch)
     */
    Page<ReviewEntity> findByRoomTypeIdAndStatus(
            Long roomTypeId,
            ReviewStatus status,
            Pageable pageable);

    List<ReviewEntity> findByRoomTypeIdAndStatus(
            Long roomTypeId,
            ReviewStatus status);

    /**
     * ✅ Tìm review của customer cho loại phòng cụ thể
     */
    Optional<ReviewEntity> findByCustomerIdAndRoomTypeId(
            Long customerId,
            Long roomTypeId);

    /**
     * ✅ Kiểm tra đã review loại phòng này chưa
     */
    boolean existsByCustomerIdAndRoomTypeId(
            Long customerId,
            Long roomTypeId);

    // ==================== MÓN ĂN ====================

    /**
     * ✅ Lấy review theo MÓN ĂN
     */
    Page<ReviewEntity> findByMenuItemIdAndStatus(
            Long menuItemId,
            ReviewStatus status,
            Pageable pageable);

    List<ReviewEntity> findByMenuItemIdAndStatus(
            Long menuItemId,
            ReviewStatus status);

    /**
     * ✅ Tìm review của customer cho món ăn cụ thể
     */
    Optional<ReviewEntity> findByCustomerIdAndMenuItemId(
            Long customerId,
            Long menuItemId);

    /**
     * ✅ Kiểm tra đã review món ăn này chưa
     */
    boolean existsByCustomerIdAndMenuItemId(
            Long customerId,
            Long menuItemId);

    // ==================== BRANCH (giữ lại để tương thích) ====================

    Page<ReviewEntity> findByBranchIdAndTypeAndStatus(
            Long branchId,
            ReviewType type,
            ReviewStatus status,
            Pageable pageable);

    List<ReviewEntity> findByBranchIdAndTypeAndStatus(
            Long branchId,
            ReviewType type,
            ReviewStatus status);

    Optional<ReviewEntity> findByCustomerIdAndBranchIdAndType(
            Long customerId,
            Long branchId,
            ReviewType type);

    boolean existsByCustomerIdAndBranchIdAndType(
            Long customerId,
            Long branchId,
            ReviewType type);

    // ==================== BOOKING ====================

    /**
     * Kiểm tra đã review booking cụ thể chưa
     */
    boolean existsByRoomBookingIdAndCustomerId(Long bookingId, Long customerId);

    // ==================== STATISTICS ====================

    List<ReviewEntity> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    List<ReviewEntity> findByBranchIdOrderByCreatedAtDesc(Long branchId);

    /**
     * ✅ Tính rating trung bình theo loại phòng
     */
    @Query("SELECT AVG(r.rating) FROM ReviewEntity r " +
            "WHERE r.roomType.id = :roomTypeId AND r.status = :status")
    Double getAverageRatingByRoomType(
            @Param("roomTypeId") Long roomTypeId,
            @Param("status") ReviewStatus status);

    /**
     * ✅ Tính rating trung bình theo món ăn
     */
    @Query("SELECT AVG(r.rating) FROM ReviewEntity r " +
            "WHERE r.menuItem.id = :menuItemId AND r.status = :status")
    Double getAverageRatingByMenuItem(
            @Param("menuItemId") Long menuItemId,
            @Param("status") ReviewStatus status);

    @Query("SELECT AVG(r.rating) FROM ReviewEntity r " +
            "WHERE r.branch.id = :branchId AND r.type = :type AND r.status = :status")
    Double getAverageRatingByBranchAndType(
            @Param("branchId") Long branchId,
            @Param("type") ReviewType type,
            @Param("status") ReviewStatus status);

    long countByBranchIdAndTypeAndStatus(Long branchId, ReviewType type, ReviewStatus status);

    /**
     * ✅ Đếm review theo loại phòng
     */
    long countByRoomTypeIdAndStatus(Long roomTypeId, ReviewStatus status);

    /**
     * ✅ Đếm review theo món ăn
     */
    long countByMenuItemIdAndStatus(Long menuItemId, ReviewStatus status);
}