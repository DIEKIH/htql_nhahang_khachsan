package com.example.htql_nhahang_khachsan.enums;

public enum BranchType {
//    HOTEL, RESTAURANT, BOTH
    HOTEL("Khách sạn"),
    RESTAURANT("Nhà hàng"),
    BOTH("Khách sạn & Nhà hàng");

    private final String displayName;

    BranchType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
