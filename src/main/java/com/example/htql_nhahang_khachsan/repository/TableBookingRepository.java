package com.example.htql_nhahang_khachsan.repository;

import com.example.htql_nhahang_khachsan.entity.TableBookingEntity;
import com.example.htql_nhahang_khachsan.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TableBookingRepository extends JpaRepository<TableBookingEntity, Long> {

    List<TableBookingEntity> findByBranchIdOrderByCreatedAtDesc(Long branchId);

    Optional<TableBookingEntity> findByIdAndBranchId(Long id, Long branchId);

    List<TableBookingEntity> findByBranchIdAndStatus(Long branchId, BookingStatus status);

    @Query("SELECT tb FROM TableBookingEntity tb " +
            "WHERE tb.branch.id = :branchId " +
            "AND tb.bookingDate = :date " +
            "AND tb.bookingTime BETWEEN :startTime AND :endTime " +
            "AND tb.status IN :statuses")
    List<TableBookingEntity> findByBranchIdAndDateAndTimeRange(
            @Param("branchId") Long branchId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("statuses") List<BookingStatus> statuses
    );

    List<TableBookingEntity> findByBranchIdAndBookingDate(Long branchId, LocalDate bookingDate);

    List<TableBookingEntity> findByBranchIdAndBookingDateAndStatus(Long branchId, LocalDate bookingDate, BookingStatus status);


    List<TableBookingEntity> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    @Query("SELECT COUNT(tb) FROM TableBookingEntity tb WHERE tb.branch.id = :branchId " +
            "AND tb.bookingDate = :date AND tb.status = :status")
    Long countByBranchAndDateAndStatus(
            @Param("branchId") Long branchId,
            @Param("date") LocalDate date,
            @Param("status") BookingStatus status
    );
}