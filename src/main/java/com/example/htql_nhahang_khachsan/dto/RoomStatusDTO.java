package com.example.htql_nhahang_khachsan.dto;


import com.example.htql_nhahang_khachsan.enums.RoomStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomStatusDTO {
    private Long roomId;
    private String roomNumber;
    private String roomType;
    private Integer floor;
    private RoomStatus status;
    private String bookingCode;
    private String guestName;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;

    public boolean isAvailable() {
        return status == RoomStatus.AVAILABLE;
    }

    public boolean isOccupied() {
        return status == RoomStatus.OCCUPIED;
    }

    public boolean isCleaning() {
        return status == RoomStatus.CLEANING;
    }

    public boolean isMaintenance() {
        return status == RoomStatus.MAINTENANCE;
    }
}
