package com.example.htql_nhahang_khachsan.enums;

public enum ShiftType {
    MORNING("Ca sáng", "bg-info", "06:00", "14:00"),
    AFTERNOON("Ca chiều", "bg-warning", "14:00", "22:00"),
    EVENING("Ca tối", "bg-primary", "18:00", "02:00"),
    NIGHT("Ca đêm", "bg-dark", "22:00", "06:00"),
    FULL_DAY("Ca full", "bg-success", "08:00", "20:00"),
    OFF("Nghỉ", "bg-secondary", null, null);

    private final String displayName;
    private final String badgeClass;
    private final String defaultStart;
    private final String defaultEnd;

    ShiftType(String displayName, String badgeClass, String defaultStart, String defaultEnd) {
        this.displayName = displayName;
        this.badgeClass = badgeClass;
        this.defaultStart = defaultStart;
        this.defaultEnd = defaultEnd;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getBadgeClass() {
        return badgeClass;
    }

    public String getDefaultStart() {
        return defaultStart;
    }

    public String getDefaultEnd() {
        return defaultEnd;
    }
}