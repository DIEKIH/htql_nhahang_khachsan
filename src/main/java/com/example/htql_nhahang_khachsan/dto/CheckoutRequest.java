package com.example.htql_nhahang_khachsan.dto;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckoutRequest {
    private String customerName;
    private String customerPhone;
    private String customerAddress;
    private String orderNotes;
    private String paymentMethod; // CASH_ON_DELIVERY, VNPAY, MOMO
    private List<Long> selectedItemIds; // Nếu chỉ thanh toán một số món
}
