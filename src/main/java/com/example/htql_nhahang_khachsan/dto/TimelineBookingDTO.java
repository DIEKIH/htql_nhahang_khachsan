package com.example.htql_nhahang_khachsan.dto;

import com.example.htql_nhahang_khachsan.enums.BookingStatus;
import com.example.htql_nhahang_khachsan.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimelineBookingDTO {
    private Long id;
    private String bookingCode;
    private String guestName;
    private Long roomId;
    private String roomNumber;
    private Long roomTypeId;
    private String roomTypeName;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private BookingStatus status;
    private PaymentStatus paymentStatus;
}
