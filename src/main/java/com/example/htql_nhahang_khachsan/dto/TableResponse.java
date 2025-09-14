package com.example.htql_nhahang_khachsan.dto;



import com.example.htql_nhahang_khachsan.enums.TableStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TableResponse {

    private Long id;
    private Long branchId;
    private String branchName;
    private String tableNumber;
    private Integer capacity;
    private TableStatus status;
    private String statusDisplayName;
    private Integer positionX;
    private Integer positionY;
    private String notes;
    private LocalDateTime createdAt;

    public String getStatusDisplayName() {
        return status != null ? status.getDisplayName() : "";
    }
}