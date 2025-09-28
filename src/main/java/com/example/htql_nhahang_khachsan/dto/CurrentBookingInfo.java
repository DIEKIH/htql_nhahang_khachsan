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
public class CurrentBookingInfo {
    private Long bookingId;
    private String guestName;
    private LocalDateTime checkIn;
    private LocalDateTime checkOut;
    private String bookingStatus;
}