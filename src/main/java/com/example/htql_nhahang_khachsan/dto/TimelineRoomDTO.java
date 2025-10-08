package com.example.htql_nhahang_khachsan.dto;

import com.example.htql_nhahang_khachsan.enums.RoomStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimelineRoomDTO {
    private Long id;
    private String roomNumber;
    private Integer floor;
    private Long roomTypeId;
    private String roomTypeName;
    private RoomStatus status;
}
