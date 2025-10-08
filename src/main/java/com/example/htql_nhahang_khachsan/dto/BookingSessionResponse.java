package com.example.htql_nhahang_khachsan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingSessionResponse {
    private String sessionId;
    private RoomTypeResponse roomType;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer numberOfNights;
    private Integer adults;
    private Integer children;
    private Integer numberOfRooms;

    // Giá
    private BigDecimal roomPrice;
    private BigDecimal totalRoomPrice;
    private BigDecimal breakfastFee;
    private BigDecimal spaFee;
    private BigDecimal airportTransferFee;
    private BigDecimal serviceFee;
    private BigDecimal vat;
    private BigDecimal totalAmount;
    private BigDecimal depositAmount;
    private BigDecimal remainingAmount;

    // Dịch vụ
    private Boolean includeBreakfast;
    private Boolean includeSpa;
    private Boolean includeAirportTransfer;

    // Thông tin khách (bước 2)
    private String guestName;
    private String guestEmail;
    private String guestPhone;
    private String guestIdNumber;
    private String specialRequests;

    // Trạng thái
    private String currentStep; // STEP_1, STEP_2, STEP_3, COMPLETED
    private Integer availableRooms;
}

