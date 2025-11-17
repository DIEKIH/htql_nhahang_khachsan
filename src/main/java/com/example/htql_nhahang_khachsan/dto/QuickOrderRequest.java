package com.example.htql_nhahang_khachsan.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * DTO cho đặt món nhanh - bỏ qua giỏ hàng
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuickOrderRequest {

    // Thông tin món ăn
    @NotNull(message = "Vui lòng chọn món ăn")
    private Long menuItemId;

    @NotNull(message = "Vui lòng chọn chi nhánh")
    private Long branchId;

    @Min(value = 1, message = "Số lượng tối thiểu là 1")
    @Max(value = 50, message = "Số lượng tối đa là 50")
    private Integer quantity = 1;

    private String notes; // Ghi chú món ăn (VD: không hành, ít cay)

    private Boolean isTakeaway = false; // Mang đi hay ăn tại chỗ

    // Thông tin khách hàng (sẽ được điền ở bước tiếp theo)
    @NotBlank(message = "Vui lòng nhập họ tên")
    @Size(min = 2, max = 100, message = "Họ tên từ 2-100 ký tự")
    private String customerName;

    @NotBlank(message = "Vui lòng nhập số điện thoại")
    @Pattern(regexp = "^0\\d{9,10}$", message = "Số điện thoại không hợp lệ")
    private String customerPhone;

    @NotBlank(message = "Vui lòng nhập địa chỉ")
    @Size(min = 10, message = "Địa chỉ quá ngắn")
    private String customerAddress;

    private String orderNotes; // Ghi chú chung cho đơn hàng

    @NotBlank(message = "Vui lòng chọn phương thức thanh toán")
    private String paymentMethod; // CASH_ON_DELIVERY, VNPAY, MOMO, BANK_TRANSFER

    // Thông tin giá (để hiển thị)
    private BigDecimal unitPrice;
    private String menuItemName;
    private String menuItemImage;
}