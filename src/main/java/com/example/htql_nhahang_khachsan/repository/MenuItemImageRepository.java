package com.example.htql_nhahang_khachsan.repository;

import com.example.htql_nhahang_khachsan.entity.MenuItemEntity;
import com.example.htql_nhahang_khachsan.entity.MenuItemImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MenuItemImageRepository extends JpaRepository<MenuItemImageEntity, Long> {

    // Find by menu item
    List<MenuItemImageEntity> findByMenuItem(MenuItemEntity menuItem);

    // Find by menu item ordered by upload date
    List<MenuItemImageEntity> findByMenuItemOrderByUploadedAt(MenuItemEntity menuItem);

    // Find by ids and menu item
    List<MenuItemImageEntity> findByIdInAndMenuItem(List<Long> ids, MenuItemEntity menuItem);

    // Find by id and menu item
    Optional<MenuItemImageEntity> findByIdAndMenuItem(Long id, MenuItemEntity menuItem);

    // Delete by menu item
    void deleteByMenuItem(MenuItemEntity menuItem);

    // Check if primary image exists for menu item
    boolean existsByMenuItemAndIsPrimary(MenuItemEntity menuItem, Boolean isPrimary);

    // Update primary status by menu item
    @Modifying
    @Query("UPDATE MenuItemImageEntity m SET m.isPrimary = :isPrimary WHERE m.menuItem = :menuItem")
    void updatePrimaryStatusByMenuItem(@Param("menuItem") MenuItemEntity menuItem, @Param("isPrimary") Boolean isPrimary);
}