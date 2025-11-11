package com.example.htql_nhahang_khachsan.enums;

public enum AttendanceStatus {
    PRESENT("Có mặt", "bg-success"),
    LATE("Đi muộn", "bg-warning"),
    ABSENT("Vắng mặt", "bg-danger"),
    LEFT_EARLY("Về sớm", "bg-info");

    private final String displayName;
    private final String badgeClass;

    AttendanceStatus(String displayName, String badgeClass) {
        this.displayName = displayName;
        this.badgeClass = badgeClass;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getBadgeClass() {
        return badgeClass;
    }
}
