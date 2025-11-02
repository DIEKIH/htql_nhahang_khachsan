package com.example.htql_nhahang_khachsan.repository;

import com.example.htql_nhahang_khachsan.entity.BranchViewHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BranchViewHistoryRepository extends JpaRepository<BranchViewHistoryEntity, Long> {

    // Tìm lịch sử xem theo user và branch
    Optional<BranchViewHistoryEntity> findByUserIdAndBranchId(Long userId, Long branchId);

    // Tìm lịch sử xem theo session và branch (cho người chưa đăng nhập)
    Optional<BranchViewHistoryEntity> findBySessionIdAndBranchId(String sessionId, Long branchId);

    // Lấy danh sách chi nhánh đã xem của user (sắp xếp theo thời gian xem gần nhất)
    @Query("SELECT h FROM BranchViewHistoryEntity h WHERE h.user.id = :userId " +
            "ORDER BY h.lastViewedAt DESC")
    List<BranchViewHistoryEntity> findByUserIdOrderByLastViewedAtDesc(@Param("userId") Long userId);

    // Lấy danh sách chi nhánh đã xem theo session
    @Query("SELECT h FROM BranchViewHistoryEntity h WHERE h.sessionId = :sessionId " +
            "ORDER BY h.lastViewedAt DESC")
    List<BranchViewHistoryEntity> findBySessionIdOrderByLastViewedAtDesc(@Param("sessionId") String sessionId);

    // Lấy N chi nhánh xem gần nhất của user
    @Query("SELECT h FROM BranchViewHistoryEntity h WHERE h.user.id = :userId " +
            "ORDER BY h.lastViewedAt DESC")
    List<BranchViewHistoryEntity> findTopNByUserId(@Param("userId") Long userId);

    // Lấy N chi nhánh xem gần nhất theo session
    @Query("SELECT h FROM BranchViewHistoryEntity h WHERE h.sessionId = :sessionId " +
            "ORDER BY h.lastViewedAt DESC")
    List<BranchViewHistoryEntity> findTopNBySessionId(@Param("sessionId") String sessionId);

    // Xóa lịch sử xem cũ (quá 30 ngày)
    @Query("DELETE FROM BranchViewHistoryEntity h WHERE h.lastViewedAt < :cutoffDate " +
            "AND h.user IS NULL")
    void deleteOldSessionHistory(@Param("cutoffDate") LocalDateTime cutoffDate);

    // Đếm số lượt xem của một chi nhánh
    @Query("SELECT COALESCE(SUM(h.viewCount), 0) FROM BranchViewHistoryEntity h " +
            "WHERE h.branch.id = :branchId")
    Long countTotalViewsByBranchId(@Param("branchId") Long branchId);

    // Chuyển lịch sử từ session sang user khi đăng nhập
    @Query("SELECT h FROM BranchViewHistoryEntity h WHERE h.sessionId = :sessionId")
    List<BranchViewHistoryEntity> findBySessionId(@Param("sessionId") String sessionId);

    // Tìm lịch sử xem theo user

    // Lấy top N chi nhánh được xem nhiều nhất
    @Query("SELECT vh FROM BranchViewHistoryEntity vh " +
            "ORDER BY vh.viewCount DESC, vh.lastViewedAt DESC")
    List<BranchViewHistoryEntity> findTopByOrderByViewCountDesc();
}