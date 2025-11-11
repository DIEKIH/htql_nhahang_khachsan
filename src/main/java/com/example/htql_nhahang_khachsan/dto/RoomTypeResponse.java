package com.example.htql_nhahang_khachsan.dto;

import com.example.htql_nhahang_khachsan.entity.RoomImageEntity;
import com.example.htql_nhahang_khachsan.entity.RoomTypeEntity;
import lombok.Builder;
import com.example.htql_nhahang_khachsan.enums.Status;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomTypeResponse {
    private Long id;
    private Long branchId;
    private String branchName;
    private String name;
    private String description;
    private BigDecimal price;
    private BigDecimal currentPrice; // Giá sau khi áp dụng khuyến mãi
    private BigDecimal originalPrice; // Giá gốc (nếu có khuyến mãi)
    private Integer maxOccupancy;
    private String bedType;
    private Double roomSize;
    private String viewType;
    private Boolean isAvailable;
    private String amenities;
    private String imageUrls;
    private Status status;
    private LocalDateTime createdAt;
    private String formattedPrice;
    private List<String> amenitiesList;
    private List<String> imageUrlsList;

    // New field for room images with metadata
    private List<RoomImageEntity> roomImages;
    private List<RoomImageResponse> roomImageResponses;

    // Helper method to get primary image
    public String getPrimaryImageUrl() {
        if (roomImages != null && !roomImages.isEmpty()) {
            return roomImages.stream()
                    .filter(RoomImageEntity::getIsPrimary)
                    .findFirst()
                    .map(RoomImageEntity::getImageUrl)
                    .orElse(roomImages.get(0).getImageUrl());
        }
        return null;
    }

    // Helper method to check if room has images
    public boolean hasImages() {
        return roomImages != null && !roomImages.isEmpty();
    }


    // Số lượng phòng có sẵn
    private Integer availableRooms;

    // Thông tin khuyến mãi
    private String promotionName;
    private BigDecimal discountAmount;
    private String discountType; // PERCENTAGE, FIXED_AMOUNT

    public String getFormattedPrice() {
        BigDecimal price = currentPrice != null ? currentPrice : this.price;
        return formatCurrency(price);
    }

    public String getFormattedOriginalPrice() {
        return formatCurrency(originalPrice);
    }

    public String getFormattedBasePrice() {
        return formatCurrency(price);
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0₫";
        return String.format("%,.0f₫", amount.doubleValue());
    }



    public boolean hasPromotion() {
        return originalPrice != null && originalPrice.compareTo(currentPrice) > 0;
    }

    public String getDiscountPercent() {
        if (!hasPromotion()) return "0%";

        BigDecimal discount = originalPrice.subtract(currentPrice);
        BigDecimal percent = discount.multiply(BigDecimal.valueOf(100)).divide(originalPrice, 0, BigDecimal.ROUND_HALF_UP);
        return percent + "%";
    }

    public static RoomTypeResponse from(RoomTypeEntity entity) {
        List<RoomImageEntity> images = entity.getRoomImages();

        // Ảnh chính
        String primaryImageUrl = null;
        if (images != null && !images.isEmpty()) {
            primaryImageUrl = images.stream()
                    .filter(RoomImageEntity::getIsPrimary)
                    .map(RoomImageEntity::getImageUrl)
                    .findFirst()
                    .orElse(images.get(0).getImageUrl());
        }

        return RoomTypeResponse.builder()
                .id(entity.getId())
                .branchId(entity.getBranch().getId())
                .branchName(entity.getBranch().getName())
                .name(entity.getName())
                .description(entity.getDescription())
                .maxOccupancy(entity.getMaxOccupancy())
                .bedType(entity.getBedType())
                .roomSize(entity.getRoomSize())
                .price(entity.getPrice())
                .status(entity.getStatus())
                .roomImages(images)
                .imageUrlsList(images != null ? images.stream()
                        .map(RoomImageEntity::getImageUrl)
                        .toList() : List.of())
                .createdAt(entity.getCreatedAt())
                .build();
    }

}