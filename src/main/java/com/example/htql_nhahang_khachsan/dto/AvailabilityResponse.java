package com.example.htql_nhahang_khachsan.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.List;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilityResponse {
    private boolean available;
    private int availableRooms;
    private String message;
    private List<LocalDate> unavailableDates;
}
