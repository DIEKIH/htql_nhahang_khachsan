package com.example.htql_nhahang_khachsan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableReservationResponse {
    private Long reservationId;
    private String reservationCode;
    private TableResponse table;
    private String customerName;
    private LocalDateTime reservationTime;
    private String status;
    private LocalDateTime createdAt;
}
