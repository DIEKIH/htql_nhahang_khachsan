package com.example.htql_nhahang_khachsan.repository;

import com.example.htql_nhahang_khachsan.entity.CartItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface CartItemRepository extends JpaRepository<CartItemEntity, Long> {
    Optional<CartItemEntity> findByCartIdAndMenuItemId(Long cartId, Long menuItemId);

    List<CartItemEntity> findByCartId(Long cartId);

    void deleteByCartId(Long cartId);

    @Query("SELECT COUNT(ci) FROM CartItemEntity ci WHERE ci.cart.user.id = :userId")
    Integer countByUserId(Long userId);
}