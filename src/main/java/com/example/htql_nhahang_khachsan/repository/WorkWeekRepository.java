package com.example.htql_nhahang_khachsan.repository;

import com.example.htql_nhahang_khachsan.entity.WorkWeekEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface WorkWeekRepository extends JpaRepository<WorkWeekEntity, Long> {

    List<WorkWeekEntity> findByBranchIdOrderByWeekStartDesc(Long branchId);

    @Query("SELECT ww FROM WorkWeekEntity ww WHERE ww.branch.id = :branchId " +
            "AND ww.weekStart >= :startDate AND ww.weekEnd <= :endDate " +
            "ORDER BY ww.weekStart DESC, ww.staff.fullName ASC")
    List<WorkWeekEntity> findByBranchAndDateRange(
            @Param("branchId") Long branchId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT ww FROM WorkWeekEntity ww WHERE ww.staff.id = :staffId " +
            "AND ww.weekStart >= :startDate AND ww.weekEnd <= :endDate " +
            "ORDER BY ww.weekStart DESC")
    List<WorkWeekEntity> findByStaffAndDateRange(
            @Param("staffId") Long staffId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    Optional<WorkWeekEntity> findByStaffIdAndWeekStart(Long staffId, LocalDate weekStart);
}