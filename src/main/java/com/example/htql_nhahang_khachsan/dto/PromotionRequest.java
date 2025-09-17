package com.example.htql_nhahang_khachsan.dto;

import com.example.htql_nhahang_khachsan.enums.PromotionApplicability;
import com.example.htql_nhahang_khachsan.enums.PromotionScope;
import com.example.htql_nhahang_khachsan.enums.PromotionType;
import com.example.htql_nhahang_khachsan.enums.Status;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PromotionRequest {

    @NotBlank(message = "Tên khuyến mãi không được để trống")
    @Size(min = 3, max = 100, message = "Tên khuyến mãi phải từ 3-100 ký tự")
    private String name;

    @Size(max = 500, message = "Mô tả không được quá 500 ký tự")
    private String description;

    @NotNull(message = "Loại khuyến mãi không được để trống")
    private PromotionType type;

    @NotNull(message = "Giá trị giảm giá không được để trống")
    @DecimalMin(value = "0.01", message = "Giá trị giảm giá phải lớn hơn 0")
    private BigDecimal discountValue;

    @DecimalMin(value = "0", message = "Số tiền tối thiểu không được âm")
    private BigDecimal minAmount;

    @DecimalMin(value = "0", message = "Giảm tối đa không được âm")
    private BigDecimal maxDiscount;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDateTime startDate;

    @NotNull(message = "Ngày kết thúc không được để trống")
    private LocalDateTime endDate;

    @Min(value = 1, message = "Số lần sử dụng phải lớn hơn 0")
    private Integer usageLimit;

    @NotNull(message = "Phạm vi áp dụng không được để trống")
    private PromotionScope scope;

    private Set<Long> branchIds; // Chỉ dùng khi scope = BRANCH_SPECIFIC

    @NotNull(message = "Đối tượng áp dụng không được để trống")
    private PromotionApplicability applicability;

    @NotNull(message = "Trạng thái không được để trống")
    private Status status;
}