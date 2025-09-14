package com.example.htql_nhahang_khachsan.enums;

public enum BranchStatus {
//    ACTIVE, INACTIVE, MAINTENANCE
    ACTIVE("Hoạt động"),
    INACTIVE("Không hoạt động"),
    MAINTENANCE("Bảo trì");

    private final String displayName;

    BranchStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }



}
