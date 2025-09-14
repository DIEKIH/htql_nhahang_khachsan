package com.example.htql_nhahang_khachsan.dto;

import com.example.htql_nhahang_khachsan.enums.RoomStatus;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomRequest {
    @NotNull(message = "Vui lòng chọn loại phòng")
    private Long roomTypeId;

    @NotBlank(message = "Số phòng không được để trống")
    @Size(max = 10, message = "Số phòng không được quá 10 ký tự")
    private String roomNumber;

    @NotNull(message = "Tầng không được để trống")
    @Min(value = 1, message = "Tầng phải lớn hơn 0")
    @Max(value = 100, message = "Tầng không được quá 100")
    private Integer floor;

    @NotNull(message = "Trạng thái không được để trống")
    private RoomStatus status;

    @Size(max = 500, message = "Ghi chú không được quá 500 ký tự")
    private String notes;
}
