package com.example.htql_nhahang_khachsan.dto;

import com.example.htql_nhahang_khachsan.entity.RoomImageEntity;
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
    private Integer maxOccupancy;
    private String bedType;
    private Double roomSize;
    private String amenities;
    private String imageUrls;
    private Status status;
    private LocalDateTime createdAt;
    private String formattedPrice;
    private List<String> amenitiesList;
    private List<String> imageUrlsList;

    // New field for room images with metadata
    private List<RoomImageEntity> roomImages;

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
}