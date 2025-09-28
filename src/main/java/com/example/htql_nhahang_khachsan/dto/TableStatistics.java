package com.example.htql_nhahang_khachsan.dto;

import com.example.htql_nhahang_khachsan.enums.RoomStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableStatistics {
    private long totalTables;
    private long availableTables;
    private long occupiedTables;
    private long reservedTables;
    private long outOfServiceTables;
}