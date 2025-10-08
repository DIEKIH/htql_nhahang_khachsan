package com.example.htql_nhahang_khachsan.repository;

import com.example.htql_nhahang_khachsan.entity.RoomEntity;
import com.example.htql_nhahang_khachsan.enums.RoomStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<RoomEntity, Long>, JpaSpecificationExecutor<RoomEntity> {

    List<RoomEntity> findByRoomTypeBranchIdOrderByFloorAscRoomNumberAsc(Long branchId);

    Optional<RoomEntity> findByIdAndRoomTypeBranchId(Long id, Long branchId);

    boolean existsByRoomNumberAndRoomTypeBranchId(String roomNumber, Long branchId);

    List<RoomEntity> findByRoomTypeIdOrderByFloorAscRoomNumberAsc(Long roomTypeId);

    List<RoomEntity> findByStatusAndRoomTypeBranchId(RoomStatus status, Long branchId);

    List<RoomEntity> findByFloorAndRoomTypeBranchId(Integer floor, Long branchId);

    @Query("SELECT DISTINCT r.floor FROM RoomEntity r WHERE r.roomType.branch.id = :branchId ORDER BY r.floor")
    List<Integer> findDistinctFloorsByBranchId(@Param("branchId") Long branchId);

    @Query("SELECT COUNT(r) FROM RoomEntity r WHERE r.roomType.branch.id = :branchId AND r.status = :status")
    long countByBranchIdAndStatus(@Param("branchId") Long branchId, @Param("status") RoomStatus status);

    List<RoomEntity> findByRoomTypeBranchId(Long branchId);



    List<RoomEntity> findByRoomTypeIdOrderByRoomNumberAsc(Long roomTypeId);


    @Query("SELECT r FROM RoomEntity r WHERE r.roomType.branch.id = :branchId AND r.status IN :statuses")
    List<RoomEntity> findByBranchIdAndStatusIn(@Param("branchId") Long branchId,
                                               @Param("statuses") List<RoomStatus> statuses);


    //chi tiết loại phòng
    /**
     * Tìm phòng theo loại phòng và trạng thái
     */
    List<RoomEntity> findByRoomTypeIdAndStatus(Long roomTypeId, RoomStatus status);

    /**
     * Đếm số phòng theo loại phòng và trạng thái
     */
    Integer countByRoomTypeIdAndStatus(Long roomTypeId, RoomStatus status);

    /**
     * Tìm phòng theo tầng
     */
    List<RoomEntity> findByFloorOrderByRoomNumberAsc(Integer floor);

    /**
     * Tìm phòng theo số phòng
     */
    List<RoomEntity> findByRoomNumberContainingIgnoreCase(String roomNumber);

    /**
     * Tìm các phòng trống trong khoảng thời gian
     * (Phòng không có booking nào trong khoảng thời gian này)
     */
//    @Query("SELECT r FROM RoomEntity r " +
//            "WHERE r.roomType.id = :roomTypeId " +
//            "AND r.status = 'AVAILABLE' " +
//            "AND r.id NOT IN (" +
//            "    SELECT DISTINCT b.room.id FROM RoomBookingEntity b " +
//            "    WHERE b.room.id IS NOT NULL " +
//            "    AND b.status NOT IN (com.example.htql_nhahang_khachsan.enums.BookingStatus.CANCELLED, " +
//            "                         com.example.htql_nhahang_khachsan.enums.BookingStatus.NO_SHOW) " +
//            "    AND (" +
//            "        (b.checkInDate <= :checkOutDate AND b.checkOutDate >= :checkInDate)" +
//            "    )" +
//            ")")
//    List<RoomEntity> findAvailableRoomsByTypeAndDateRange(
//            @Param("roomTypeId") Long roomTypeId,
//            @Param("checkInDate") LocalDate checkInDate,
//            @Param("checkOutDate") LocalDate checkOutDate);

    @Query("SELECT r FROM RoomEntity r " +
            "WHERE r.roomType.id = :roomTypeId " +
            "AND r.status = :availableStatus " +
            "AND r.id NOT IN (" +
            "    SELECT DISTINCT b.room.id FROM RoomBookingEntity b " +
            "    WHERE b.room.id IS NOT NULL " +
            "    AND b.status NOT IN :excludedStatuses " +
            "    AND (b.checkInDate <= :checkOutDate AND b.checkOutDate >= :checkInDate)" +
            ")")
    List<RoomEntity> findAvailableRoomsByTypeAndDateRange(
            @Param("roomTypeId") Long roomTypeId,
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate,
            @Param("availableStatus") com.example.htql_nhahang_khachsan.enums.RoomStatus availableStatus,
            @Param("excludedStatuses") List<com.example.htql_nhahang_khachsan.enums.BookingStatus> excludedStatuses);





//    /**
//     * Tìm tất cả phòng trống trong khoảng thời gian (không phân biệt loại phòng)
//     */
//    @Query("SELECT r FROM RoomEntity r " +
//            "WHERE r.status = 'AVAILABLE' " +
//            "AND r.id NOT IN (" +
//            "    SELECT DISTINCT b.room.id FROM RoomBookingEntity b " +
//            "    WHERE b.room.id IS NOT NULL " +
//            "    AND b.status NOT IN ('CANCELLED', 'REJECTED') " +
//            "    AND (" +
//            "        (b.checkInDate <= :checkOutDate AND b.checkOutDate >= :checkInDate)" +
//            "    )" +
//            ")")
//    List<RoomEntity> findAllAvailableRoomsInDateRange(
//            @Param("checkInDate") LocalDate checkInDate,
//            @Param("checkOutDate") LocalDate checkOutDate);
//
//    /**
//     * Đếm số phòng trống theo loại phòng trong khoảng thời gian
//     */
//    @Query("SELECT COUNT(r) FROM RoomEntity r " +
//            "WHERE r.roomType.id = :roomTypeId " +
//            "AND r.status = 'AVAILABLE' " +
//            "AND r.id NOT IN (" +
//            "    SELECT DISTINCT b.room.id FROM BookingEntity b " +
//            "    WHERE b.room.id IS NOT NULL " +
//            "    AND b.status NOT IN ('CANCELLED', 'REJECTED') " +
//            "    AND (" +
//            "        (b.checkInDate <= :checkOutDate AND b.checkOutDate >= :checkInDate)" +
//            "    )" +
//            ")")
//    Integer countAvailableRoomsByTypeAndDateRange(
//            @Param("roomTypeId") Long roomTypeId,
//            @Param("checkInDate") LocalDate checkInDate,
//            @Param("checkOutDate") LocalDate checkOutDate);

    /**
     * Tìm phòng theo chi nhánh
     */
    @Query("SELECT r FROM RoomEntity r WHERE r.roomType.branch.id = :branchId")
    List<RoomEntity> findByBranchId(@Param("branchId") Long branchId);

    /**
     * Đếm tổng số phòng theo chi nhánh
     */
    @Query("SELECT COUNT(r) FROM RoomEntity r WHERE r.roomType.branch.id = :branchId")
    Integer countByBranchId(@Param("branchId") Long branchId);

    /**
     * Đếm số phòng theo trạng thái trong chi nhánh
     */


    //room realtime
    // Thêm vào RoomRepository
    @Query("SELECT DISTINCT r FROM RoomEntity r " +
            "JOIN r.roomType rt " +
            "WHERE rt.branch.id = :branchId " +
            "AND (:roomStatus IS NULL OR r.status = :roomStatus) " +
            "AND (:floor IS NULL OR r.floor = :floor) " +
            "AND (:roomTypeId IS NULL OR rt.id = :roomTypeId) " +
            "ORDER BY r.floor ASC, r.roomNumber ASC")
    List<RoomEntity> findByBranchWithFilters(
            @Param("branchId") Long branchId,
            @Param("roomStatus") RoomStatus roomStatus,
            @Param("floor") Integer floor,
            @Param("roomTypeId") Long roomTypeId
    );

//    @Query("SELECT DISTINCT r.floor FROM RoomEntity r " +
//            "JOIN r.roomType rt " +
//            "WHERE rt.branch.id = :branchId " +
//            "ORDER BY r.floor ASC")
//    List<Integer> findDistinctFloorsByBranchId(@Param("branchId") Long branchId);



    //booking room realtime
    @Query("""
        SELECT COUNT(r) FROM RoomEntity r
        WHERE r.roomType.id = :roomTypeId
          AND r.id NOT IN (
              SELECT b.room.id FROM RoomBookingEntity b
              WHERE b.status <> 'CANCELLED'
                AND b.checkInDate < :checkOut
                AND b.checkOutDate > :checkIn
          )
    """)
    int countAvailableRoomsByTypeAndDateRange(
            @Param("roomTypeId") Long roomTypeId,
            @Param("checkIn") LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut
    );


}
