package com.example.htql_nhahang_khachsan.service;

import com.example.htql_nhahang_khachsan.dto.MenuCategoryResponse;
import com.example.htql_nhahang_khachsan.dto.MenuItemRequest;
import com.example.htql_nhahang_khachsan.dto.MenuItemResponse;
import com.example.htql_nhahang_khachsan.entity.MenuCategoryEntity;
import com.example.htql_nhahang_khachsan.entity.MenuItemEntity;
import com.example.htql_nhahang_khachsan.entity.MenuItemImageEntity;
import com.example.htql_nhahang_khachsan.enums.Status;
import com.example.htql_nhahang_khachsan.repository.MenuCategoryRepository;
import com.example.htql_nhahang_khachsan.repository.MenuItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.text.DecimalFormat;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ManagerMenuItemService {
    private final MenuItemRepository menuItemRepository;
    private final MenuCategoryRepository menuCategoryRepository;
    private final ManagerMenuItemImageService menuItemImageService;

    public ManagerMenuItemService(MenuItemRepository menuItemRepository,
                                  MenuCategoryRepository menuCategoryRepository,
                                  ManagerMenuItemImageService menuItemImageService) {
        this.menuItemRepository = menuItemRepository;
        this.menuCategoryRepository = menuCategoryRepository;
        this.menuItemImageService = menuItemImageService;
    }

    public List<MenuItemResponse> getAllMenuItemsByBranch(Long branchId) {
        List<MenuItemEntity> menuItems = menuItemRepository.findByCategoryBranchIdOrderByCreatedAtDesc(branchId);
        return menuItems.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<MenuItemResponse> searchMenuItems(Long branchId, String name, Long categoryId, Status status) {
        List<MenuItemEntity> menuItems;

        if (name != null && categoryId != null && status != null) {
            menuItems = menuItemRepository.findByCategoryBranchIdAndNameContainingIgnoreCaseAndCategoryIdAndStatusOrderByCreatedAtDesc(
                    branchId, name, categoryId, status);
        } else if (name != null && categoryId != null) {
            menuItems = menuItemRepository.findByCategoryBranchIdAndNameContainingIgnoreCaseAndCategoryIdOrderByCreatedAtDesc(
                    branchId, name, categoryId);
        } else if (name != null && status != null) {
            menuItems = menuItemRepository.findByCategoryBranchIdAndNameContainingIgnoreCaseAndStatusOrderByCreatedAtDesc(
                    branchId, name, status);
        } else if (categoryId != null && status != null) {
            menuItems = menuItemRepository.findByCategoryBranchIdAndCategoryIdAndStatusOrderByCreatedAtDesc(
                    branchId, categoryId, status);
        } else if (name != null) {
            menuItems = menuItemRepository.findByCategoryBranchIdAndNameContainingIgnoreCaseOrderByCreatedAtDesc(
                    branchId, name);
        } else if (categoryId != null) {
            menuItems = menuItemRepository.findByCategoryBranchIdAndCategoryIdOrderByCreatedAtDesc(
                    branchId, categoryId);
        } else if (status != null) {
            menuItems = menuItemRepository.findByCategoryBranchIdAndStatusOrderByCreatedAtDesc(
                    branchId, status);
        } else {
            menuItems = menuItemRepository.findByCategoryBranchIdOrderByCreatedAtDesc(branchId);
        }

        return menuItems.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public MenuItemResponse getMenuItemById(Long id, Long branchId) {
        MenuItemEntity menuItem = menuItemRepository.findByIdAndCategoryBranchId(id, branchId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy món ăn"));
        return convertToResponse(menuItem);
    }

    public MenuItemResponse createMenuItem(Long branchId,
                                           MenuItemRequest request,
                                           List<MultipartFile> imageFiles) {
        MenuCategoryEntity category = menuCategoryRepository.findByIdAndBranchId(request.getCategoryId(), branchId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục"));

        // Kiểm tra trùng tên trong cùng chi nhánh
        if (menuItemRepository.existsByCategoryBranchIdAndNameIgnoreCase(branchId, request.getName())) {
            throw new RuntimeException("Tên món ăn đã tồn tại trong chi nhánh này");
        }

        MenuItemEntity menuItem = MenuItemEntity.builder()
                .category(category)
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .preparationTime(request.getPreparationTime())
                .isAvailable(request.getIsAvailable())
                .ingredients(request.getIngredients())
                .allergens(request.getAllergens())
                .calories(request.getCalories())
                .status(request.getStatus())
                .build();

        menuItem = menuItemRepository.save(menuItem);

        // Save menu item images
        if (imageFiles != null && !imageFiles.isEmpty()) {
            menuItemImageService.saveMenuItemImages(menuItem, imageFiles);
        }

        return convertToResponse(menuItem);
    }

    public MenuItemResponse updateMenuItem(Long id,
                                           Long branchId,
                                           MenuItemRequest request,
                                           List<MultipartFile> imageFiles,
                                           List<Long> deleteImageIds) {
        MenuItemEntity menuItem = menuItemRepository.findByIdAndCategoryBranchId(id, branchId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy món ăn"));

        MenuCategoryEntity category = menuCategoryRepository.findByIdAndBranchId(request.getCategoryId(), branchId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục"));

        // Kiểm tra trùng tên (trừ chính nó)
        if (menuItemRepository.existsByCategoryBranchIdAndNameIgnoreCaseAndIdNot(
                branchId, request.getName(), id)) {
            throw new RuntimeException("Tên món ăn đã tồn tại trong chi nhánh này");
        }

        menuItem.setCategory(category);
        menuItem.setName(request.getName());
        menuItem.setDescription(request.getDescription());
        menuItem.setPrice(request.getPrice());
        menuItem.setPreparationTime(request.getPreparationTime());
        menuItem.setIsAvailable(request.getIsAvailable());
        menuItem.setIngredients(request.getIngredients());
        menuItem.setAllergens(request.getAllergens());
        menuItem.setCalories(request.getCalories());
        menuItem.setStatus(request.getStatus());

        menuItem = menuItemRepository.save(menuItem);

        // Update menu item images
        menuItemImageService.updateMenuItemImages(menuItem, imageFiles, deleteImageIds);

        return convertToResponse(menuItem);
    }

    public void deleteMenuItem(Long id, Long branchId) {
        MenuItemEntity menuItem = menuItemRepository.findByIdAndCategoryBranchId(id, branchId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy món ăn"));

        // Delete all images
        menuItemImageService.deleteAllMenuItemImages(menuItem);

        menuItem.setStatus(Status.INACTIVE);
        menuItemRepository.save(menuItem);
    }

    public List<MenuCategoryResponse> getActiveMenuCategoriesByBranch(Long branchId) {
        List<MenuCategoryEntity> categories = menuCategoryRepository.findByBranchIdAndStatusOrderByDisplayOrder(
                branchId, Status.ACTIVE);

        return categories.stream()
                .map(category -> MenuCategoryResponse.builder()
                        .id(category.getId())
                        .name(category.getName())
                        .description(category.getDescription())
                        .displayOrder(category.getDisplayOrder())
                        .status(category.getStatus())
                        .build())
                .collect(Collectors.toList());
    }

    private MenuItemResponse convertToResponse(MenuItemEntity entity) {
        MenuItemResponse response = MenuItemResponse.builder()
                .id(entity.getId())
                .categoryId(entity.getCategory().getId())
                .categoryName(entity.getCategory().getName())
                .name(entity.getName())
                .description(entity.getDescription())
                .price(entity.getPrice())
                .preparationTime(entity.getPreparationTime())
                .isAvailable(entity.getIsAvailable())
                .ingredients(entity.getIngredients())
                .allergens(entity.getAllergens())
                .calories(entity.getCalories())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();

        // Format price
        if (entity.getPrice() != null) {
            DecimalFormat df = new DecimalFormat("#,###");
            response.setFormattedPrice(df.format(entity.getPrice()) + " VNĐ");
        }

        // Get menu item images
        List<MenuItemImageEntity> menuItemImages = menuItemImageService.getMenuItemImages(entity);
        List<String> imageUrlsList = menuItemImages.stream()
                .map(MenuItemImageEntity::getImageUrl)
                .collect(Collectors.toList());
        response.setImageUrlsList(imageUrlsList);
        response.setMenuItemImages(menuItemImages);

        // Set primary image as imageUrl for backward compatibility
        menuItemImages.stream()
                .filter(MenuItemImageEntity::getIsPrimary)
                .findFirst()
                .ifPresent(image -> response.setImageUrl(image.getImageUrl()));

        return response;
    }
}
