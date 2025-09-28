package com.example.htql_nhahang_khachsan.enums;

public enum PromotionApplicability {
//    ROOM, RESTAURANT, BOTH
    ROOM("Phòng"),
    RESTAURANT("Nhà hàng"),
    BOTH("Cả hai");

    private final String displayName;

    PromotionApplicability(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
