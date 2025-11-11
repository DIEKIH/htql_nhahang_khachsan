package com.example.htql_nhahang_khachsan.repository;

import com.example.htql_nhahang_khachsan.entity.AttendanceEntity;
import com.example.htql_nhahang_khachsan.enums.AttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<AttendanceEntity, Long> {

    Optional<AttendanceEntity> findByShiftId(Long shiftId);

    List<AttendanceEntity> findByStaffIdAndCheckInBetween(Long staffId, LocalDateTime start, LocalDateTime end);

    @Query("SELECT a FROM AttendanceEntity a WHERE a.staff.id = :staffId " +
            "AND DATE(a.checkIn) BETWEEN :startDate AND :endDate " +
            "ORDER BY a.checkIn DESC")
    List<AttendanceEntity> findByStaffAndDateRange(
            @Param("staffId") Long staffId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT a FROM AttendanceEntity a WHERE a.shift.workWeek.branch.id = :branchId " +
            "AND DATE(a.checkIn) = :date " +
            "AND a.status = :status")
    List<AttendanceEntity> findByBranchAndDateAndStatus(
            @Param("branchId") Long branchId,
            @Param("date") LocalDate date,
            @Param("status") AttendanceStatus status
    );
}