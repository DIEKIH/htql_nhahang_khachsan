package com.example.htql_nhahang_khachsan.dto;

import com.example.htql_nhahang_khachsan.enums.TableStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantTableDTO {
    private Long id;
    private String tableNumber;
    private Integer capacity;
    private TableStatus status;
}