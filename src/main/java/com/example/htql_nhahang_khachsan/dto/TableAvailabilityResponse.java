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
public class TableAvailabilityResponse {
    private Long branchId;
    private String branchName;
    private LocalDateTime checkTime;
    private List<TableResponse> tables;
    private TableStatistics statistics;
}