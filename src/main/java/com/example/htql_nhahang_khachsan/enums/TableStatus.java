package com.example.htql_nhahang_khachsan.enums;

public enum TableStatus {
//    AVAILABLE, OCCUPIED, RESERVED, OUT_OF_SERVICE, CLEANING
    AVAILABLE("Có sẵn"),
    OCCUPIED("Đang sử dụng"),
    RESERVED("Đã đặt"),
    OUT_OF_SERVICE("Ngừng phục vụ"),

    CLEANING("Đang dọn dẹp");



    private final String displayName;

    TableStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
