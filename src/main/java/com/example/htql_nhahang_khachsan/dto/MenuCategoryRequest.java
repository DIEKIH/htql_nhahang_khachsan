package com.example.htql_nhahang_khachsan.dto;

import com.example.htql_nhahang_khachsan.enums.Status;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MenuCategoryRequest {

    @NotBlank(message = "Tên loại menu không được để trống")
    @Size(max = 100, message = "Tên loại menu không được vượt quá 100 ký tự")
    private String name;

    @Size(max = 500, message = "Mô tả không được vượt quá 500 ký tự")
    private String description;

    @Min(value = 1, message = "Thứ tự hiển thị phải lớn hơn 0")
    @Max(value = 999, message = "Thứ tự hiển thị không được vượt quá 999")
    private Integer displayOrder;

    @NotNull(message = "Trạng thái không được để trống")
    private Status status; // ACTIVE, INACTIVE
}
