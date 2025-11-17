package com.example.htql_nhahang_khachsan.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthApiController {

    /**
     * ✅ API kiểm tra trạng thái đăng nhập
     * Gọi từ chatbot JavaScript để check session
     */
    @GetMapping("/check")
    public Map<String, Object> checkLoginStatus(HttpSession session) {
        Map<String, Object> response = new HashMap<>();

        Long userId = (Long) session.getAttribute("userId");
        boolean isLoggedIn = userId != null;

        response.put("isLoggedIn", isLoggedIn);

        if (isLoggedIn) {
            response.put("userId", userId);

            // ✅ Lấy thêm thông tin user nếu cần
            String userName = (String) session.getAttribute("userName");
            if (userName != null) {
                response.put("userName", userName);
            }
        }

        return response;
    }
}