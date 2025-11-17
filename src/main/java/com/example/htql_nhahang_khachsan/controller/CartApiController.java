package com.example.htql_nhahang_khachsan.controller;

import com.example.htql_nhahang_khachsan.dto.AddToCartRequest;
import com.example.htql_nhahang_khachsan.service.CartService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Slf4j
public class CartApiController {

    private final CartService cartService;

    /**
     * ✅ API thêm món vào giỏ hàng nhanh từ chatbot
     */
    @PostMapping("/quick-add")
    public Map<String, Object> quickAddToCart(@RequestBody Map<String, Object> request,
                                              HttpSession session) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Kiểm tra đăng nhập
            Long userId = (Long) session.getAttribute("userId");

            if (userId == null) {
                response.put("success", false);
                response.put("message", "Vui lòng đăng nhập để thêm vào giỏ hàng");
                return response;
            }

            // Parse request
            Long menuItemId = Long.valueOf(request.get("menuItemId").toString());
            Integer quantity = Integer.valueOf(request.getOrDefault("quantity", 1).toString());

            log.info("Quick add to cart - User: {}, Item: {}, Quantity: {}",
                    userId, menuItemId, quantity);

            // Tạo AddToCartRequest
            AddToCartRequest addRequest = AddToCartRequest.builder()
                    .menuItemId(menuItemId)
                    .quantity(quantity)
                    .isTakeaway(false)
                    .build();

            // Thêm vào giỏ
            cartService.addToCart(userId, addRequest);

            // Lấy số lượng món trong giỏ
            Integer cartCount = cartService.getCartItemCount(userId);

            response.put("success", true);
            response.put("message", "Đã thêm vào giỏ hàng");
            response.put("cartCount", cartCount);

            log.info("✅ Added to cart successfully");

        } catch (Exception e) {
            log.error("Error adding to cart from chatbot", e);
            response.put("success", false);
            response.put("message", e.getMessage());
        }

        return response;
    }

    /**
     * ✅ API lấy tóm tắt giỏ hàng cho chatbot
     */
    @GetMapping("/summary")
    public Map<String, Object> getCartSummaryForChat(HttpSession session) {
        Map<String, Object> response = new HashMap<>();

        try {
            Long userId = (Long) session.getAttribute("userId");

            if (userId == null) {
                response.put("success", false);
                response.put("message", "Vui lòng đăng nhập");
                return response;
            }

            var cart = cartService.getCartSummary(userId);

            response.put("success", true);
            response.put("totalItems", cart.getTotalItems());
            response.put("totalAmount", cart.getFormattedTotalAmount());
            response.put("isEmpty", cart.getItems().isEmpty());

        } catch (Exception e) {
            log.error("Error getting cart summary", e);
            response.put("success", false);
            response.put("message", e.getMessage());
        }

        return response;
    }
}