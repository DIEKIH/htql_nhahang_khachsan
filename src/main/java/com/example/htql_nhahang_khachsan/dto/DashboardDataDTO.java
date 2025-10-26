package com.example.htql_nhahang_khachsan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardDataDTO {
    private DashboardStatsDTO stats;
    private List<ReceptionistBookingDTO> todayCheckIns;
    private List<ReceptionistBookingDTO> todayCheckOuts;
    private Integer unpaidDepositsCount;
    private Integer roomsCleaningCount;
    private Integer monthlyBookingsCount;
    private Double occupancyRate;
    private BigDecimal todayRevenue;
}