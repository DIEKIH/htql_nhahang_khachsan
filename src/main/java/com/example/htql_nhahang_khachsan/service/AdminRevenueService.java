package com.example.htql_nhahang_khachsan.service;

import com.example.htql_nhahang_khachsan.dto.RevenueStatisticsDTO;
import com.example.htql_nhahang_khachsan.entity.OrderEntity;
import com.example.htql_nhahang_khachsan.entity.RoomBookingEntity;
import com.example.htql_nhahang_khachsan.enums.OrderStatus;
import com.example.htql_nhahang_khachsan.enums.PaymentStatus;
import com.example.htql_nhahang_khachsan.repository.OrderRepository;
import com.example.htql_nhahang_khachsan.repository.RoomBookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminRevenueService {

    private final RoomBookingRepository bookingRepository;
    private final OrderRepository orderRepository;

    public RevenueStatisticsDTO getRevenueStatistics(Long branchId, String period, LocalDate fromDate, LocalDate toDate) {
        // Tính toán khoảng thời gian
        LocalDate[] dateRange = calculateDateRange(period, fromDate, toDate);
        LocalDate startDate = dateRange[0];
        LocalDate endDate = dateRange[1];

        // Lấy dữ liệu doanh thu
        List<RoomBookingEntity> bookings = getBookings(branchId, startDate, endDate);
        List<OrderEntity> orders = getOrders(branchId, startDate, endDate);

        // Tính toán thống kê
        BigDecimal hotelRevenue = calculateHotelRevenue(bookings);
        BigDecimal restaurantRevenue = calculateRestaurantRevenue(orders);
        BigDecimal totalRevenue = hotelRevenue.add(restaurantRevenue);

        // Tạo dữ liệu biểu đồ
        List<RevenueStatisticsDTO.ChartDataPoint> chartData = createChartData(
                period, startDate, endDate, bookings, orders
        );

        // Tính tỷ lệ tăng trưởng
        BigDecimal growthRate = calculateGrowthRate(branchId, period, startDate, endDate);

        return RevenueStatisticsDTO.builder()
                .totalRevenue(totalRevenue)
                .hotelRevenue(hotelRevenue)
                .restaurantRevenue(restaurantRevenue)
                .totalBookings(bookings.size())
                .totalOrders(orders.size())
                .chartData(chartData)
                .fromDate(startDate)
                .toDate(endDate)
                .period(period)
                .growthRate(growthRate)
                .build();
    }

    private LocalDate[] calculateDateRange(String period, LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null && toDate != null) {
            return new LocalDate[]{fromDate, toDate};
        }

        LocalDate endDate = LocalDate.now();
        LocalDate startDate;

        switch (period) {
            case "WEEK":
                startDate = endDate.minusWeeks(11); // 12 tuần
                break;
            case "YEAR":
                startDate = endDate.minusYears(4).withDayOfYear(1); // 5 năm
                endDate = endDate.withDayOfYear(endDate.lengthOfYear());
                break;
            case "QUARTER":
                startDate = endDate.minusMonths(11).withDayOfMonth(1); // 4 quý (12 tháng)
                endDate = endDate.withDayOfMonth(endDate.lengthOfMonth());
                break;
            default: // MONTH
                startDate = endDate.minusMonths(11).withDayOfMonth(1); // 12 tháng
                endDate = endDate.withDayOfMonth(endDate.lengthOfMonth());
        }

        return new LocalDate[]{startDate, endDate};
    }

    private List<RoomBookingEntity> getBookings(Long branchId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        if (branchId != null) {
            return bookingRepository.findByBranchIdAndCreatedAtBetweenAndPaymentStatus(
                    branchId, startDateTime, endDateTime, PaymentStatus.PAID
            );
        } else {
            return bookingRepository.findByCreatedAtBetweenAndPaymentStatus(
                    startDateTime, endDateTime, PaymentStatus.PAID
            );
        }
    }

    private List<OrderEntity> getOrders(Long branchId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        if (branchId != null) {
            return orderRepository.findByBranchIdAndCreatedAtBetweenAndStatusIn(
                    branchId, startDateTime, endDateTime,
                    List.of(OrderStatus.COMPLETED, OrderStatus.SERVED)
            );
        } else {
            return orderRepository.findByCreatedAtBetweenAndStatusIn(
                    startDateTime, endDateTime,
                    List.of(OrderStatus.COMPLETED, OrderStatus.SERVED)
            );
        }
    }

    private BigDecimal calculateHotelRevenue(List<RoomBookingEntity> bookings) {
        return bookings.stream()
                .map(RoomBookingEntity::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateRestaurantRevenue(List<OrderEntity> orders) {
        return orders.stream()
                .map(OrderEntity::getFinalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<RevenueStatisticsDTO.ChartDataPoint> createChartData(
            String period, LocalDate startDate, LocalDate endDate,
            List<RoomBookingEntity> bookings, List<OrderEntity> orders) {

        List<RevenueStatisticsDTO.ChartDataPoint> chartData = new ArrayList<>();

        switch (period) {
            case "WEEK":
                chartData = createWeeklyChartData(startDate, endDate, bookings, orders);
                break;
            case "QUARTER":
                chartData = createQuarterlyChartData(startDate, endDate, bookings, orders);
                break;
            case "YEAR":
                chartData = createYearlyChartData(startDate, endDate, bookings, orders);
                break;
            default: // MONTH
                chartData = createMonthlyChartData(startDate, endDate, bookings, orders);
        }

        return chartData;
    }

    private List<RevenueStatisticsDTO.ChartDataPoint> createWeeklyChartData(
            LocalDate startDate, LocalDate endDate,
            List<RoomBookingEntity> bookings, List<OrderEntity> orders) {

        List<RevenueStatisticsDTO.ChartDataPoint> chartData = new ArrayList<>();
        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate)) {
            LocalDate weekStart = currentDate;
            LocalDate weekEnd = currentDate.plusDays(6);
            if (weekEnd.isAfter(endDate)) {
                weekEnd = endDate;
            }

            String label = "Tuần " + weekStart.format(DateTimeFormatter.ofPattern("dd/MM"));

            BigDecimal hotelRev = calculateRevenueForPeriod(bookings, weekStart, weekEnd);
            BigDecimal restaurantRev = calculateOrderRevenueForPeriod(orders, weekStart, weekEnd);
            int bookingCount = countBookingsForPeriod(bookings, weekStart, weekEnd);
            int orderCount = countOrdersForPeriod(orders, weekStart, weekEnd);

            chartData.add(RevenueStatisticsDTO.ChartDataPoint.builder()
                    .label(label)
                    .hotelRevenue(hotelRev)
                    .restaurantRevenue(restaurantRev)
                    .totalRevenue(hotelRev.add(restaurantRev))
                    .bookingCount(bookingCount)
                    .orderCount(orderCount)
                    .build());

            currentDate = weekEnd.plusDays(1);
        }

        return chartData;
    }

    private List<RevenueStatisticsDTO.ChartDataPoint> createMonthlyChartData(
            LocalDate startDate, LocalDate endDate,
            List<RoomBookingEntity> bookings, List<OrderEntity> orders) {

        List<RevenueStatisticsDTO.ChartDataPoint> chartData = new ArrayList<>();
        YearMonth currentMonth = YearMonth.from(startDate);
        YearMonth lastMonth = YearMonth.from(endDate);

        while (!currentMonth.isAfter(lastMonth)) {
            LocalDate monthStart = currentMonth.atDay(1);
            LocalDate monthEnd = currentMonth.atEndOfMonth();

            String label = "Tháng " + currentMonth.getMonthValue() + "/" + currentMonth.getYear();

            BigDecimal hotelRev = calculateRevenueForPeriod(bookings, monthStart, monthEnd);
            BigDecimal restaurantRev = calculateOrderRevenueForPeriod(orders, monthStart, monthEnd);
            int bookingCount = countBookingsForPeriod(bookings, monthStart, monthEnd);
            int orderCount = countOrdersForPeriod(orders, monthStart, monthEnd);

            chartData.add(RevenueStatisticsDTO.ChartDataPoint.builder()
                    .label(label)
                    .hotelRevenue(hotelRev)
                    .restaurantRevenue(restaurantRev)
                    .totalRevenue(hotelRev.add(restaurantRev))
                    .bookingCount(bookingCount)
                    .orderCount(orderCount)
                    .build());

            currentMonth = currentMonth.plusMonths(1);
        }

        return chartData;
    }

    private List<RevenueStatisticsDTO.ChartDataPoint> createQuarterlyChartData(
            LocalDate startDate, LocalDate endDate,
            List<RoomBookingEntity> bookings, List<OrderEntity> orders) {

        List<RevenueStatisticsDTO.ChartDataPoint> chartData = new ArrayList<>();
        Map<String, RevenueStatisticsDTO.ChartDataPoint> quarterMap = new HashMap<>();

        YearMonth currentMonth = YearMonth.from(startDate);
        YearMonth lastMonth = YearMonth.from(endDate);

        while (!currentMonth.isAfter(lastMonth)) {
            int quarter = (currentMonth.getMonthValue() - 1) / 3 + 1;
            int year = currentMonth.getYear();
            String label = "Quý " + quarter + "/" + year;

            LocalDate monthStart = currentMonth.atDay(1);
            LocalDate monthEnd = currentMonth.atEndOfMonth();

            BigDecimal hotelRev = calculateRevenueForPeriod(bookings, monthStart, monthEnd);
            BigDecimal restaurantRev = calculateOrderRevenueForPeriod(orders, monthStart, monthEnd);
            int bookingCount = countBookingsForPeriod(bookings, monthStart, monthEnd);
            int orderCount = countOrdersForPeriod(orders, monthStart, monthEnd);

            RevenueStatisticsDTO.ChartDataPoint existing = quarterMap.get(label);
            if (existing != null) {
                existing.setHotelRevenue(existing.getHotelRevenue().add(hotelRev));
                existing.setRestaurantRevenue(existing.getRestaurantRevenue().add(restaurantRev));
                existing.setTotalRevenue(existing.getTotalRevenue().add(hotelRev).add(restaurantRev));
                existing.setBookingCount(existing.getBookingCount() + bookingCount);
                existing.setOrderCount(existing.getOrderCount() + orderCount);
            } else {
                quarterMap.put(label, RevenueStatisticsDTO.ChartDataPoint.builder()
                        .label(label)
                        .hotelRevenue(hotelRev)
                        .restaurantRevenue(restaurantRev)
                        .totalRevenue(hotelRev.add(restaurantRev))
                        .bookingCount(bookingCount)
                        .orderCount(orderCount)
                        .build());
            }

            currentMonth = currentMonth.plusMonths(1);
        }

        quarterMap.keySet().stream().sorted().forEach(key -> chartData.add(quarterMap.get(key)));
        return chartData;
    }

    private List<RevenueStatisticsDTO.ChartDataPoint> createYearlyChartData(
            LocalDate startDate, LocalDate endDate,
            List<RoomBookingEntity> bookings, List<OrderEntity> orders) {

        List<RevenueStatisticsDTO.ChartDataPoint> chartData = new ArrayList<>();
        int startYear = startDate.getYear();
        int endYear = endDate.getYear();

        for (int year = startYear; year <= endYear; year++) {
            LocalDate yearStart = LocalDate.of(year, 1, 1);
            LocalDate yearEnd = LocalDate.of(year, 12, 31);

            if (yearStart.isBefore(startDate)) yearStart = startDate;
            if (yearEnd.isAfter(endDate)) yearEnd = endDate;

            String label = "Năm " + year;

            BigDecimal hotelRev = calculateRevenueForPeriod(bookings, yearStart, yearEnd);
            BigDecimal restaurantRev = calculateOrderRevenueForPeriod(orders, yearStart, yearEnd);
            int bookingCount = countBookingsForPeriod(bookings, yearStart, yearEnd);
            int orderCount = countOrdersForPeriod(orders, yearStart, yearEnd);

            chartData.add(RevenueStatisticsDTO.ChartDataPoint.builder()
                    .label(label)
                    .hotelRevenue(hotelRev)
                    .restaurantRevenue(restaurantRev)
                    .totalRevenue(hotelRev.add(restaurantRev))
                    .bookingCount(bookingCount)
                    .orderCount(orderCount)
                    .build());
        }

        return chartData;
    }

    private BigDecimal calculateRevenueForPeriod(List<RoomBookingEntity> bookings, LocalDate start, LocalDate end) {
        return bookings.stream()
                .filter(b -> {
                    LocalDate bookingDate = b.getCreatedAt().toLocalDate();
                    return !bookingDate.isBefore(start) && !bookingDate.isAfter(end);
                })
                .map(RoomBookingEntity::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateOrderRevenueForPeriod(List<OrderEntity> orders, LocalDate start, LocalDate end) {
        return orders.stream()
                .filter(o -> {
                    LocalDate orderDate = o.getCreatedAt().toLocalDate();
                    return !orderDate.isBefore(start) && !orderDate.isAfter(end);
                })
                .map(OrderEntity::getFinalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private int countBookingsForPeriod(List<RoomBookingEntity> bookings, LocalDate start, LocalDate end) {
        return (int) bookings.stream()
                .filter(b -> {
                    LocalDate bookingDate = b.getCreatedAt().toLocalDate();
                    return !bookingDate.isBefore(start) && !bookingDate.isAfter(end);
                })
                .count();
    }

    private int countOrdersForPeriod(List<OrderEntity> orders, LocalDate start, LocalDate end) {
        return (int) orders.stream()
                .filter(o -> {
                    LocalDate orderDate = o.getCreatedAt().toLocalDate();
                    return !orderDate.isBefore(start) && !orderDate.isAfter(end);
                })
                .count();
    }

    private BigDecimal calculateGrowthRate(Long branchId, String period, LocalDate startDate, LocalDate endDate) {
        // Tính khoảng thời gian kỳ trước
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate);
        LocalDate prevStartDate = startDate.minusDays(daysBetween + 1);
        LocalDate prevEndDate = startDate.minusDays(1);

        // Lấy doanh thu kỳ hiện tại
        List<RoomBookingEntity> currentBookings = getBookings(branchId, startDate, endDate);
        List<OrderEntity> currentOrders = getOrders(branchId, startDate, endDate);
        BigDecimal currentRevenue = calculateHotelRevenue(currentBookings)
                .add(calculateRestaurantRevenue(currentOrders));

        // Lấy doanh thu kỳ trước
        List<RoomBookingEntity> prevBookings = getBookings(branchId, prevStartDate, prevEndDate);
        List<OrderEntity> prevOrders = getOrders(branchId, prevStartDate, prevEndDate);
        BigDecimal prevRevenue = calculateHotelRevenue(prevBookings)
                .add(calculateRestaurantRevenue(prevOrders));

        // Tính tỷ lệ tăng trưởng
        if (prevRevenue.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return currentRevenue.subtract(prevRevenue)
                .divide(prevRevenue, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
}