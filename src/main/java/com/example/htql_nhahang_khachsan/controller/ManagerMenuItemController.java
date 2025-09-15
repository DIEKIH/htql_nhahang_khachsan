package com.example.htql_nhahang_khachsan.controller;

import com.example.htql_nhahang_khachsan.dto.MenuItemRequest;
import com.example.htql_nhahang_khachsan.dto.MenuItemResponse;
import com.example.htql_nhahang_khachsan.enums.Status;
import com.example.htql_nhahang_khachsan.service.AuthService;
import com.example.htql_nhahang_khachsan.service.ManagerMenuItemService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/manager")
@RequiredArgsConstructor
public class ManagerMenuItemController {

    private final AuthService authService;
    private final ManagerMenuItemService menuItemService;

    @GetMapping("/menu-items")
    public String menuItemList(Model model, HttpSession session,
                               @RequestParam(required = false) String name,
                               @RequestParam(required = false) Long categoryId,
                               @RequestParam(required = false) Status status) {
        if (!authService.isLoggedIn(session) || !authService.isManager(session)) {
            return "redirect:/manager/login";
        }

        Long branchId = authService.getCurrentUserBranchId(session);

        List<MenuItemResponse> menuItems;
        if (name != null || categoryId != null || status != null) {
            menuItems = menuItemService.searchMenuItems(branchId, name, categoryId, status);
        } else {
            menuItems = menuItemService.getAllMenuItemsByBranch(branchId);
        }

        model.addAttribute("menuItems", menuItems);
        model.addAttribute("categories", menuItemService.getActiveMenuCategoriesByBranch(branchId));
        model.addAttribute("statuses", Status.values());
        model.addAttribute("searchName", name);
        model.addAttribute("searchCategoryId", categoryId);
        model.addAttribute("searchStatus", status);

        return "manager/menu_items/list";
    }

    @GetMapping("/menu-items/add")
    public String addMenuItemForm(Model model, HttpSession session) {
        if (!authService.isLoggedIn(session) || !authService.isManager(session)) {
            return "redirect:/manager/login";
        }

        Long branchId = authService.getCurrentUserBranchId(session);

        model.addAttribute("menuItemRequest", new MenuItemRequest());
        model.addAttribute("categories", menuItemService.getActiveMenuCategoriesByBranch(branchId));
        model.addAttribute("statuses", Status.values());

        return "manager/menu_items/form";
    }

    @PostMapping("/menu-items/add")
    public String addMenuItem(@Valid @ModelAttribute MenuItemRequest request,
                              BindingResult result,
                              @RequestParam(value = "imageFiles", required = false) List<MultipartFile> imageFiles,
                              Model model,
                              RedirectAttributes redirectAttributes,
                              HttpSession session) {
        if (!authService.isLoggedIn(session) || !authService.isManager(session)) {
            return "redirect:/manager/login";
        }

        if (result.hasErrors()) {
            Long branchId = authService.getCurrentUserBranchId(session);
            model.addAttribute("categories", menuItemService.getActiveMenuCategoriesByBranch(branchId));
            model.addAttribute("statuses", Status.values());
            return "manager/menu_items/form";
        }

        try {
            Long branchId = authService.getCurrentUserBranchId(session);
            menuItemService.createMenuItem(branchId, request, imageFiles);
            redirectAttributes.addFlashAttribute("success", "Thêm món ăn thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/manager/menu-items";
    }

    @GetMapping("/menu-items/{id}/edit")
    public String editMenuItemForm(@PathVariable Long id, Model model, HttpSession session) {
        if (!authService.isLoggedIn(session) || !authService.isManager(session)) {
            return "redirect:/manager/login";
        }

        try {
            Long branchId = authService.getCurrentUserBranchId(session);
            MenuItemResponse menuItem = menuItemService.getMenuItemById(id, branchId);

            MenuItemRequest menuItemRequest = new MenuItemRequest();
            menuItemRequest.setName(menuItem.getName());
            menuItemRequest.setDescription(menuItem.getDescription());
            menuItemRequest.setPrice(menuItem.getPrice());
            menuItemRequest.setCategoryId(menuItem.getCategoryId());
            menuItemRequest.setPreparationTime(menuItem.getPreparationTime());
            menuItemRequest.setIsAvailable(menuItem.getIsAvailable());
            menuItemRequest.setIngredients(menuItem.getIngredients());
            menuItemRequest.setAllergens(menuItem.getAllergens());
            menuItemRequest.setCalories(menuItem.getCalories());
            menuItemRequest.setStatus(menuItem.getStatus());

            model.addAttribute("menuItemRequest", menuItemRequest);
            model.addAttribute("menuItemId", id);
            model.addAttribute("categories", menuItemService.getActiveMenuCategoriesByBranch(branchId));
            model.addAttribute("statuses", Status.values());
            model.addAttribute("existingImages", menuItem.getMenuItemImages());

            return "manager/menu_items/form";
        } catch (Exception e) {
            return "redirect:/manager/menu-items";
        }
    }

    @PostMapping("/menu-items/{id}/edit")
    public String updateMenuItem(@PathVariable Long id,
                                 @Valid @ModelAttribute MenuItemRequest request,
                                 BindingResult result,
                                 @RequestParam(value = "imageFiles", required = false) List<MultipartFile> imageFiles,
                                 @RequestParam(value = "deleteImageIds", required = false) List<Long> deleteImageIds,
                                 Model model,
                                 RedirectAttributes redirectAttributes,
                                 HttpSession session) {
        if (!authService.isLoggedIn(session) || !authService.isManager(session)) {
            return "redirect:/manager/login";
        }

        if (result.hasErrors()) {
            Long branchId = authService.getCurrentUserBranchId(session);
            model.addAttribute("menuItemId", id);
            model.addAttribute("categories", menuItemService.getActiveMenuCategoriesByBranch(branchId));
            model.addAttribute("statuses", Status.values());

            // Reload existing images for display
            try {
                MenuItemResponse menuItem = menuItemService.getMenuItemById(id, branchId);
                model.addAttribute("existingImages", menuItem.getMenuItemImages());
            } catch (Exception e) {
                // Handle silently
            }

            return "manager/menu_items/form";
        }

        try {
            Long branchId = authService.getCurrentUserBranchId(session);
            menuItemService.updateMenuItem(id, branchId, request, imageFiles, deleteImageIds);
            redirectAttributes.addFlashAttribute("success", "Cập nhật món ăn thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/manager/menu-items";
    }

    @PostMapping("/menu-items/{id}/delete")
    public String deleteMenuItem(@PathVariable Long id,
                                 RedirectAttributes redirectAttributes,
                                 HttpSession session) {
        if (!authService.isLoggedIn(session) || !authService.isManager(session)) {
            return "redirect:/manager/login";
        }

        try {
            Long branchId = authService.getCurrentUserBranchId(session);
            menuItemService.deleteMenuItem(id, branchId);
            redirectAttributes.addFlashAttribute("success", "Ẩn món ăn thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/manager/menu-items";
    }
}