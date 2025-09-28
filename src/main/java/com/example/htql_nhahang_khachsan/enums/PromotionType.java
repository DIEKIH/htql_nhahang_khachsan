package com.example.htql_nhahang_khachsan.enums;

public enum PromotionType {
//    PERCENTAGE, FIXED_AMOUNT, BOGO, FREE_UPGRADE

    PERCENTAGE("Phần trăm"),
    FIXED_AMOUNT("Số tiền cố định"),
    BOGO("Mua 1 tặng 1");

    private final String displayName;

    PromotionType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
