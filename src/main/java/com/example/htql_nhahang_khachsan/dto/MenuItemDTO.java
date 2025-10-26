package com.example.htql_nhahang_khachsan.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MenuItemDTO {
    private Long id;
    private String name;
    private BigDecimal price;
    private String imageUrl;
    private Integer preparationTime;
    private Boolean isAvailable;

    // Thêm các trường cần thiết
    private Long categoryId;
    private String categoryName;
    private BigDecimal originalPrice;
    private BigDecimal discountPercent;
    private Boolean hasPromotion;

    // Constructor cũ để tương thích
    public MenuItemDTO(Long id, String name, BigDecimal price, String imageUrl,
                       Integer preparationTime, Boolean isAvailable) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.imageUrl = imageUrl;
        this.preparationTime = preparationTime;
        this.isAvailable = isAvailable;
        this.hasPromotion = false;
        this.discountPercent = BigDecimal.ZERO;
    }
}