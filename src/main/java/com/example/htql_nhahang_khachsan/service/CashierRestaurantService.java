package com.example.htql_nhahang_khachsan.service;

import com.example.htql_nhahang_khachsan.dto.*;
import com.example.htql_nhahang_khachsan.entity.*;
import com.example.htql_nhahang_khachsan.enums.*;
import com.example.htql_nhahang_khachsan.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CashierRestaurantService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final MenuItemRepository menuItemRepository;
    private final MenuCategoryRepository menuCategoryRepository;
    private final RestaurantTableRepository tableRepository;
    private final BranchRepository branchRepository;
    private final PromotionRepository promotionRepository;
    private final TableBookingRepository tableBookingRepository;
    private final RestaurantTableRepository restaurantTableRepository;


    public Map<String, Object> getDashboardStats(Long branchId) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(23, 59, 59);

        Map<String, Object> stats = new HashMap<>();

        long ordersToday = orderRepository.countByBranchIdAndOrderTimeBetween(branchId, startOfDay, endOfDay);
        long pendingOrders = orderRepository.countByBranchIdAndStatus(branchId, OrderStatus.PENDING);
        long availableTables = tableRepository.countByBranchIdAndStatus(branchId, TableStatus.AVAILABLE);
        BigDecimal revenueToday = orderRepository.sumRevenueByBranchAndDate(branchId, startOfDay, endOfDay);

        stats.put("ordersToday", ordersToday);
        stats.put("pendingOrders", pendingOrders);
        stats.put("availableTables", availableTables);
        stats.put("revenueToday", revenueToday != null ? revenueToday : BigDecimal.ZERO);

        return stats;
    }

    public List<OrderDTO> getOrdersByBranch(Long branchId, OrderStatus status) {
        List<OrderEntity> orders = status != null
                ? orderRepository.findByBranchIdAndStatus(branchId, status)
                : orderRepository.findByBranchId(branchId);

        return orders.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public OrderDTO getOrderDetail(Long id, Long branchId) {
        OrderEntity order = orderRepository.findByIdAndBranchId(id, branchId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));

        return convertToDTO(order);
    }

    public OrderDTO createOrderAtCounter(CreateOrderAtCounterRequest request, Long branchId) {
        RestaurantTableEntity table = tableRepository.findByIdAndBranchId(request.getTableId(), branchId)
                .orElseThrow(() -> new IllegalArgumentException("Bàn không hợp lệ"));

        BranchEntity branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new IllegalArgumentException("Chi nhánh không hợp lệ"));

        // Cập nhật trạng thái bàn
        table.setStatus(TableStatus.OCCUPIED);
        tableRepository.save(table);

        OrderEntity order = OrderEntity.builder()
                .branch(branch)
                .table(table)
                .orderType(OrderType.AT_STORE)
                .status(OrderStatus.PENDING)
                .paymentStatus(PaymentStatus.PENDING)
                .customerName(request.getCashierName())
                .notes(request.getNotes())
                .totalAmount(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .build();

        BigDecimal total = BigDecimal.ZERO;
        List<OrderItemEntity> items = new ArrayList<>();

        for (OrderItemRequest itemReq : request.getItems()) {
            MenuItemEntity menuItem = menuItemRepository.findById(itemReq.getMenuItemId())
                    .orElseThrow(() -> new IllegalArgumentException("Món không hợp lệ"));

            // Tính giá sau khuyến mãi
            BigDecimal finalPrice = calculateItemPrice(menuItem, branchId);
            BigDecimal itemTotal = finalPrice.multiply(BigDecimal.valueOf(itemReq.getQuantity()));

            OrderItemEntity orderItem = OrderItemEntity.builder()
                    .order(order)
                    .menuItem(menuItem)
                    .quantity(itemReq.getQuantity())
                    .unitPrice(finalPrice)
                    .totalPrice(itemTotal)
                    .notes(itemReq.getNotes())
                    .status(OrderItemStatus.PENDING)
                    .build();

            items.add(orderItem);
            total = total.add(itemTotal);
        }

        order.setItems(items);
        order.setTotalAmount(total);
        order.setFinalAmount(total);

        OrderEntity saved = orderRepository.save(order);
        return convertToDTO(saved);
    }

    public void confirmOrder(Long orderId, Long branchId) {
        OrderEntity order = orderRepository.findByIdAndBranchId(orderId, branchId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));

        order.setStatus(OrderStatus.CONFIRMED);

        // Cập nhật trạng thái các món
        for (OrderItemEntity item : order.getItems()) {
            item.setStatus(OrderItemStatus.PREPARING);
            item.setPreparationStart(LocalDateTime.now());
        }

        orderRepository.save(order);
    }

    public void confirmPayment(Long orderId, Long branchId, PaymentMethod paymentMethod) {
        OrderEntity order = orderRepository.findByIdAndBranchId(orderId, branchId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));

        order.setPaymentStatus(PaymentStatus.PAID);
        order.setPaymentMethod(paymentMethod);
        order.setStatus(OrderStatus.COMPLETED);
        order.setCompletedTime(LocalDateTime.now());

        // Giải phóng bàn
        if (order.getTable() != null) {
            order.getTable().setStatus(TableStatus.AVAILABLE);
        }

        orderRepository.save(order);
    }

    public void cancelOrder(Long orderId, Long branchId, String reason) {
        OrderEntity order = orderRepository.findByIdAndBranchId(orderId, branchId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));

        order.setStatus(OrderStatus.CANCELLED);
        order.setNotes((order.getNotes() != null ? order.getNotes() + " | " : "") + "Hủy: " + reason);

        // Giải phóng bàn
        if (order.getTable() != null) {
            order.getTable().setStatus(TableStatus.AVAILABLE);
        }

        orderRepository.save(order);
    }

    public List<RestaurantTableDTO> getAvailableTables(Long branchId) {
        return tableRepository.findByBranchIdAndStatus(branchId, TableStatus.AVAILABLE)
                .stream()
                .map(t -> new RestaurantTableDTO(t.getId(), t.getTableNumber(), t.getCapacity(), t.getStatus()))
                .collect(Collectors.toList());
    }

    public List<MenuCategoryDTO> getMenuCategories(Long branchId) {
        return menuCategoryRepository.findByBranchIdAndStatus(branchId, Status.ACTIVE)
                .stream()
                .map(c -> new MenuCategoryDTO(c.getId(), c.getName()))
                .collect(Collectors.toList());
    }

    public List<MenuItemDTO> getMenuItems(Long branchId, Long categoryId) {
        List<MenuItemEntity> items;

        if (categoryId != null) {
            items = menuItemRepository.findByCategoryIdAndStatusAndIsAvailable(categoryId, Status.ACTIVE, true);
        } else {
            // Lấy tất cả items thuộc chi nhánh này
            items = menuItemRepository.findByCategoryBranchIdAndStatusAndIsAvailable(branchId, Status.ACTIVE, true);
        }

        return items.stream()
                .map(i -> {
                    BigDecimal finalPrice = calculateItemPrice(i, branchId);
                    BigDecimal originalPrice = i.getPrice();
                    BigDecimal discountPercent = BigDecimal.ZERO;

                    if (finalPrice.compareTo(originalPrice) < 0) {
                        discountPercent = originalPrice.subtract(finalPrice)
                                .divide(originalPrice, 4, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100));
                    }

                    MenuItemDTO dto = new MenuItemDTO();
                    dto.setId(i.getId());
                    dto.setName(i.getName());
                    dto.setPrice(finalPrice);
                    dto.setImageUrl(i.getImageUrl());
                    dto.setPreparationTime(i.getPreparationTime());
                    dto.setIsAvailable(i.getIsAvailable());
                    dto.setCategoryId(i.getCategory().getId());
                    dto.setCategoryName(i.getCategory().getName());
                    dto.setOriginalPrice(originalPrice);
                    dto.setDiscountPercent(discountPercent);
                    dto.setHasPromotion(!finalPrice.equals(originalPrice));

                    return dto;
                })
                .collect(Collectors.toList());
    }

    private BigDecimal calculateItemPrice(MenuItemEntity item, Long branchId) {
        LocalDateTime now = LocalDateTime.now();

        // Tìm khuyến mãi áp dụng cho món này
        List<PromotionEntity> promotions = promotionRepository.findActivePromotionsForRestaurant(branchId, now);

        BigDecimal finalPrice = item.getPrice();

        for (PromotionEntity promo : promotions) {
            if (promo.getType() == PromotionType.PERCENTAGE) {
                BigDecimal discount = item.getPrice()
                        .multiply(promo.getDiscountValue())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

                if (promo.getMaxDiscount() != null && discount.compareTo(promo.getMaxDiscount()) > 0) {
                    discount = promo.getMaxDiscount();
                }

                finalPrice = finalPrice.subtract(discount);
            } else if (promo.getType() == PromotionType.FIXED_AMOUNT) {
                finalPrice = finalPrice.subtract(promo.getDiscountValue());
            }
        }

        return finalPrice.max(BigDecimal.ZERO);
    }

    public InvoiceDTO generateInvoice(Long orderId, Long branchId) {
        OrderEntity order = orderRepository.findByIdAndBranchId(orderId, branchId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));

        InvoiceDTO invoice = new InvoiceDTO();
        invoice.setOrderCode(order.getOrderCode());

        if (order.getTable() != null) {
            invoice.setTableName(order.getTable().getTableNumber());
        } else {
            invoice.setTableName("N/A");
        }

        invoice.setOrderTime(order.getOrderTime());
        invoice.setTotalAmount(order.getTotalAmount());
        invoice.setDiscountAmount(order.getDiscountAmount());
        invoice.setFinalAmount(order.getFinalAmount());
        invoice.setPaymentStatus(order.getPaymentStatus());
        invoice.setPaymentMethod(order.getPaymentMethod());
        invoice.setNotes(order.getNotes());

        List<InvoiceItemDTO> invoiceItems = order.getItems().stream()
                .map(i -> new InvoiceItemDTO(i.getMenuItem().getName(), i.getQuantity(), i.getUnitPrice(), i.getTotalPrice()))
                .collect(Collectors.toList());

        invoice.setItems(invoiceItems);
        return invoice;
    }

    private OrderDTO convertToDTO(OrderEntity order) {
        OrderDTO dto = new OrderDTO();
        dto.setId(order.getId());
        dto.setOrderCode(order.getOrderCode());

        if (order.getTable() != null) {
            dto.setTableId(order.getTable().getId());
            dto.setTableName(order.getTable().getTableNumber());
        } else {
            dto.setTableId(null);
            dto.setTableName("Không có bàn");
        }

        if (order.getCashier() != null) {
            dto.setCashierName(order.getCashier().getFullName());
        } else if (order.getCustomerName() != null) {
            dto.setCashierName(order.getCustomerName());
        } else {
            dto.setCashierName("-");
        }

        if (order.getStaff() != null) {
            dto.setStaffName(order.getStaff().getFullName());
        } else {
            dto.setStaffName("-");
        }

        dto.setTotalAmount(order.getTotalAmount());
        dto.setDiscountAmount(order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO);
        dto.setFinalAmount(order.getFinalAmount());
        dto.setStatus(order.getStatus());
        dto.setPaymentStatus(order.getPaymentStatus());
        dto.setPaymentMethod(order.getPaymentMethod());
        dto.setOrderTime(order.getOrderTime());
        dto.setCompletedTime(order.getCompletedTime());
        dto.setNotes(order.getNotes());

        List<OrderItemDTO> items = order.getItems().stream()
                .map(i -> new OrderItemDTO(
                        i.getId(),
                        i.getMenuItem().getName(),
                        i.getQuantity(),
                        i.getUnitPrice(),
                        i.getTotalPrice(),
                        i.getNotes(),
                        i.getStatus()
                ))
                .collect(Collectors.toList());

        dto.setItems(items);
        return dto;
    }


    //bàn
    // Thêm vào CashierRestaurantService.java (hoặc tạo service riêng)

    public List<TableBookingResponse> getTodayBookings(Long branchId, BookingStatus status) {
        LocalDate today = LocalDate.now();

        List<TableBookingEntity> bookings;
        if (status != null) {
            bookings = tableBookingRepository.findByBranchIdAndBookingDateAndStatus(
                    branchId, today, status);
        } else {
            bookings = tableBookingRepository.findByBranchIdAndBookingDate(branchId, today);
        }

        return bookings.stream()
                .map(this::convertToResponse)
                .sorted(Comparator.comparing(TableBookingResponse::getBookingTime))
                .collect(Collectors.toList());
    }

    public TableBookingResponse getBookingDetail(Long bookingId, Long branchId) {
        TableBookingEntity booking = tableBookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đặt bàn"));

        if (!booking.getBranch().getId().equals(branchId)) {
            throw new RuntimeException("Đặt bàn không thuộc chi nhánh này");
        }

        return convertToResponse(booking);
    }

    public void checkInBooking(Long bookingId, Long branchId) {
        TableBookingEntity booking = tableBookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đặt bàn"));

        if (!booking.getBranch().getId().equals(branchId)) {
            throw new RuntimeException("Đặt bàn không thuộc chi nhánh này");
        }

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new RuntimeException("Chỉ có thể check-in đặt bàn đã xác nhận");
        }

        booking.setStatus(BookingStatus.CHECKED_IN);
        booking.setActualArrival(LocalDateTime.now());

        // Cập nhật trạng thái bàn sang OCCUPIED
        for (RestaurantTableEntity table : booking.getTables()) {
            table.setStatus(TableStatus.OCCUPIED);
            restaurantTableRepository.save(table);
        }

        tableBookingRepository.save(booking);
    }

    public void checkOutBooking(Long bookingId, Long branchId) {
        TableBookingEntity booking = tableBookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đặt bàn"));

        if (!booking.getBranch().getId().equals(branchId)) {
            throw new RuntimeException("Đặt bàn không thuộc chi nhánh này");
        }

        if (booking.getStatus() != BookingStatus.CHECKED_IN) {
            throw new RuntimeException("Chỉ có thể check-out đặt bàn đã check-in");
        }

        booking.setStatus(BookingStatus.CHECKED_OUT);

        // Giải phóng bàn
        for (RestaurantTableEntity table : booking.getTables()) {
            table.setStatus(TableStatus.AVAILABLE);
            restaurantTableRepository.save(table);
        }

        tableBookingRepository.save(booking);
    }

    public void markNoShow(Long bookingId, Long branchId) {
        TableBookingEntity booking = tableBookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đặt bàn"));

        if (!booking.getBranch().getId().equals(branchId)) {
            throw new RuntimeException("Đặt bàn không thuộc chi nhánh này");
        }

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new RuntimeException("Chỉ có thể đánh dấu No Show cho đặt bàn đã xác nhận");
        }

        booking.setStatus(BookingStatus.NO_SHOW);

        // Giải phóng bàn đã đặt trước
        for (RestaurantTableEntity table : booking.getTables()) {
            if (table.getStatus() == TableStatus.RESERVED) {
                table.setStatus(TableStatus.AVAILABLE);
                restaurantTableRepository.save(table);
            }
        }

        tableBookingRepository.save(booking);
    }

    private TableBookingResponse convertToResponse(TableBookingEntity booking) {
        List<TableInfo> tableInfos = booking.getTables().stream()
                .map(t -> TableInfo.builder()
                        .id(t.getId())
                        .tableNumber(t.getTableNumber())
                        .capacity(t.getCapacity())
                        .status(t.getStatus())
                        .build())
                .collect(Collectors.toList());

        return TableBookingResponse.builder()
                .id(booking.getId())
                .bookingCode(booking.getBookingCode())
                .customerName(booking.getCustomerName())
                .customerEmail(booking.getCustomerEmail())
                .contactPhone(booking.getContactPhone())
                .branchName(booking.getBranch().getName())
                .bookingDate(booking.getBookingDate())
                .bookingTime(booking.getBookingTime())
                .partySize(booking.getPartySize())
                .assignedTables(tableInfos)
                .status(booking.getStatus())
                .statusDisplay(getStatusDisplay(booking.getStatus()))
                .specialRequests(booking.getSpecialRequests())
                .createdAt(booking.getCreatedAt())
                .actualArrival(booking.getActualArrival())
                .build();
    }

    private String getStatusDisplay(BookingStatus status) {
        switch (status) {
            case PENDING: return "Chờ Xác Nhận";
            case CONFIRMED: return "Đã Xác Nhận";
            case CHECKED_IN: return "Đã Check-in";
            case CHECKED_OUT: return "Đã Check-out";
            case CANCELLED: return "Đã Hủy";
            case NO_SHOW: return "Không Đến";
            default: return status.name();
        }
    }
}