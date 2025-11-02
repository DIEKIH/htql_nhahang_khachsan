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
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ManagerRevenueService {

    private final RoomBookingRepository bookingRepository;
    private final OrderRepository orderRepository;

    public RevenueStatisticsDTO getRevenueStatistics(Long branchId, String period, LocalDate fromDate, LocalDate toDate) {

        // Nếu không có fromDate/toDate, tự động set dựa vào period
        if (fromDate == null || toDate == null) {
            LocalDate[] dates = getDefaultDateRange(period);
            fromDate = dates[0];
            toDate = dates[1];
        }

        // Lấy dữ liệu bookings và orders
        List<RoomBookingEntity> bookings = bookingRepository.findByBranchIdAndBookingDateBetweenAndPaymentStatus(
                branchId, fromDate.atStartOfDay(), toDate.atTime(23, 59, 59), PaymentStatus.PAID);

        List<OrderEntity> orders = orderRepository.findByBranchIdAndOrderTimeBetweenAndStatus(
                branchId, fromDate.atStartOfDay(), toDate.atTime(23, 59, 59), OrderStatus.COMPLETED);

        // Tính tổng doanh thu
        BigDecimal hotelRevenue = bookings.stream()
                .map(RoomBookingEntity::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal restaurantRevenue = orders.stream()
                .map(OrderEntity::getFinalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRevenue = hotelRevenue.add(restaurantRevenue);

        // Tạo dữ liệu biểu đồ
        List<RevenueStatisticsDTO.ChartDataPoint> chartData = createChartData(period, fromDate, toDate, bookings, orders);

        // Tính tốc độ tăng trưởng
        LocalDate[] previousDates = getPreviousPeriodDateRange(period, fromDate, toDate);
        BigDecimal previousRevenue = calculatePreviousPeriodRevenue(branchId, previousDates[0], previousDates[1]);
        BigDecimal growthRate = calculateGrowthRate(totalRevenue, previousRevenue);

        return RevenueStatisticsDTO.builder()
                .totalRevenue(totalRevenue)
                .hotelRevenue(hotelRevenue)
                .restaurantRevenue(restaurantRevenue)
                .totalBookings(bookings.size())
                .totalOrders(orders.size())
                .chartData(chartData)
                .fromDate(fromDate)
                .toDate(toDate)
                .period(period)
                .growthRate(growthRate)
                .previousPeriodRevenue(previousRevenue)
                .build();
    }

    private LocalDate[] getDefaultDateRange(String period) {
        LocalDate today = LocalDate.now();
        LocalDate fromDate, toDate = today;

        switch (period.toUpperCase()) {
            case "WEEK":
                fromDate = today.with(DayOfWeek.MONDAY);
                toDate = today.with(DayOfWeek.SUNDAY);
                break;
            case "MONTH":
                fromDate = today.withDayOfMonth(1);
                toDate = today.withDayOfMonth(today.lengthOfMonth());
                break;
            case "YEAR":
                fromDate = today.withDayOfYear(1);
                toDate = today.withDayOfYear(today.lengthOfYear());
                break;
            default:
                fromDate = today.minusDays(30);
        }

        return new LocalDate[]{fromDate, toDate};
    }

    private LocalDate[] getPreviousPeriodDateRange(String period, LocalDate fromDate, LocalDate toDate) {
        long daysBetween = ChronoUnit.DAYS.between(fromDate, toDate);
        LocalDate previousFrom = fromDate.minusDays(daysBetween + 1);
        LocalDate previousTo = fromDate.minusDays(1);
        return new LocalDate[]{previousFrom, previousTo};
    }

    private BigDecimal calculatePreviousPeriodRevenue(Long branchId, LocalDate fromDate, LocalDate toDate) {
        List<RoomBookingEntity> bookings = bookingRepository.findByBranchIdAndBookingDateBetweenAndPaymentStatus(
                branchId, fromDate.atStartOfDay(), toDate.atTime(23, 59, 59), PaymentStatus.PAID);

        List<OrderEntity> orders = orderRepository.findByBranchIdAndOrderTimeBetweenAndStatus(
                branchId, fromDate.atStartOfDay(), toDate.atTime(23, 59, 59), OrderStatus.COMPLETED);

        BigDecimal hotelRevenue = bookings.stream()
                .map(RoomBookingEntity::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal restaurantRevenue = orders.stream()
                .map(OrderEntity::getFinalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return hotelRevenue.add(restaurantRevenue);
    }

    private BigDecimal calculateGrowthRate(BigDecimal current, BigDecimal previous) {
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return current.subtract(previous)
                .divide(previous, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    private List<RevenueStatisticsDTO.ChartDataPoint> createChartData(String period, LocalDate fromDate, LocalDate toDate,
                                                                      List<RoomBookingEntity> bookings, List<OrderEntity> orders) {
        List<RevenueStatisticsDTO.ChartDataPoint> chartData = new ArrayList<>();

        switch (period.toUpperCase()) {
            case "WEEK":
                chartData = createWeeklyChartData(fromDate, toDate, bookings, orders);
                break;
            case "MONTH":
                chartData = createMonthlyChartData(fromDate, toDate, bookings, orders);
                break;
            case "YEAR":
                chartData = createYearlyChartData(fromDate, toDate, bookings, orders);
                break;
        }

        return chartData;
    }

    private List<RevenueStatisticsDTO.ChartDataPoint> createWeeklyChartData(LocalDate fromDate, LocalDate toDate,
                                                                            List<RoomBookingEntity> bookings, List<OrderEntity> orders) {
        List<RevenueStatisticsDTO.ChartDataPoint> data = new ArrayList<>();
        WeekFields weekFields = WeekFields.of(Locale.getDefault());

        LocalDate current = fromDate;
        while (!current.isAfter(toDate)) {
            LocalDate weekStart = current.with(DayOfWeek.MONDAY);
            LocalDate weekEnd = current.with(DayOfWeek.SUNDAY);

            if (weekEnd.isAfter(toDate)) {
                weekEnd = toDate;
            }

            final LocalDate finalWeekStart = weekStart;
            final LocalDate finalWeekEnd = weekEnd;

            List<RoomBookingEntity> weekBookings = bookings.stream()
                    .filter(b -> !b.getBookingDate().toLocalDate().isBefore(finalWeekStart) &&
                            !b.getBookingDate().toLocalDate().isAfter(finalWeekEnd))
                    .collect(Collectors.toList());

            List<OrderEntity> weekOrders = orders.stream()
                    .filter(o -> !o.getOrderTime().toLocalDate().isBefore(finalWeekStart) &&
                            !o.getOrderTime().toLocalDate().isAfter(finalWeekEnd))
                    .collect(Collectors.toList());

            BigDecimal hotelRev = weekBookings.stream()
                    .map(RoomBookingEntity::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal restaurantRev = weekOrders.stream()
                    .map(OrderEntity::getFinalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            data.add(RevenueStatisticsDTO.ChartDataPoint.builder()
                    .label("Tuần " + weekStart.get(weekFields.weekOfYear()))
                    .hotelRevenue(hotelRev)
                    .restaurantRevenue(restaurantRev)
                    .totalRevenue(hotelRev.add(restaurantRev))
                    .bookingCount(weekBookings.size())
                    .orderCount(weekOrders.size())
                    .build());

            current = weekEnd.plusDays(1);
        }

        return data;
    }

    private List<RevenueStatisticsDTO.ChartDataPoint> createMonthlyChartData(LocalDate fromDate, LocalDate toDate,
                                                                             List<RoomBookingEntity> bookings, List<OrderEntity> orders) {
        List<RevenueStatisticsDTO.ChartDataPoint> data = new ArrayList<>();

        LocalDate current = fromDate.withDayOfMonth(1);
        while (!current.isAfter(toDate)) {
            LocalDate monthEnd = current.withDayOfMonth(current.lengthOfMonth());
            if (monthEnd.isAfter(toDate)) {
                monthEnd = toDate;
            }

            final LocalDate finalCurrent = current;
            final LocalDate finalMonthEnd = monthEnd;

            List<RoomBookingEntity> monthBookings = bookings.stream()
                    .filter(b -> !b.getBookingDate().toLocalDate().isBefore(finalCurrent) &&
                            !b.getBookingDate().toLocalDate().isAfter(finalMonthEnd))
                    .collect(Collectors.toList());

            List<OrderEntity> monthOrders = orders.stream()
                    .filter(o -> !o.getOrderTime().toLocalDate().isBefore(finalCurrent) &&
                            !o.getOrderTime().toLocalDate().isAfter(finalMonthEnd))
                    .collect(Collectors.toList());

            BigDecimal hotelRev = monthBookings.stream()
                    .map(RoomBookingEntity::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal restaurantRev = monthOrders.stream()
                    .map(OrderEntity::getFinalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            data.add(RevenueStatisticsDTO.ChartDataPoint.builder()
                    .label("Tháng " + current.getMonthValue())
                    .hotelRevenue(hotelRev)
                    .restaurantRevenue(restaurantRev)
                    .totalRevenue(hotelRev.add(restaurantRev))
                    .bookingCount(monthBookings.size())
                    .orderCount(monthOrders.size())
                    .build());

            current = current.plusMonths(1);
        }

        return data;
    }

    private List<RevenueStatisticsDTO.ChartDataPoint> createYearlyChartData(LocalDate fromDate, LocalDate toDate,
                                                                            List<RoomBookingEntity> bookings, List<OrderEntity> orders) {
        List<RevenueStatisticsDTO.ChartDataPoint> data = new ArrayList<>();

        int startYear = fromDate.getYear();
        int endYear = toDate.getYear();

        for (int year = startYear; year <= endYear; year++) {
            LocalDate yearStart = LocalDate.of(year, 1, 1);
            LocalDate yearEnd = LocalDate.of(year, 12, 31);

            if (yearStart.isBefore(fromDate)) {
                yearStart = fromDate;
            }
            if (yearEnd.isAfter(toDate)) {
                yearEnd = toDate;
            }

            final LocalDate finalYearStart = yearStart;
            final LocalDate finalYearEnd = yearEnd;

            List<RoomBookingEntity> yearBookings = bookings.stream()
                    .filter(b -> !b.getBookingDate().toLocalDate().isBefore(finalYearStart) &&
                            !b.getBookingDate().toLocalDate().isAfter(finalYearEnd))
                    .collect(Collectors.toList());

            List<OrderEntity> yearOrders = orders.stream()
                    .filter(o -> !o.getOrderTime().toLocalDate().isBefore(finalYearStart) &&
                            !o.getOrderTime().toLocalDate().isAfter(finalYearEnd))
                    .collect(Collectors.toList());

            BigDecimal hotelRev = yearBookings.stream()
                    .map(RoomBookingEntity::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal restaurantRev = yearOrders.stream()
                    .map(OrderEntity::getFinalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            data.add(RevenueStatisticsDTO.ChartDataPoint.builder()
                    .label("Năm " + year)
                    .hotelRevenue(hotelRev)
                    .restaurantRevenue(restaurantRev)
                    .totalRevenue(hotelRev.add(restaurantRev))
                    .bookingCount(yearBookings.size())
                    .orderCount(yearOrders.size())
                    .build());
        }

        return data;
    }
}