package com.example.htql_nhahang_khachsan.dto;

import com.example.htql_nhahang_khachsan.entity.MenuItemEntity;
import com.example.htql_nhahang_khachsan.entity.MenuItemImageEntity;
import com.example.htql_nhahang_khachsan.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuItemResponse {
    private Long id;
    private Long categoryId;
    private String categoryName;
    private String name;
    private String description;
    private BigDecimal price;
    private BigDecimal currentPrice;     // Giá sau giảm
    private BigDecimal originalPrice;    // Giá gốc khi có khuyến mãi
    private String formattedPrice;
    private String imageUrl;
    private Integer preparationTime;
    private Boolean isAvailable;
    private String ingredients;
    private String allergens;
    private Integer calories;
    private Status status;
    private LocalDateTime createdAt;

    // For multiple images
    private List<String> imageUrlsList;
    private List<MenuItemImageEntity> menuItemImages;

    // Helper
    public boolean hasPromotion() {
        return originalPrice != null
                && currentPrice != null
                && originalPrice.compareTo(currentPrice) > 0;
    }

    public static MenuItemResponse from(MenuItemEntity entity) {
        List<MenuItemImageEntity> images = entity.getMenuItemImages();

        // Ảnh chính (ưu tiên isPrimary = true, nếu không thì lấy ảnh đầu tiên)
        String mainImageUrl = null;
        if (images != null && !images.isEmpty()) {
            mainImageUrl = images.stream()
                    .filter(MenuItemImageEntity::getIsPrimary)
                    .map(MenuItemImageEntity::getImageUrl)
                    .findFirst()
                    .orElse(images.get(0).getImageUrl());
        }

        // Lấy danh sách tất cả ảnh
        List<String> imageUrls = images != null
                ? images.stream().map(MenuItemImageEntity::getImageUrl).toList()
                : List.of();

        return MenuItemResponse.builder()
                .id(entity.getId())
                .categoryId(entity.getCategory().getId())
                .categoryName(entity.getCategory().getName())
                .name(entity.getName())
                .description(entity.getDescription())
                .price(entity.getPrice())
                .currentPrice(entity.getPrice())       // nếu chưa có khuyến mãi
                .originalPrice(entity.getPrice())       // tạm gán bằng nhau
                .formattedPrice(entity.getPrice().toString())
                .imageUrl(mainImageUrl)
                .imageUrlsList(imageUrls)
                .menuItemImages(images)
                .preparationTime(entity.getPreparationTime())
                .isAvailable(entity.getIsAvailable())
                .ingredients(entity.getIngredients())
                .allergens(entity.getAllergens())
                .calories(entity.getCalories())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }



}