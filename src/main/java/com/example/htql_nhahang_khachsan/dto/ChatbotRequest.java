package com.example.htql_nhahang_khachsan.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotRequest {

    @NotBlank(message = "Tin nhắn không được để trống")
    @Size(max = 500, message = "Tin nhắn không được quá 500 ký tự")
    private String message;

    @Size(max = 5000, message = "Lịch sử chat không được quá 5000 ký tự")
    private String history;
}
