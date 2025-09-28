package com.example.htql_nhahang_khachsan.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TableReservationRequest {
    private String customerName;
    private String customerPhone;
    private String customerEmail;
    private LocalDateTime reservationTime;
    private Integer partySize;
    private String specialRequests;
}