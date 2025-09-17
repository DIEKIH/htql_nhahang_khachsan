package com.example.htql_nhahang_khachsan.dto;

import com.example.htql_nhahang_khachsan.entity.PromotionEntity;
import com.example.htql_nhahang_khachsan.enums.PromotionApplicability;
import com.example.htql_nhahang_khachsan.enums.PromotionScope;
import com.example.htql_nhahang_khachsan.enums.PromotionType;
import com.example.htql_nhahang_khachsan.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PromotionResponse {

    private Long id;
    private String name;
    private String description;
    private PromotionType type;
    private BigDecimal discountValue;
    private BigDecimal minAmount;
    private BigDecimal maxDiscount;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Integer usageLimit;
    private Integer usedCount;
    private PromotionScope scope;
    private Set<String> branchNames; // Tên các chi nhánh áp dụng
    private Set<Long> branchIds; // ID các chi nhánh áp dụng
    private PromotionApplicability applicability;
    private Status status;
    private LocalDateTime createdAt;
    private Long createdBy;

    public static PromotionResponse from(PromotionEntity entity) {
        PromotionResponse response = new PromotionResponse();
        response.setId(entity.getId());
        response.setName(entity.getName());
        response.setDescription(entity.getDescription());
        response.setType(entity.getType());
        response.setDiscountValue(entity.getDiscountValue());
        response.setMinAmount(entity.getMinAmount());
        response.setMaxDiscount(entity.getMaxDiscount());
        response.setStartDate(entity.getStartDate());
        response.setEndDate(entity.getEndDate());
        response.setUsageLimit(entity.getUsageLimit());
        response.setUsedCount(entity.getUsedCount());
        response.setScope(entity.getScope());

        if (entity.getBranches() != null && !entity.getBranches().isEmpty()) {
            response.setBranchNames(entity.getBranches().stream()
                    .map(branch -> branch.getName())
                    .collect(Collectors.toSet()));
            response.setBranchIds(entity.getBranches().stream()
                    .map(branch -> branch.getId())
                    .collect(Collectors.toSet()));
        }

        response.setApplicability(entity.getApplicability());
        response.setStatus(entity.getStatus());
        response.setCreatedAt(entity.getCreatedAt());
        response.setCreatedBy(entity.getCreatedBy());

        return response;
    }

    // Helper methods for display
    public String getTypeLabel() {
        return switch (type) {
            case PERCENTAGE -> "Phần trăm (%)";
            case FIXED_AMOUNT -> "Số tiền cố định";
            case BOGO -> "Mua 1 tặng 1";
            default -> "Không xác định";
        };
    }


    public String getScopeLabel() {
        return switch (scope) {
            case SYSTEM_WIDE -> "Toàn hệ thống";
            case BRANCH_SPECIFIC -> "Chi nhánh cụ thể";
        };
    }

    public String getApplicabilityLabel() {
        return switch (applicability) {
            case ROOM -> "Khách sạn";
            case RESTAURANT -> "Nhà hàng";
            case BOTH -> "Cả hai";
        };
    }

    public String getStatusLabel() {
        return switch (status) {
            case ACTIVE -> "Hoạt động";
            case INACTIVE -> "Không hoạt động";
        };
    }

    public boolean isActive() {
        LocalDateTime now = LocalDateTime.now();
        return status == Status.ACTIVE &&
                now.isAfter(startDate) &&
                now.isBefore(endDate) &&
                (usageLimit == null || usedCount < usageLimit);
    }
}