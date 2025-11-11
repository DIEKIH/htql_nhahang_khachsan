//package com.example.htql_nhahang_khachsan.service;
//
//import com.example.htql_nhahang_khachsan.dto.MenuItemResponse;
//import com.example.htql_nhahang_khachsan.dto.RoomTypeResponse;
//import com.example.htql_nhahang_khachsan.entity.MenuItemEntity;
//import com.example.htql_nhahang_khachsan.entity.RoomTypeEntity;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//import java.math.BigDecimal;
//import java.text.DecimalFormat;
//
//@Service
//@RequiredArgsConstructor
//public class ResponseMapper {
//
//    private final PromotionService promotionService;
//
//    public RoomTypeResponse toRoomTypeResponse(RoomTypeEntity entity) {
//        RoomTypeResponse response = RoomTypeResponse.from(entity);
//
//        // Thêm thông tin khuyến mãi nếu có
//        BigDecimal discountedPrice = promotionService.getDiscountedPriceForRoomType(entity.getId());
//        if (discountedPrice != null && discountedPrice.compareTo(entity.getPrice()) < 0) {
//            response.setOriginalPrice(entity.getPrice());
//            response.setCurrentPrice(discountedPrice);
//            response.setFormattedOriginalPrice(formatCurrency(entity.getPrice()));
//            response.setFormattedPrice(formatCurrency(discountedPrice));
//        } else {
//            response.setCurrentPrice(entity.getPrice());
//            response.setFormattedPrice(formatCurrency(entity.getPrice()));
//        }
//
//        return response;
//    }
//
//    public MenuItemResponse toMenuItemResponse(MenuItemEntity entity) {
//        MenuItemResponse response = MenuItemResponse.from(entity);
//
//        // Thêm thông tin khuyến mãi nếu có
//        BigDecimal discountedPrice = promotionService.getDiscountedPriceForMenuItem(entity.getId());
//        if (discountedPrice != null && discountedPrice.compareTo(entity.getPrice()) < 0) {
//            response.setOriginalPrice(entity.getPrice());
//            response.setCurrentPrice(discountedPrice);
//        } else {
//            response.setCurrentPrice(entity.getPrice());
//        }
//
//        return response;
//    }
//
//    private String formatCurrency(BigDecimal amount) {
//        if (amount == null) return "0₫";
//        DecimalFormat formatter = new DecimalFormat("#,###");
//        return formatter.format(amount) + "₫";
//    }
//}