package com.example.htql_nhahang_khachsan.dto;

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
}