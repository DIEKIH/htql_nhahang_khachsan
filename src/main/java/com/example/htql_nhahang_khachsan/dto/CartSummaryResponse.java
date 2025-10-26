package com.example.htql_nhahang_khachsan.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartSummaryResponse {
    private Long cartId;
    private Long branchId;
    private String branchName;
    private List<CartItemResponse> items;
    private Integer totalItems;
    private BigDecimal subtotal;
    private BigDecimal serviceCharge; // 10%
    private BigDecimal vat; // 8%
    private BigDecimal totalAmount;
    private String formattedSubtotal;
    private String formattedServiceCharge;
    private String formattedVat;
    private String formattedTotalAmount;
    private LocalDateTime updatedAt;
}
