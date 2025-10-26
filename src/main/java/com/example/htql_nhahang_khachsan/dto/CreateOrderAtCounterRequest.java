package com.example.htql_nhahang_khachsan.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderAtCounterRequest {
    private Long tableId;
    private String cashierName;
    private String staffName;
    private List<OrderItemRequest> items;
    private String notes;
}