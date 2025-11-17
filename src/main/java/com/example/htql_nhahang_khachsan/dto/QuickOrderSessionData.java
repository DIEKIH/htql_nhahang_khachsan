package com.example.htql_nhahang_khachsan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * DTO lưu thông tin quick order vào session
 * Phải implements Serializable để có thể lưu vào HttpSession
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuickOrderSessionData implements Serializable {

    private static final long serialVersionUID = 1L; // Best practice cho Serializable

    private Long menuItemId;
    private Long branchId;
    private Integer quantity;
    private BigDecimal unitPrice;
    private String menuItemName;
    private String menuItemImage;
    private String branchName;
}