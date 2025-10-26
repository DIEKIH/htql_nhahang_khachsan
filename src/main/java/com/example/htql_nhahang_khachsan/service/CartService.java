package com.example.htql_nhahang_khachsan.service;

import com.example.htql_nhahang_khachsan.dto.*;
import com.example.htql_nhahang_khachsan.entity.*;
import com.example.htql_nhahang_khachsan.enums.PromotionApplicability;
import com.example.htql_nhahang_khachsan.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final MenuItemRepository menuItemRepository;
    private final UserRepository userRepository;
    private final PromotionService promotionService;

    private static final BigDecimal SERVICE_RATE = new BigDecimal("0.10"); // 10%
    private static final BigDecimal VAT_RATE = new BigDecimal("0.08"); // 8%

    @Transactional
    public void addToCart(Long userId, AddToCartRequest request) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        MenuItemEntity menuItem = menuItemRepository.findById(request.getMenuItemId())
                .orElseThrow(() -> new RuntimeException("Menu item not found"));

        if (!menuItem.getIsAvailable()) {
            throw new RuntimeException("Món ăn hiện không có sẵn");
        }

        Long branchId = menuItem.getCategory().getBranch().getId();

        // Tìm hoặc tạo giỏ hàng
        CartEntity cart = cartRepository.findByUserIdAndBranchId(userId, branchId)
                .orElseGet(() -> {
                    CartEntity newCart = CartEntity.builder()
                            .user(user)
                            .branch(menuItem.getCategory().getBranch())
                            .build();
                    return cartRepository.save(newCart);
                });

        // Tính giá sau giảm
        BigDecimal originalPrice = menuItem.getPrice();
        BigDecimal discountedPrice = promotionService.calculateDiscountedPrice(
                originalPrice, branchId, PromotionApplicability.RESTAURANT);

        // Kiểm tra món đã có trong giỏ chưa
        Optional<CartItemEntity> existingItem = cartItemRepository
                .findByCartIdAndMenuItemId(cart.getId(), menuItem.getId());

        if (existingItem.isPresent()) {
            // Cập nhật số lượng
            CartItemEntity item = existingItem.get();
            item.setQuantity(item.getQuantity() + request.getQuantity());
            item.setNotes(request.getNotes());
            item.setIsTakeaway(request.getIsTakeaway());
            cartItemRepository.save(item);
        } else {
            // Thêm mới
            CartItemEntity newItem = CartItemEntity.builder()
                    .cart(cart)
                    .menuItem(menuItem)
                    .quantity(request.getQuantity())
                    .unitPrice(discountedPrice)
                    .originalPrice(originalPrice.compareTo(discountedPrice) > 0 ? originalPrice : null)
                    .notes(request.getNotes())
                    .isTakeaway(request.getIsTakeaway())
                    .build();
            cartItemRepository.save(newItem);
        }
    }

    public CartSummaryResponse getCartSummary(Long userId) {
        CartEntity cart = cartRepository.findByUserIdWithItems(userId)
                .orElse(null);

        if (cart == null || cart.getItems().isEmpty()) {
            return CartSummaryResponse.builder()
                    .items(List.of())
                    .totalItems(0)
                    .subtotal(BigDecimal.ZERO)
                    .serviceCharge(BigDecimal.ZERO)
                    .vat(BigDecimal.ZERO)
                    .totalAmount(BigDecimal.ZERO)
                    .build();
        }

        List<CartItemResponse> itemResponses = cart.getItems().stream()
                .map(this::toCartItemResponse)
                .collect(Collectors.toList());

        BigDecimal subtotal = cart.getItems().stream()
                .map(CartItemEntity::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal serviceCharge = subtotal.multiply(SERVICE_RATE)
                .setScale(0, RoundingMode.HALF_UP);
        BigDecimal vat = subtotal.add(serviceCharge).multiply(VAT_RATE)
                .setScale(0, RoundingMode.HALF_UP);
        BigDecimal totalAmount = subtotal.add(serviceCharge).add(vat);

        return CartSummaryResponse.builder()
                .cartId(cart.getId())
                .branchId(cart.getBranch().getId())
                .branchName(cart.getBranch().getName())
                .items(itemResponses)
                .totalItems(cart.getItems().size())
                .subtotal(subtotal)
                .serviceCharge(serviceCharge)
                .vat(vat)
                .totalAmount(totalAmount)
                .formattedSubtotal(formatPrice(subtotal))
                .formattedServiceCharge(formatPrice(serviceCharge))
                .formattedVat(formatPrice(vat))
                .formattedTotalAmount(formatPrice(totalAmount))
                .updatedAt(cart.getUpdatedAt())
                .build();
    }

    @Transactional
    public void updateCartItem(Long userId, UpdateCartItemRequest request) {
        CartItemEntity item = cartItemRepository.findById(request.getCartItemId())
                .orElseThrow(() -> new RuntimeException("Cart item not found"));

        if (!item.getCart().getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }

        if (request.getQuantity() <= 0) {
            cartItemRepository.delete(item);
        } else {
            item.setQuantity(request.getQuantity());
            if (request.getNotes() != null) {
                item.setNotes(request.getNotes());
            }
            if (request.getIsTakeaway() != null) {
                item.setIsTakeaway(request.getIsTakeaway());
            }
            cartItemRepository.save(item);
        }
    }

    @Transactional
    public void removeCartItem(Long userId, Long cartItemId) {
        CartItemEntity item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));

        if (!item.getCart().getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }

        cartItemRepository.delete(item);
    }

    @Transactional
    public void clearCart(Long userId) {
        CartEntity cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        cartItemRepository.deleteByCartId(cart.getId());
    }

    public Integer getCartItemCount(Long userId) {
        return cartItemRepository.countByUserId(userId);
    }

    private CartItemResponse toCartItemResponse(CartItemEntity item) {
        MenuItemEntity menuItem = item.getMenuItem();

        return CartItemResponse.builder()
                .id(item.getId())
                .menuItemId(menuItem.getId())
                .menuItemName(menuItem.getName())
                .menuItemImage(menuItem.getImageUrl())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .originalPrice(item.getOriginalPrice())
                .subtotal(item.getSubtotal())
                .notes(item.getNotes())
                .isTakeaway(item.getIsTakeaway())
                .isAvailable(menuItem.getIsAvailable())
                .preparationTime(menuItem.getPreparationTime())
                .formattedUnitPrice(formatPrice(item.getUnitPrice()))
                .formattedOriginalPrice(item.getOriginalPrice() != null ?
                        formatPrice(item.getOriginalPrice()) : null)
                .formattedSubtotal(formatPrice(item.getSubtotal()))
                .build();
    }

    private String formatPrice(BigDecimal price) {
        DecimalFormat formatter = new DecimalFormat("#,###");
        return formatter.format(price) + " VNĐ";
    }
}