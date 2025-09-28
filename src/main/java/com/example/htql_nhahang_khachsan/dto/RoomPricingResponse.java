package com.example.htql_nhahang_khachsan.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomPricingResponse {
    private Long roomTypeId;
    private BigDecimal basePrice;
    private BigDecimal serviceFee;
    private BigDecimal vat;
    private BigDecimal totalPrice;
    private Integer nights;
    private String checkInDate;
    private String checkOutDate;
    private Boolean hasPromotion;
    private String promotionName;
    private BigDecimal discountAmount;

    public String getFormattedBasePrice() {
        return formatCurrency(basePrice);
    }

    public String getFormattedServiceFee() {
        return formatCurrency(serviceFee);
    }

    public String getFormattedVat() {
        return formatCurrency(vat);
    }

    public String getFormattedTotalPrice() {
        return formatCurrency(totalPrice);
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0₫";
        return String.format("%,.0f₫", amount.doubleValue());
    }
}
