package com.example.htql_nhahang_khachsan.dto;

import com.example.htql_nhahang_khachsan.enums.Status;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuItemRequest {

    @NotBlank(message = "Tên món ăn không được để trống")
    @Size(max = 100, message = "Tên món ăn không được vượt quá 100 ký tự")
    private String name;

    @Size(max = 1000, message = "Mô tả không được vượt quá 1000 ký tự")
    private String description;

    @NotNull(message = "Giá không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá phải lớn hơn 0")
    @Digits(integer = 10, fraction = 2, message = "Giá không hợp lệ")
    private BigDecimal price;

    @NotNull(message = "Vui lòng chọn danh mục")
    private Long categoryId;

    @Min(value = 1, message = "Thời gian chuẩn bị phải ít nhất 1 phút")
    @Max(value = 999, message = "Thời gian chuẩn bị không được vượt quá 999 phút")
    private Integer preparationTime;

    @Builder.Default
    private Boolean isAvailable = true;

    @Size(max = 1000, message = "Nguyên liệu không được vượt quá 1000 ký tự")
    private String ingredients;

    @Size(max = 500, message = "Chất gây dị ứng không được vượt quá 500 ký tự")
    private String allergens;

    @Min(value = 0, message = "Calories không được âm")
    @Max(value = 9999, message = "Calories không được vượt quá 9999")
    private Integer calories;

    @NotNull(message = "Vui lòng chọn trạng thái")
    private Status status;
}
