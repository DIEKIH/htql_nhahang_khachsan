package com.example.htql_nhahang_khachsan.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemResponse {
    private Long id;
    private Long menuItemId;
    private String menuItemName;
    private String menuItemImage;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal originalPrice;
    private BigDecimal subtotal;
    private String notes;
    private Boolean isTakeaway;
    private Boolean isAvailable;
    private Integer preparationTime;
    private String formattedUnitPrice;
    private String formattedOriginalPrice;
    private String formattedSubtotal;

    public boolean hasDiscount() {
        return originalPrice != null &&
                originalPrice.compareTo(unitPrice) > 0;
    }
}
