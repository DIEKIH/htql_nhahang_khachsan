package com.example.htql_nhahang_khachsan.repository;

import com.example.htql_nhahang_khachsan.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface CartRepository extends JpaRepository<CartEntity, Long> {
    Optional<CartEntity> findByUserId(Long userId);

    Optional<CartEntity> findByUserIdAndBranchId(Long userId, Long branchId);

    @Query("SELECT c FROM CartEntity c " +
            "LEFT JOIN FETCH c.items ci " +
            "LEFT JOIN FETCH ci.menuItem " +
            "WHERE c.user.id = :userId")
    Optional<CartEntity> findByUserIdWithItems(Long userId);
}

