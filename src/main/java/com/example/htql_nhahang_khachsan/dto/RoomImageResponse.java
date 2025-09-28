package com.example.htql_nhahang_khachsan.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomImageResponse {
    private Long id;
    private Long roomTypeId;
    private String imageUrl;
    private String imageTitle;
    private Boolean isPrimary;
    private Integer displayOrder;
    private LocalDateTime uploadedAt;
}
