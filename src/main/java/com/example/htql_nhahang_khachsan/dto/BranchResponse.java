package com.example.htql_nhahang_khachsan.dto;

import com.example.htql_nhahang_khachsan.entity.BranchEntity;
import com.example.htql_nhahang_khachsan.enums.BranchStatus;
import com.example.htql_nhahang_khachsan.enums.BranchType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchResponse {
    private Long id;
    private String name;
    private String description;
    private String address;
    private String streetAddress;
    private String district;
    private String province;
    private String phoneNumber;
    private String email;
    private BranchType type;
    private BranchStatus status;
    private String imageUrl;
    private Double latitude;
    private Double longitude;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static BranchResponse from(BranchEntity entity) {
        return BranchResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .address(entity.getAddress())
                .streetAddress(entity.getStreetAddress())
                .district(entity.getDistrict())
                .province(entity.getProvince())
                .phoneNumber(entity.getPhoneNumber())
                .email(entity.getEmail())
                .type(entity.getType())
                .status(entity.getStatus())
                .imageUrl(entity.getImageUrl())
                .latitude(entity.getLatitude())
                .longitude(entity.getLongitude())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    // Thêm các method này vào BranchResponse.java hiện tại

    public String getTypeDisplayName() {
        switch (type) {
            case HOTEL:
                return "Khách sạn";
            case RESTAURANT:
                return "Nhà hàng";
            case BOTH:
                return "Khách sạn & Nhà hàng";
            default:
                return "Không xác định";
        }
    }

    public String getStatusDisplayName() {
        switch (status) {
            case ACTIVE:
                return "Đang mở cửa";
            case INACTIVE:
                return "Đóng cửa";
            case MAINTENANCE:
                return "Đang bảo trì";
            default:
                return "Không xác định";
        }
    }

    public String getFullAddress() {
        return streetAddress + ", " + district + ", " + province;
    }

    public String getShortDescription() {
        if (description == null) return "";
        return description.length() > 100 ? description.substring(0, 100) + "..." : description;
    }

    public boolean hasImage() {
        return imageUrl != null && !imageUrl.trim().isEmpty();
    }

    public String getDefaultImage() {
        if (hasImage()) {
            return imageUrl;
        }
        // Trả về ảnh mặc định dựa trên loại chi nhánh
        switch (type) {
            case HOTEL:
                return "https://images.unsplash.com/photo-1566073771259-6a8506099945?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80";
            case RESTAURANT:
                return "https://images.unsplash.com/photo-1551882547-ff40c63fe5fa?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80";
            case BOTH:
                return "https://images.unsplash.com/photo-1564501049412-61c2a3083791?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80";
            default:
                return "https://images.unsplash.com/photo-1571896349842-33c89424de2d?ixlib=rb-4.0.3&auto=format&fit=crop&w=800&q=80";
        }
    }

    public List<String> getServiceBadges() {
        List<String> badges = new ArrayList<>();
        switch (type) {
            case HOTEL:
                badges.add("<span class=\"badge bg-primary me-1\"><i class=\"fas fa-bed\"></i> Khách sạn</span>");
                badges.add("<span class=\"badge bg-info\"><i class=\"fas fa-concierge-bell\"></i> Dịch vụ</span>");
                break;
            case RESTAURANT:
                badges.add("<span class=\"badge bg-success me-1\"><i class=\"fas fa-utensils\"></i> Nhà hàng</span>");
                badges.add("<span class=\"badge bg-warning\"><i class=\"fas fa-coffee\"></i> Đồ uống</span>");
                break;
            case BOTH:
                badges.add("<span class=\"badge bg-primary me-1\"><i class=\"fas fa-bed\"></i> Khách sạn</span>");
                badges.add("<span class=\"badge bg-success me-1\"><i class=\"fas fa-utensils\"></i> Nhà hàng</span>");
                badges.add("<span class=\"badge bg-info\"><i class=\"fas fa-swimming-pool\"></i> Tiện ích</span>");
                break;
        }
        return badges;
    }

    public String getProvinceSlug() {
        if (province == null) return "";
        return province.toLowerCase()
                .replace("tp. ", "")
                .replace("thành phố ", "")
                .replace("tỉnh ", "")
                .replace(" ", "-")
                .replace("ồ", "o")
                .replace("ì", "i")
                .replace("ả", "a")
                .replace("ệ", "e")
                .replace("ị", "i");
    }
}