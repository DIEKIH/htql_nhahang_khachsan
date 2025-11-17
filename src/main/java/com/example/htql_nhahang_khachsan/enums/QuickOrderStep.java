package com.example.htql_nhahang_khachsan.enums;

public enum QuickOrderStep {
    ITEM_SELECTED,      // Đã chọn món
    INFO_COLLECTING,    // Đang thu thập thông tin
    INFO_COLLECTED,     // Đã có đủ thông tin
    READY_TO_PAY  ,     // Sẵn sàng thanh toán

     // Đã đủ info
    ORDER_CREATED,       // Đã tạo order
    PAYMENT_PENDING,     // Chờ thanh toán
    COMPLETED
}

