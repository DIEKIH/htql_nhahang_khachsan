package com.example.htql_nhahang_khachsan.controller;

import com.example.htql_nhahang_khachsan.dto.*;
import com.example.htql_nhahang_khachsan.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    @PostMapping("/cart/add")
    public String addToCart(@ModelAttribute AddToCartRequest request,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");

        if (userId == null) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập để thêm vào giỏ hàng");
            return "redirect:/customer/login";
        }

        try {
            cartService.addToCart(userId, request);
            redirectAttributes.addFlashAttribute("success", "Đã thêm món vào giỏ hàng");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/menu-items/" + request.getMenuItemId();
    }


    @GetMapping("/cart")
    public String viewCart(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");

        if (userId == null) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/customer/login";
        }

        CartSummaryResponse cart = cartService.getCartSummary(userId);
        model.addAttribute("cart", cart);

        return "customer/cart/view";
    }

    @PostMapping("/cart/update")
    @ResponseBody
    public Map<String, Object> updateCartItem(@RequestBody UpdateCartItemRequest request,
                                              HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        Long userId = (Long) session.getAttribute("userId");

        if (userId == null) {
            response.put("success", false);
            response.put("message", "Vui lòng đăng nhập");
            return response;
        }

        try {
            cartService.updateCartItem(userId, request);
            CartSummaryResponse cart = cartService.getCartSummary(userId);

            response.put("success", true);
            response.put("cart", cart);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
        }

        return response;
    }

    @PostMapping("/cart/remove/{itemId}")
    public String removeCartItem(@PathVariable Long itemId,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");

        if (userId == null) {
            return "redirect:/customer/login";
        }

        try {
            cartService.removeCartItem(userId, itemId);
            redirectAttributes.addFlashAttribute("success", "Đã xóa món khỏi giỏ hàng");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/cart";
    }

    @PostMapping("/cart/clear")
    public String clearCart(HttpSession session, RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("userId");

        if (userId == null) {
            return "redirect:/customer/login";
        }

        try {
            cartService.clearCart(userId);
            redirectAttributes.addFlashAttribute("success", "Đã xóa toàn bộ giỏ hàng");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/cart";
    }

    @GetMapping("/api/cart/count")
    @ResponseBody
    public Map<String, Object> getCartCount(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        Long userId = (Long) session.getAttribute("userId");

        if (userId != null) {
            Integer count = cartService.getCartItemCount(userId);
            response.put("count", count);
        } else {
            response.put("count", 0);
        }

        return response;
    }
}