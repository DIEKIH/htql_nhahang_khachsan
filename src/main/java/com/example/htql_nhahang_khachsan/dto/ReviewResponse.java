package com.example.htql_nhahang_khachsan.dto;

import com.example.htql_nhahang_khachsan.enums.ReviewStatus;
import com.example.htql_nhahang_khachsan.enums.ReviewType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewResponse {

    private Long id;
    private Long customerId;
    private String customerName;
    private String customerAvatar;
    private Long branchId;
    private Long roomBookingId;
    private ReviewType type;
    private Integer rating;
    private String comment;
    private String staffResponse;
    private ReviewStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime responseDate;

    /**
     * Format thời gian hiển thị
     */
    public String getFormattedDate() {
        if (createdAt == null) return "";

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return createdAt.format(formatter);
    }

    /**
     * Lấy avatar URL, nếu không có thì dùng avatar mặc định
     */
    public String getAvatarUrl() {
        if (customerAvatar != null && !customerAvatar.isEmpty()) {
            return customerAvatar;
        }

        // Generate avatar từ tên
        String name = customerName != null ? customerName : "User";
        return "https://ui-avatars.com/api/?name=" + name +
                "&background=c9a96e&color=fff&size=80&bold=true";
    }
}