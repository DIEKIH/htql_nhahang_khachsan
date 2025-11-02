package com.example.htql_nhahang_khachsan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevenueStatisticsDTO {

    // Tổng quan
    private BigDecimal totalRevenue;
    private BigDecimal hotelRevenue;
    private BigDecimal restaurantRevenue;
    private Integer totalBookings;
    private Integer totalOrders;

    // Dữ liệu biểu đồ
    private List<ChartDataPoint> chartData;

    // Thống kê theo khoảng thời gian
    private LocalDate fromDate;
    private LocalDate toDate;
    private String period; // WEEK, MONTH, YEAR

    // So sánh với kỳ trước
    private BigDecimal growthRate;
    private BigDecimal previousPeriodRevenue;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChartDataPoint {
        private String label; // Tên khoảng thời gian (Tuần 1, Tháng 1, 2024, etc)
        private BigDecimal hotelRevenue;
        private BigDecimal restaurantRevenue;
        private BigDecimal totalRevenue;
        private Integer bookingCount;
        private Integer orderCount;
    }
}