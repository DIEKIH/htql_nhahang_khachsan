package com.example.htql_nhahang_khachsan.dto;

import com.example.htql_nhahang_khachsan.enums.PaymentMethod;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.time.LocalDate;



@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalkinBookingRequest {
    @NotNull(message = "Loại phòng không được để trống")
    private Long roomTypeId;

    @NotNull(message = "Ngày check-in không được để trống")
    @FutureOrPresent(message = "Ngày check-in phải từ hôm nay trở đi")
    private LocalDate checkInDate;

    @NotNull(message = "Ngày check-out không được để trống")
    @Future(message = "Ngày check-out phải là ngày trong tương lai")
    private LocalDate checkOutDate;

    @NotNull(message = "Số người lớn không được để trống")
    @Min(value = 1, message = "Phải có ít nhất 1 người lớn")
    private Integer adults;

    @Min(value = 0, message = "Số trẻ em không được âm")
    private Integer children = 0;

    @NotBlank(message = "Tên khách hàng không được để trống")
    private String guestName;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String guestEmail;

    @NotBlank(message = "Số điện thoại không được để trống")
    private String guestPhone;

    private String guestIdNumber;
    private String specialRequests;
    private PaymentMethod paymentMethod = PaymentMethod.CASH;
}
