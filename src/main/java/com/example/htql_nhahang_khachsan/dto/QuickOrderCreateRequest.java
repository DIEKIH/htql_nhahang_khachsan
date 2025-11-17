package com.example.htql_nhahang_khachsan.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public  class QuickOrderCreateRequest {
    private Long menuItemId;
    private Long branchId;
    private Integer quantity;
}
