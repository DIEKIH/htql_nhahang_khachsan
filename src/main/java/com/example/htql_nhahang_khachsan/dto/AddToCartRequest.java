
package com.example.htql_nhahang_khachsan.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// Add to Cart Request
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddToCartRequest {
    private Long menuItemId;
    private Integer quantity;
    private String notes;
    private Boolean isTakeaway;
}