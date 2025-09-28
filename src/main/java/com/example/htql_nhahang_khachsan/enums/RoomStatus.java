package com.example.htql_nhahang_khachsan.enums;

public enum RoomStatus {
//    AVAILABLE, OCCUPIED, MAINTENANCE, OUT_OF_ORDER, CLEANING

    AVAILABLE("Có thể thuê"),
    OCCUPIED("Đang thuê"),
    MAINTENANCE("Bảo trì"),
    OUT_OF_ORDER("Không hoạt động"),
    CLEANING("Đang dọn dẹp"),
    RESERVED("Đã đặt trước");

    private final String displayName;

    RoomStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
