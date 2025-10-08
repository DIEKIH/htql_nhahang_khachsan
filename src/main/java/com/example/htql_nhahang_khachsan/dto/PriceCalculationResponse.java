package com.example.htql_nhahang_khachsan.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceCalculationResponse {

    private BigDecimal roomPrice; // Giá phòng/đêm
    private Integer numberOfNights;
    private Integer numberOfRooms;
    private BigDecimal totalRoomPrice; // roomPrice * nights * rooms

    private BigDecimal breakfastFee;
    private BigDecimal spaFee;
    private BigDecimal airportTransferFee;

    private BigDecimal subtotal; // Tổng trước thuế phí
    private BigDecimal serviceFee; // 10% của subtotal
    private BigDecimal vat; // 10% của (subtotal + serviceFee)

    private BigDecimal totalAmount;
    private BigDecimal depositAmount; // 50% của totalAmount
    private BigDecimal remainingAmount;

    private BigDecimal originalPrice;
    private BigDecimal discountAmount;
    private BigDecimal finalPrice;
    private String discountType;
    private String promotionName;
    private String discountDescription;
    private BigDecimal savedAmount;
    private String savedPercentage;
}
