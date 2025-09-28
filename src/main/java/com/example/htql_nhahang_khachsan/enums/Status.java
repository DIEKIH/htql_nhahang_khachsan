package com.example.htql_nhahang_khachsan.enums;

public enum Status {
//    ACTIVE, INACTIVE

    ACTIVE("Hoạt động"),
    INACTIVE("Không hoạt động");

    private final String displayName;

    Status(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

