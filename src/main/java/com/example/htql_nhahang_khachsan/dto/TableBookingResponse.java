package com.example.htql_nhahang_khachsan.dto;

import com.example.htql_nhahang_khachsan.enums.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TableBookingResponse {
    private Long id;
    private String bookingCode;
    private String customerName;
    private String customerEmail;
    private String contactPhone;
    private String branchName;
    private LocalDate bookingDate;
    private LocalTime bookingTime;
    private Integer partySize;
    private List<TableInfo> assignedTables;
    private BookingStatus status;
    private String statusDisplay;
    private String specialRequests;
    private LocalDateTime createdAt;
    private LocalDateTime actualArrival;
}
