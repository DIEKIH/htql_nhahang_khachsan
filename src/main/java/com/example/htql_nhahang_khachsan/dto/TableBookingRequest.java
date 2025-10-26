package com.example.htql_nhahang_khachsan.dto;

import com.example.htql_nhahang_khachsan.enums.BookingStatus;
import jakarta.validation.constraints.*;
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
public class TableBookingRequest {

    @NotBlank(message = "Vui lòng nhập tên khách hàng")
    @Size(min = 2, max = 100, message = "Tên phải từ 2-100 ký tự")
    private String customerName;

    @NotBlank(message = "Vui lòng nhập email")
    @Email(message = "Email không hợp lệ")
    private String customerEmail;

    @NotBlank(message = "Vui lòng nhập số điện thoại")
    @Pattern(regexp = "^(0|\\+84)[0-9]{9,10}$", message = "Số điện thoại không hợp lệ")
    private String contactPhone;

    @NotNull(message = "Vui lòng chọn ngày đặt bàn")
    private LocalDate bookingDate;

    @NotNull(message = "Vui lòng chọn giờ đặt bàn")
    private LocalTime bookingTime;

    @NotNull(message = "Vui lòng nhập số lượng khách")
    @Min(value = 1, message = "Số lượng khách tối thiểu là 1")
    @Max(value = 50, message = "Số lượng khách tối đa là 50")
    private Integer partySize;

    private List<Long> preferredTableIds;

    @Size(max = 500, message = "Yêu cầu đặc biệt không quá 500 ký tự")
    private String specialRequests;
}