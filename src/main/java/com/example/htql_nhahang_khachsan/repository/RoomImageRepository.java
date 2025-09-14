package com.example.htql_nhahang_khachsan.repository;

import com.example.htql_nhahang_khachsan.entity.RoomImageEntity;
import com.example.htql_nhahang_khachsan.entity.RoomTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomImageRepository extends JpaRepository<RoomImageEntity, Long> {

    List<RoomImageEntity> findByRoomTypeOrderByDisplayOrder(RoomTypeEntity roomType);

    List<RoomImageEntity> findByRoomType(RoomTypeEntity roomType);

    List<RoomImageEntity> findByIdInAndRoomType(List<Long> ids, RoomTypeEntity roomType);

    Optional<RoomImageEntity> findByIdAndRoomType(Long id, RoomTypeEntity roomType);

    @Query("SELECT MAX(ri.displayOrder) FROM RoomImageEntity ri WHERE ri.roomType = :roomType")
    Integer findMaxDisplayOrderByRoomType(@Param("roomType") RoomTypeEntity roomType);

    boolean existsByRoomTypeAndIsPrimary(RoomTypeEntity roomType, Boolean isPrimary);

    @Modifying
    @Query("UPDATE RoomImageEntity ri SET ri.isPrimary = :isPrimary WHERE ri.roomType = :roomType")
    void updatePrimaryStatusByRoomType(@Param("roomType") RoomTypeEntity roomType, @Param("isPrimary") Boolean isPrimary);

    void deleteByRoomType(RoomTypeEntity roomType);

    @Query("SELECT ri FROM RoomImageEntity ri WHERE ri.roomType = :roomType AND ri.isPrimary = true")
    Optional<RoomImageEntity> findPrimaryImageByRoomType(@Param("roomType") RoomTypeEntity roomType);
}