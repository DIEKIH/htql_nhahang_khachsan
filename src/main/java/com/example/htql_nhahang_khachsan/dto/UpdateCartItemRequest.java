package com.example.htql_nhahang_khachsan.dto;

import lombok.*;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCartItemRequest {
    private Long cartItemId;
    private Integer quantity;
    private String notes;
    private Boolean isTakeaway;
}