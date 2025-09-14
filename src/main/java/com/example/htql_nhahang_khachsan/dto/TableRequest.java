package com.example.htql_nhahang_khachsan.dto;



import com.example.htql_nhahang_khachsan.enums.TableStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.*;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TableRequest {

    @NotBlank(message = "Số bàn không được để trống")
    private String tableNumber;

    @NotNull(message = "Sức chứa không được để trống")
    @Min(value = 1, message = "Sức chứa phải lớn hơn 0")
    @Max(value = 50, message = "Sức chứa không được vượt quá 50")
    private Integer capacity;

    @NotNull(message = "Trạng thái không được để trống")
    private TableStatus status;

    @Min(value = 0, message = "Vị trí X phải >= 0")
    @Max(value = 1000, message = "Vị trí X phải <= 1000")
    private Integer positionX;

    @Min(value = 0, message = "Vị trí Y phải >= 0")
    @Max(value = 1000, message = "Vị trí Y phải <= 1000")
    private Integer positionY;

    private String notes;
}