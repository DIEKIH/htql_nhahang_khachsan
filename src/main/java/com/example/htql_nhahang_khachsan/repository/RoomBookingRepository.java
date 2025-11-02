package com.example.htql_nhahang_khachsan.repository;

import com.example.htql_nhahang_khachsan.entity.RoomBookingEntity;
import com.example.htql_nhahang_khachsan.enums.BookingStatus;
import com.example.htql_nhahang_khachsan.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoomBookingRepository extends JpaRepository<RoomBookingEntity, Long> {
    Optional<RoomBookingEntity> findByBookingCode(String bookingCode);


    List<RoomBookingEntity> findByCustomerId(Long customerId);

    List<RoomBookingEntity> findByGuestEmail(String guestEmail);

    List<RoomBookingEntity> findByStatus(BookingStatus status);

    List<RoomBookingEntity> findByBranchIdAndCheckInDateBetween(
            Long branchId,
            LocalDate startDate,
            LocalDate endDate
    );

    // Kiểm tra booking overlap
    @Query("SELECT COUNT(b) FROM RoomBookingEntity b " +
            "WHERE b.roomType.id = :roomTypeId " +
            "AND b.status NOT IN (:excludedStatuses) " +
            "AND (b.checkInDate <= :checkOutDate AND b.checkOutDate >= :checkInDate)")
    long countOverlappingBookings(
            @Param("roomTypeId") Long roomTypeId,
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate,
            @Param("excludedStatuses") List<BookingStatus> excludedStatuses
    );


    // Thống kê booking theo trạng thái
    @Query("SELECT b.status, COUNT(b) FROM RoomBookingEntity b " +
            "WHERE b.branch.id = :branchId " +
            "AND b.checkInDate BETWEEN :startDate AND :endDate " +
            "GROUP BY b.status")
    List<Object[]> getBookingStatsByStatus(
            @Param("branchId") Long branchId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // Tổng doanh thu từ booking
    @Query("SELECT SUM(b.totalAmount) FROM RoomBookingEntity b " +
            "WHERE b.branch.id = :branchId " +
            "AND b.status = 'CONFIRMED' " +
            "AND b.checkInDate BETWEEN :startDate AND :endDate")
    BigDecimal getTotalRevenue(
            @Param("branchId") Long branchId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );


    // Kiểm tra xem phòng có booking trong khoảng thời gian không
    @Query("SELECT COUNT(b) > 0 FROM RoomBookingEntity b " +
            "WHERE b.room.id = :roomId " +
            "AND b.status NOT IN (:excludedStatuses) " +
            "AND ((b.checkInDate <= :checkOut AND b.checkOutDate >= :checkIn))")
    boolean existsBookingForRoomInDateRange(
            @Param("roomId") Long roomId,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut,
            @Param("excludedStatuses") List<BookingStatus> excludedStatuses
    );


    List<RoomBookingEntity> findByCheckOutDateAndStatus(
            LocalDate checkOutDate, BookingStatus status);


    List<RoomBookingEntity> findByBranchIdAndBookingDateBetweenAndPaymentStatus(
            Long branchId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            PaymentStatus paymentStatus
    );

    // Tìm bookings theo chi nhánh và khoảng thời gian
    List<RoomBookingEntity> findByBranchIdAndCreatedAtBetweenAndPaymentStatus(
            Long branchId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            PaymentStatus paymentStatus
    );

    // Tìm tất cả bookings trong khoảng thời gian (tất cả chi nhánh)
    List<RoomBookingEntity> findByCreatedAtBetweenAndPaymentStatus(
            LocalDateTime startDate,
            LocalDateTime endDate,
            PaymentStatus paymentStatus
    );

}


