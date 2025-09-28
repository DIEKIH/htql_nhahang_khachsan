package com.example.htql_nhahang_khachsan.enums;

public enum PromotionScope {
//    SYSTEM_WIDE, BRANCH_SPECIFIC

    SYSTEM_WIDE("Toàn hệ thống"),
    BRANCH_SPECIFIC("Chi nhánh cụ thể");

    private final String displayName;

    PromotionScope(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
