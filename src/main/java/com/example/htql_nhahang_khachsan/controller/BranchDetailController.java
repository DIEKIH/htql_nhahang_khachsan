package com.example.htql_nhahang_khachsan.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.example.htql_nhahang_khachsan.dto.*;
import com.example.htql_nhahang_khachsan.enums.BranchStatus;
import com.example.htql_nhahang_khachsan.enums.BranchType;
import com.example.htql_nhahang_khachsan.service.*;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class BranchDetailController {

    private final BranchService branchService;
    private final RoomService roomService;
    private final MenuService menuService;
    private final RestaurantTableService tableService;
    private final PromotionService promotionService;

    @GetMapping("/branches/{id}")
    public String showBranchDetail(@PathVariable Long id, Model model) {
        try {
            BranchResponse branch = branchService.getBranchById(id);

            // Chỉ hiển thị chi nhánh đang hoạt động
            if (branch.getStatus() != BranchStatus.ACTIVE) {
                return "redirect:/";
            }

            model.addAttribute("branch", branch);

            // Dựa vào loại chi nhánh để load dữ liệu
            BranchType branchType = branch.getType();

            // Nếu có dịch vụ khách sạn (HOTEL hoặc BOTH)
//            if (branchType == BranchType.HOTEL || branchType == BranchType.BOTH) {
//                // Lấy danh sách loại phòng với giá sau giảm giá
//                List<RoomTypeResponse> roomTypes = roomService.getRoomTypesByBranchWithPromotions(id);
//                model.addAttribute("roomTypes", roomTypes);
//
//                // Lấy danh sách phòng với thông tin loại phòng và ảnh
//                List<RoomResponse> rooms = roomService.getRoomsByBranchWithDetails(id);
//                model.addAttribute("rooms", rooms);
//            }

            // Trong BranchDetailController.java
            // Thay đổi để truyền empty list thay vì null
            if (branchType == BranchType.HOTEL || branchType == BranchType.BOTH) {
                List<RoomTypeResponse> roomTypes = roomService.getRoomTypesByBranchWithPromotions(id);
                model.addAttribute("roomTypes", roomTypes != null ? roomTypes : new ArrayList<>());

                List<RoomResponse> rooms = roomService.getRoomsByBranchWithDetails(id);
                model.addAttribute("rooms", rooms != null ? rooms : new ArrayList<>());
            } else {
                model.addAttribute("roomTypes", new ArrayList<>());
                model.addAttribute("rooms", new ArrayList<>());
            }

            // Nếu có dịch vụ nhà hàng (RESTAURANT hoặc BOTH)
            if (branchType == BranchType.RESTAURANT || branchType == BranchType.BOTH) {
                // Lấy danh sách danh mục món ăn
                List<MenuCategoryResponse> menuCategories = menuService.getCategoriesByBranch(id);
                model.addAttribute("menuCategories", menuCategories);

                // Lấy danh sách món ăn với giá sau giảm giá
                List<MenuItemResponse> menuItems = menuService.getMenuItemsByBranchWithPromotions(id);
                model.addAttribute("menuItems", menuItems);

                // Lấy danh sách bàn
                List<TableResponse> tables = tableService.getTablesByBranch(id);
                model.addAttribute("tables", tables);
            }

            // Lấy danh sách khuyến mãi đang áp dụng cho chi nhánh này
            List<PromotionResponse> activePromotions = promotionService.getActivePromotionsByBranch(id);
            model.addAttribute("promotions", activePromotions);

            return "customer/branches/detail";
        } catch (Exception e) {
            return "redirect:/";
        }
    }

    // API endpoint để lấy dữ liệu động qua AJAX
    @GetMapping("/api/branches/{id}/rooms")
    @ResponseBody
    public List<RoomResponse> getRoomsByBranch(@PathVariable Long id) {
        return roomService.getRoomsByBranchWithDetails(id);
    }

    @GetMapping("/api/branches/{id}/menu")
    @ResponseBody
    public List<MenuItemResponse> getMenuByBranch(@PathVariable Long id,
                                                  @RequestParam(required = false) Long categoryId) {
        if (categoryId != null) {
            return menuService.getMenuItemsByCategory(categoryId);
        }
        return menuService.getMenuItemsByBranchWithPromotions(id);
    }

    @GetMapping("/api/branches/{id}/tables")
    @ResponseBody
    public List<TableResponse> getTablesByBranch(@PathVariable Long id) {
        return tableService.getTablesByBranch(id);
    }

    @GetMapping("/api/branches/{id}/promotions")
    @ResponseBody
    public List<PromotionResponse> getPromotionsByBranch(@PathVariable Long id) {
        return promotionService.getActivePromotionsByBranch(id);
    }
}
