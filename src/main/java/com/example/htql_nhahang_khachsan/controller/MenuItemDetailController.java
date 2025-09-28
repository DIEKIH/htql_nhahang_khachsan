package com.example.htql_nhahang_khachsan.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.example.htql_nhahang_khachsan.dto.*;
import com.example.htql_nhahang_khachsan.entity.MenuItemEntity;
import com.example.htql_nhahang_khachsan.entity.MenuItemImageEntity;
import com.example.htql_nhahang_khachsan.enums.Status;
import com.example.htql_nhahang_khachsan.enums.PromotionApplicability;
import com.example.htql_nhahang_khachsan.service.*;
import com.example.htql_nhahang_khachsan.repository.MenuItemRepository;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class MenuItemDetailController {

    private final MenuService menuService;
    private final PromotionService promotionService;
    private final BranchService branchService;
    private final MenuItemRepository menuItemRepository;

    @GetMapping("/menu-items/{id}")
    public String showMenuItemDetail(@PathVariable Long id, Model model) {
        try {
            Optional<MenuItemEntity> menuItemOpt = menuItemRepository.findById(id);

            if (menuItemOpt.isEmpty() || menuItemOpt.get().getStatus() != Status.ACTIVE) {
                return "redirect:/";
            }

            MenuItemEntity menuItemEntity = menuItemOpt.get();

            // Lấy thông tin chi nhánh từ category
            Long branchId = menuItemEntity.getCategory().getBranch().getId();
            BranchResponse branch = branchService.getBranchById(branchId);

            // Tạo MenuItemResponse với thông tin đầy đủ
            MenuItemResponse menuItem = buildMenuItemResponse(menuItemEntity, branchId);

            // Lấy món ăn tương tự trong cùng danh mục (trừ món hiện tại)
            List<MenuItemResponse> similarItems = menuService.getSimilarMenuItems(
                    menuItemEntity.getCategory().getId(), id, 3);

            // Lấy khuyến mãi áp dụng cho chi nhánh
            List<PromotionResponse> promotions = promotionService.getActivePromotionsByBranch(branchId)
                    .stream()
                    .filter(p -> p.getApplicability() == PromotionApplicability.RESTAURANT ||
                            p.getApplicability() == PromotionApplicability.BOTH)
                    .toList();

            // Tính giá gốc và giá sau giảm
            BigDecimal originalPrice = menuItemEntity.getPrice();
            BigDecimal discountedPrice = promotionService.calculateDiscountedPrice(
                    originalPrice, branchId, PromotionApplicability.RESTAURANT);

            boolean hasDiscount = originalPrice.compareTo(discountedPrice) > 0;

            // Ảnh chính từ entity
//            String mainImage = menuItemEntity.getImageUrl();

            // Ảnh phụ: loại bỏ ảnh chính
            Map<String, Object> imageData = menuService.getMenuItemImages(id, menuItemEntity.getImageUrl());
            String mainImage = (String) imageData.get("mainImage");
            List<MenuItemImageEntity> thumbnails = (List<MenuItemImageEntity>) imageData.get("thumbnails");

            // Add attributes to model
            model.addAttribute("menuItem", menuItem);
            model.addAttribute("branch", branch);
            model.addAttribute("mainImage", mainImage);
            model.addAttribute("thumbnails", thumbnails);
            model.addAttribute("similarItems", similarItems);
            model.addAttribute("promotions", promotions);
            model.addAttribute("originalPrice", originalPrice);
            model.addAttribute("discountedPrice", discountedPrice);
            model.addAttribute("hasDiscount", hasDiscount);
            model.addAttribute("formattedOriginalPrice", formatPrice(originalPrice));
            model.addAttribute("formattedDiscountedPrice", formatPrice(discountedPrice));

            return "customer/menu/detail";

        } catch (Exception e) {
            e.printStackTrace();
            return "redirect:/branches/" + getBranchIdFromMenuItemId(id);
        }
    }


    @GetMapping("/api/menu-items/{id}")
    @ResponseBody
    public MenuItemResponse getMenuItemDetail(@PathVariable Long id) {
        try {
            Optional<MenuItemEntity> menuItemOpt = menuItemRepository.findById(id);
            if (menuItemOpt.isEmpty()) {
                return null;
            }

            MenuItemEntity menuItem = menuItemOpt.get();
            Long branchId = menuItem.getCategory().getBranch().getId();

            return buildMenuItemResponse(menuItem, branchId);
        } catch (Exception e) {
            return null;
        }
    }

    @GetMapping("/api/menu-items/{id}/similar")
    @ResponseBody
    public List<MenuItemResponse> getSimilarMenuItems(@PathVariable Long id,
                                                      @RequestParam(defaultValue = "3") int limit) {
        try {
            Optional<MenuItemEntity> menuItemOpt = menuItemRepository.findById(id);
            if (menuItemOpt.isEmpty()) {
                return List.of();
            }

            Long categoryId = menuItemOpt.get().getCategory().getId();
            return menuService.getSimilarMenuItems(categoryId, id, limit);
        } catch (Exception e) {
            return List.of();
        }
    }

    private MenuItemResponse buildMenuItemResponse(MenuItemEntity menuItem, Long branchId) {
        // Tính giá sau giảm giá
        BigDecimal originalPrice = menuItem.getPrice();
        BigDecimal discountedPrice = promotionService.calculateDiscountedPrice(
                originalPrice, branchId, PromotionApplicability.RESTAURANT);

        MenuItemResponse response = MenuItemResponse.builder()
                .id(menuItem.getId())
                .categoryId(menuItem.getCategory().getId())
                .categoryName(menuItem.getCategory().getName())
                .name(menuItem.getName())
                .description(menuItem.getDescription())
                .price(discountedPrice)
                .imageUrl(menuItem.getImageUrl())
                .preparationTime(menuItem.getPreparationTime())
                .isAvailable(menuItem.getIsAvailable())
                .ingredients(menuItem.getIngredients())
                .allergens(menuItem.getAllergens())
                .calories(menuItem.getCalories())
                .status(menuItem.getStatus())
                .createdAt(menuItem.getCreatedAt())
                .formattedPrice(formatPrice(discountedPrice))
                .build();

        // Lấy ảnh của món ăn
        List<MenuItemImageEntity> images = menuService.getMenuItemImages(menuItem.getId());
        response.setMenuItemImages(images);

        return response;
    }

    private Long getBranchIdFromMenuItemId(Long menuItemId) {
        try {
            Optional<MenuItemEntity> menuItemOpt = menuItemRepository.findById(menuItemId);
            if (menuItemOpt.isPresent()) {
                return menuItemOpt.get().getCategory().getBranch().getId();
            }
        } catch (Exception e) {
            // Log error
        }
        return 1L; // Default fallback
    }

    private String formatPrice(BigDecimal price) {
        DecimalFormat formatter = new DecimalFormat("#,###");
        return formatter.format(price) + " VNĐ";
    }
}