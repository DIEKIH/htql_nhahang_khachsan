package com.example.htql_nhahang_khachsan.service;

import com.example.htql_nhahang_khachsan.dto.MenuCategoryResponse;
import com.example.htql_nhahang_khachsan.dto.MenuItemResponse;
import com.example.htql_nhahang_khachsan.entity.MenuCategoryEntity;
import com.example.htql_nhahang_khachsan.entity.MenuItemEntity;
import com.example.htql_nhahang_khachsan.entity.MenuItemImageEntity;
import com.example.htql_nhahang_khachsan.enums.PromotionApplicability;
import com.example.htql_nhahang_khachsan.enums.Status;
import com.example.htql_nhahang_khachsan.repository.MenuCategoryRepository;
import com.example.htql_nhahang_khachsan.repository.MenuItemImageRepository;
import com.example.htql_nhahang_khachsan.repository.MenuItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MenuService {

    private final MenuCategoryRepository categoryRepository;
    private final MenuItemRepository menuItemRepository;
    private final MenuItemImageRepository menuItemImageRepository;
    private final PromotionService promotionService;

    public List<MenuCategoryResponse> getCategoriesByBranch(Long branchId) {
        List<MenuCategoryEntity> categories = categoryRepository.findByBranchIdAndStatusOrderByDisplayOrder(
                branchId, Status.ACTIVE);

        return categories.stream().map(category -> {
            long itemCount = menuItemRepository.countByCategoryIdAndStatus(category.getId(), Status.ACTIVE);

            return MenuCategoryResponse.builder()
                    .id(category.getId())
                    .branchId(category.getBranch().getId())
                    .branchName(category.getBranch().getName())
                    .name(category.getName())
                    .description(category.getDescription())
                    .displayOrder(category.getDisplayOrder())
                    .status(category.getStatus())
                    .createdAt(category.getCreatedAt())
                    .menuItemCount(itemCount)
                    .build();
        }).collect(Collectors.toList());
    }

//    public List<MenuItemResponse> getMenuItemsByBranchWithPromotions(Long branchId) {
//        List<MenuItemEntity> menuItems = menuItemRepository.findByCategoryBranchIdAndStatusAndIsAvailable(
//                branchId, Status.ACTIVE, true);
//
//        return menuItems.stream().map(item -> {
//            MenuItemResponse response = MenuItemResponse.builder()
//                    .id(item.getId())
//                    .categoryId(item.getCategory().getId())
//                    .categoryName(item.getCategory().getName())
//                    .name(item.getName())
//                    .description(item.getDescription())
//                    .price(item.getPrice())
//                    .imageUrl(item.getImageUrl())
//                    .preparationTime(item.getPreparationTime())
//                    .isAvailable(item.getIsAvailable())
//                    .ingredients(item.getIngredients())
//                    .allergens(item.getAllergens())
//                    .calories(item.getCalories())
//                    .status(item.getStatus())
//                    .createdAt(item.getCreatedAt())
//                    .build();
//
//            // Lấy ảnh của món ăn
//            List<MenuItemImageEntity> images = menuItemImageRepository.findByMenuItemIdOrderByIsPrimaryDesc(item.getId());
//            response.setMenuItemImages(images);
//
//            // Tính giá sau giảm giá
//            BigDecimal originalPrice = item.getPrice();
//            BigDecimal discountedPrice = promotionService.calculateDiscountedPrice(
//                    originalPrice, branchId, PromotionApplicability.RESTAURANT);
//
//            response.setPrice(discountedPrice);
//            response.setFormattedPrice(formatPrice(discountedPrice));
//
//            return response;
//        }).collect(Collectors.toList());
//    }

    public List<MenuItemResponse> getMenuItemsByBranchWithPromotions(Long branchId) {
        List<MenuItemEntity> menuItems = menuItemRepository.findByCategoryBranchIdAndStatusAndIsAvailable(
                branchId, Status.ACTIVE, true);

        return menuItems.stream().map(item -> {
            BigDecimal originalPrice = item.getPrice();
            BigDecimal discountedPrice = promotionService.calculateDiscountedPrice(
                    originalPrice, branchId, PromotionApplicability.RESTAURANT
            );

            MenuItemResponse response = MenuItemResponse.builder()
                    .id(item.getId())
                    .categoryId(item.getCategory().getId())
                    .categoryName(item.getCategory().getName())
                    .name(item.getName())
                    .description(item.getDescription())
                    .price(originalPrice)              // Giá gốc trong DB
                    .originalPrice(originalPrice)      // Hiển thị khi có KM
                    .currentPrice(discountedPrice)     // Giá sau giảm
                    .formattedPrice(formatPrice(discountedPrice))
                    .imageUrl(item.getImageUrl())
                    .preparationTime(item.getPreparationTime())
                    .isAvailable(item.getIsAvailable())
                    .ingredients(item.getIngredients())
                    .allergens(item.getAllergens())
                    .calories(item.getCalories())
                    .status(item.getStatus())
                    .createdAt(item.getCreatedAt())
                    .build();

            // Lấy ảnh phụ
            List<MenuItemImageEntity> images = menuItemImageRepository.findByMenuItemIdOrderByIsPrimaryDesc(item.getId());
            response.setMenuItemImages(images);

            return response;
        }).collect(Collectors.toList());
    }


    public List<MenuItemResponse> getMenuItemsByCategory(Long categoryId) {
        List<MenuItemEntity> menuItems = menuItemRepository.findByCategoryIdAndStatusAndIsAvailable(
                categoryId, Status.ACTIVE, true);

        return menuItems.stream().map(item -> {
            MenuItemResponse response = MenuItemResponse.builder()
                    .id(item.getId())
                    .categoryId(item.getCategory().getId())
                    .categoryName(item.getCategory().getName())
                    .name(item.getName())
                    .description(item.getDescription())
                    .price(item.getPrice())
                    .imageUrl(item.getImageUrl())
                    .preparationTime(item.getPreparationTime())
                    .isAvailable(item.getIsAvailable())
                    .ingredients(item.getIngredients())
                    .allergens(item.getAllergens())
                    .calories(item.getCalories())
                    .status(item.getStatus())
                    .createdAt(item.getCreatedAt())
                    .build();

            // Lấy ảnh của món ăn
            List<MenuItemImageEntity> images = menuItemImageRepository.findByMenuItemIdOrderByIsPrimaryDesc(item.getId());
            response.setMenuItemImages(images);

            // Tính giá sau giảm giá
            Long branchId = item.getCategory().getBranch().getId();
            BigDecimal originalPrice = item.getPrice();
            BigDecimal discountedPrice = promotionService.calculateDiscountedPrice(
                    originalPrice, branchId, PromotionApplicability.RESTAURANT);

            response.setPrice(discountedPrice);
            response.setFormattedPrice(formatPrice(discountedPrice));

            return response;
        }).collect(Collectors.toList());
    }

    private String formatPrice(BigDecimal price) {
        DecimalFormat formatter = new DecimalFormat("#,###");
        return formatter.format(price) + " VNĐ";
    }


    // Thêm các methods này vào MenuService.java

    public MenuItemResponse getMenuItemById(Long id) {
        MenuItemEntity menuItem = menuItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy món ăn"));

        MenuItemResponse response = MenuItemResponse.builder()
                .id(menuItem.getId())
                .categoryId(menuItem.getCategory().getId())
                .categoryName(menuItem.getCategory().getName())
                .name(menuItem.getName())
                .description(menuItem.getDescription())
                .price(menuItem.getPrice())
                .imageUrl(menuItem.getImageUrl())
                .preparationTime(menuItem.getPreparationTime())
                .isAvailable(menuItem.getIsAvailable())
                .ingredients(menuItem.getIngredients())
                .allergens(menuItem.getAllergens())
                .calories(menuItem.getCalories())
                .status(menuItem.getStatus())
                .createdAt(menuItem.getCreatedAt())
                .build();

        // Lấy ảnh của món ăn
        List<MenuItemImageEntity> images = menuItemImageRepository.findByMenuItemIdOrderByIsPrimaryDesc(id);
        response.setMenuItemImages(images);

        // Tính giá sau giảm giá
        Long branchId = menuItem.getCategory().getBranch().getId();
        BigDecimal originalPrice = menuItem.getPrice();
        BigDecimal discountedPrice = promotionService.calculateDiscountedPrice(
                originalPrice, branchId, PromotionApplicability.RESTAURANT);

        response.setPrice(discountedPrice);
        response.setFormattedPrice(formatPrice(discountedPrice));

        return response;
    }

    public List<MenuItemImageEntity> getMenuItemImages(Long menuItemId) {
        return menuItemImageRepository.findByMenuItemIdOrderByIsPrimaryDesc(menuItemId);
    }

    public Map<String, Object> getMenuItemImages(Long menuItemId, String fallbackImageUrl) {
        // Tìm ảnh chính từ bảng menu_item_images
        String mainImage = menuItemImageRepository.findByMenuItemIdAndIsPrimaryTrue(menuItemId)
                .stream()
                .findFirst()
                .map(MenuItemImageEntity::getImageUrl)
                .orElse(fallbackImageUrl); // fallback sang menu_items.image_url

        // Lấy tất cả ảnh, loại bỏ ảnh chính
        List<MenuItemImageEntity> thumbnails = menuItemImageRepository
                .findByMenuItemIdOrderByPrimaryAndUploadDate(menuItemId)
                .stream()
                .filter(img -> !img.getImageUrl().equals(mainImage))
                .toList();

        Map<String, Object> result = new HashMap<>();
        result.put("mainImage", mainImage);
        result.put("thumbnails", thumbnails);
        return result;
    }

    public List<MenuItemResponse> getSimilarMenuItems(Long categoryId, Long excludeId, int limit) {
        List<MenuItemEntity> menuItems = menuItemRepository.findByCategoryIdAndStatusAndIsAvailable(
                        categoryId, Status.ACTIVE, true)
                .stream()
                .filter(item -> !item.getId().equals(excludeId))
                .limit(limit)
                .collect(Collectors.toList());

        return menuItems.stream().map(item -> {
            MenuItemResponse response = MenuItemResponse.builder()
                    .id(item.getId())
                    .categoryId(item.getCategory().getId())
                    .categoryName(item.getCategory().getName())
                    .name(item.getName())
                    .description(item.getDescription())
                    .price(item.getPrice())
                    .imageUrl(item.getImageUrl())
                    .preparationTime(item.getPreparationTime())
                    .isAvailable(item.getIsAvailable())
                    .ingredients(item.getIngredients())
                    .allergens(item.getAllergens())
                    .calories(item.getCalories())
                    .status(item.getStatus())
                    .createdAt(item.getCreatedAt())
                    .build();

            // Tính giá sau giảm giá
            Long branchId = item.getCategory().getBranch().getId();
            BigDecimal originalPrice = item.getPrice();
            BigDecimal discountedPrice = promotionService.calculateDiscountedPrice(
                    originalPrice, branchId, PromotionApplicability.RESTAURANT);

            response.setPrice(discountedPrice);
            response.setFormattedPrice(formatPrice(discountedPrice));

            return response;
        }).collect(Collectors.toList());
    }

    public Long getBranchIdFromMenuItem(Long menuItemId) {
        MenuItemEntity menuItem = menuItemRepository.findById(menuItemId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy món ăn"));
        return menuItem.getCategory().getBranch().getId();
    }
}