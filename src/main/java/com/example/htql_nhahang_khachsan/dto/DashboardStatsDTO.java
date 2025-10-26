package com.example.htql_nhahang_khachsan.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDTO {
    private Integer checkInToday;
    private Integer checkOutToday;
    private Integer occupied;
    private Integer available;
}
