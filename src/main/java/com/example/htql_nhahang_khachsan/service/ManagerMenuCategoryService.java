package com.example.htql_nhahang_khachsan.service;

import com.example.htql_nhahang_khachsan.dto.MenuCategoryRequest;
import com.example.htql_nhahang_khachsan.dto.MenuCategoryResponse;
import com.example.htql_nhahang_khachsan.entity.BranchEntity;
import com.example.htql_nhahang_khachsan.entity.MenuCategoryEntity;
import com.example.htql_nhahang_khachsan.enums.Status;
import com.example.htql_nhahang_khachsan.repository.BranchRepository;
import com.example.htql_nhahang_khachsan.repository.MenuCategoryRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;


@Service
@Transactional
public class ManagerMenuCategoryService {
    private final MenuCategoryRepository menuCategoryRepository;
    private final BranchRepository branchRepository;

    public ManagerMenuCategoryService(MenuCategoryRepository menuCategoryRepository,
                                      BranchRepository branchRepository) {
        this.menuCategoryRepository = menuCategoryRepository;
        this.branchRepository = branchRepository;
    }

    public List<MenuCategoryResponse> getAllMenuCategoriesByBranch(Long branchId) {
        List<MenuCategoryEntity> menuCategories = menuCategoryRepository.findByBranchIdOrderByDisplayOrderAscCreatedAtDesc(branchId);
        return menuCategories.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    private MenuCategoryResponse convertToResponse(MenuCategoryEntity entity) {
        return MenuCategoryResponse.builder()
                .id(entity.getId())
                .branchId(entity.getBranch().getId())
                .branchName(entity.getBranch().getName())
                .name(entity.getName())
                .description(entity.getDescription())
                .displayOrder(entity.getDisplayOrder())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public List<MenuCategoryResponse> searchMenuCategories(Long branchId, String name, Status status) {
        List<MenuCategoryEntity> menuCategories;

        if (name != null && status != null) {
            menuCategories = menuCategoryRepository.findByBranchIdAndNameContainingIgnoreCaseAndStatusOrderByDisplayOrderAscCreatedAtDesc(
                    branchId, name, status);
        } else if (name != null) {
            menuCategories = menuCategoryRepository.findByBranchIdAndNameContainingIgnoreCaseOrderByDisplayOrderAscCreatedAtDesc(
                    branchId, name);
        } else if (status != null) {
            menuCategories = menuCategoryRepository.findByBranchIdAndStatusOrderByDisplayOrderAscCreatedAtDesc(
                    branchId, status);
        } else {
            menuCategories = menuCategoryRepository.findByBranchIdOrderByDisplayOrderAscCreatedAtDesc(branchId);
        }

        return menuCategories.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public MenuCategoryResponse getMenuCategoryById(Long id, Long branchId) {
        MenuCategoryEntity menuCategory = menuCategoryRepository.findByIdAndBranchId(id, branchId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy loại menu"));
        return convertToResponse(menuCategory);
    }

    public MenuCategoryResponse createMenuCategory(Long branchId, MenuCategoryRequest request) {
        BranchEntity branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chi nhánh"));

        // Kiểm tra trùng tên trong cùng chi nhánh
        if (menuCategoryRepository.existsByBranchIdAndNameIgnoreCase(branchId, request.getName())) {
            throw new RuntimeException("Tên loại menu đã tồn tại trong chi nhánh này");
        }

        // Tự động set display order nếu không có
        if (request.getDisplayOrder() == null) {
            Integer maxOrder = menuCategoryRepository.findMaxDisplayOrderByBranchId(branchId);
            request.setDisplayOrder(maxOrder != null ? maxOrder + 1 : 1);
        }

        MenuCategoryEntity menuCategory = MenuCategoryEntity.builder()
                .branch(branch)
                .name(request.getName())
                .description(request.getDescription())
                .displayOrder(request.getDisplayOrder())
                .status(request.getStatus())
                .build();

        menuCategory = menuCategoryRepository.save(menuCategory);
        return convertToResponse(menuCategory);
    }

    public MenuCategoryResponse updateMenuCategory(Long id, Long branchId, MenuCategoryRequest request) {
        MenuCategoryEntity menuCategory = menuCategoryRepository.findByIdAndBranchId(id, branchId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy loại menu"));

        // Kiểm tra trùng tên (trừ chính nó)
        if (menuCategoryRepository.existsByBranchIdAndNameIgnoreCaseAndIdNot(
                branchId, request.getName(), id)) {
            throw new RuntimeException("Tên loại menu đã tồn tại trong chi nhánh này");
        }

        menuCategory.setName(request.getName());
        menuCategory.setDescription(request.getDescription());
        menuCategory.setDisplayOrder(request.getDisplayOrder());
        menuCategory.setStatus(request.getStatus());

        menuCategory = menuCategoryRepository.save(menuCategory);
        return convertToResponse(menuCategory);
    }

    public void deleteMenuCategory(Long id, Long branchId) {
        MenuCategoryEntity menuCategory = menuCategoryRepository.findByIdAndBranchId(id, branchId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy loại menu"));

        // Kiểm tra xem có menu item nào đang sử dụng category này không
        // TODO: Implement check for existing menu items

        menuCategory.setStatus(Status.INACTIVE);
        menuCategoryRepository.save(menuCategory);
    }

    public List<MenuCategoryResponse> getAllActiveMenuCategoriesByBranch(Long branchId) {
        List<MenuCategoryEntity> menuCategories = menuCategoryRepository.findByBranchIdAndStatusOrderByDisplayOrderAsc(
                branchId, Status.ACTIVE);
        return menuCategories.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public void updateDisplayOrder(Long branchId, List<Long> categoryIds) {
        for (int i = 0; i < categoryIds.size(); i++) {
            Long categoryId = categoryIds.get(i);
            MenuCategoryEntity category = menuCategoryRepository.findByIdAndBranchId(categoryId, branchId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy loại menu với ID: " + categoryId));

            category.setDisplayOrder(i + 1);
            menuCategoryRepository.save(category);
        }
    }
}