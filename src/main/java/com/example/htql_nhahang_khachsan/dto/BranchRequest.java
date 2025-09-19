package com.example.htql_nhahang_khachsan.dto;

import com.example.htql_nhahang_khachsan.enums.BranchStatus;
import com.example.htql_nhahang_khachsan.enums.BranchType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BranchRequest {
    @NotBlank(message = "Tên chi nhánh không được để trống")
    @Size(min = 2, max = 100, message = "Tên chi nhánh phải từ 2-100 ký tự")
    private String name;

    private String description;

    @NotBlank(message = "Địa chỉ không được để trống")
    @Size(max = 255, message = "Địa chỉ không được vượt quá 255 ký tự")
    private String address;

    @NotBlank(message = "Vui lòng nhập số nhà, đường")
    private String streetAddress;

    @NotBlank(message = "Vui lòng nhập quận/huyện")
    private String district;

    @NotBlank(message = "Vui lòng nhập tỉnh/thành phố")
    private String province;

    @Pattern(regexp = "^[0-9+\\-\\s()]*$", message = "Số điện thoại không hợp lệ")
    private String phoneNumber;

    @Email(message = "Email không hợp lệ")
    private String email;

    @NotNull(message = "Loại chi nhánh không được để trống")
    private BranchType type;

    @NotNull(message = "Trạng thái không được để trống")
    private BranchStatus status;

    private String imageUrl;

    private Double latitude;
    private Double longitude;
}
