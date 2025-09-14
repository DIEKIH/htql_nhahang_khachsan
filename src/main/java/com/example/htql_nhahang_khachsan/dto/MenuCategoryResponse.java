package com.example.htql_nhahang_khachsan.dto;


import com.example.htql_nhahang_khachsan.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuCategoryResponse {

    private Long id;
    private Long branchId;
    private String branchName;
    private String name;
    private String description;
    private Integer displayOrder;
    private Status status;
    private LocalDateTime createdAt;

    // Các field bổ sung cho hiển thị
    private long menuItemCount; // Số lượng menu item trong category này
    private String statusText; // Text hiển thị cho status

    public String getStatusText() {
        if (status == null) return "";
        return switch (status) {
            case ACTIVE -> "Hoạt động";
            case INACTIVE -> "Không hoạt động";
            default -> status.name();
        };
    }

    public String getStatusClass() {
        if (status == null) return "secondary";
        return switch (status) {
            case ACTIVE -> "success";
            case INACTIVE -> "secondary";
            default -> "secondary";
        };
    }
}
