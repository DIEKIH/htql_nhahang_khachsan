package com.example.htql_nhahang_khachsan.dto;

import com.example.htql_nhahang_khachsan.enums.PromotionApplicability;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PriceCalculationRequest {
    private BigDecimal originalPrice;
    private Long branchId;
    private PromotionApplicability applicability;
    private Map<String, Object> additionalData;
}
