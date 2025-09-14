package com.example.htql_nhahang_khachsan.dto;

import com.example.htql_nhahang_khachsan.enums.Status;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomTypeRequest {

    @NotBlank(message = "Tên loại phòng không được để trống")
    @Size(max = 100, message = "Tên loại phòng không quá 100 ký tự")
    private String name;

    @Size(max = 1000, message = "Mô tả không quá 1000 ký tự")
    private String description;

    @NotNull(message = "Giá phòng không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá phòng phải lớn hơn 0")
    private BigDecimal price;

    @NotNull(message = "Số lượng khách tối đa không được để trống")
    @Min(value = 1, message = "Số lượng khách tối đa ít nhất là 1")
    @Max(value = 10, message = "Số lượng khách tối đa không quá 10")
    private Integer maxOccupancy;

    @Size(max = 50, message = "Loại giường không quá 50 ký tự")
    private String bedType;

    @DecimalMin(value = "0.0", inclusive = false, message = "Diện tích phòng phải lớn hơn 0")
    private Double roomSize;

    private String amenities; // JSON string

    private String imageUrls; // JSON array

    @NotNull(message = "Trạng thái không được để trống")
    private Status status;
}