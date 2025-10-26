package com.example.htql_nhahang_khachsan.dto;

import com.example.htql_nhahang_khachsan.enums.OrderStatus;
import com.example.htql_nhahang_khachsan.enums.PaymentMethod;
import com.example.htql_nhahang_khachsan.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {
    private Long id;
    private String orderCode;

    // Thông tin bàn
    private Long tableId;
    private String tableName;

    // Thông tin nhân viên
    private String cashierName;      // Tên Thu Ngân
    private String staffName;        // Tên Phục Vụ

    // Chi tiết món
    private List<OrderItemDTO> items;

    // Số tiền
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;

    // Trạng thái
    private OrderStatus status;
    private PaymentStatus paymentStatus;
    private PaymentMethod paymentMethod;

    // Thời gian
    private LocalDateTime orderTime;
    private LocalDateTime completedTime;

    // Ghi chú
    private String notes;
}