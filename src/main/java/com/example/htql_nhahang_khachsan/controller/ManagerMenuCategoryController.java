package com.example.htql_nhahang_khachsan.controller;

import com.example.htql_nhahang_khachsan.dto.MenuCategoryRequest;
import com.example.htql_nhahang_khachsan.dto.MenuCategoryResponse;
import com.example.htql_nhahang_khachsan.entity.MenuCategoryEntity;
import com.example.htql_nhahang_khachsan.enums.Status;
import com.example.htql_nhahang_khachsan.service.AuthService;
import com.example.htql_nhahang_khachsan.service.ManagerMenuCategoryService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/manager")
@RequiredArgsConstructor
public class ManagerMenuCategoryController {

    private final AuthService authService;
    private final ManagerMenuCategoryService menuCategoryService;

    @GetMapping("/menu-categories")
    public String menuCategoryList(Model model, HttpSession session,
                                   @RequestParam(required = false) String name,
                                   @RequestParam(required = false) Status status) {
        if (!authService.isLoggedIn(session) || !authService.isManager(session)) {
            return "redirect:/manager/login";
        }

        Long branchId = authService.getCurrentUserBranchId(session);

        List<MenuCategoryResponse> menuCategories;
        if (name != null || status != null) {
            menuCategories = menuCategoryService.searchMenuCategories(branchId, name, status);
        } else {
            menuCategories = menuCategoryService.getAllMenuCategoriesByBranch(branchId);
        }

        model.addAttribute("menuCategories", menuCategories);
        model.addAttribute("statuses", Status.values());
        model.addAttribute("searchName", name);
        model.addAttribute("searchStatus", status);

        return "manager/menu_categories/list";


    }

    @GetMapping("/menu-categories/add")
    public String addMenuCategoryForm(Model model, HttpSession session) {
        if (!authService.isLoggedIn(session) || !authService.isManager(session)) {
            return "redirect:/manager/login";
        }

        model.addAttribute("menuCategoryRequest", new MenuCategoryRequest());
        model.addAttribute("statuses", Status.values());

        return "manager/menu_categories/form";
    }

    @PostMapping("/menu-categories/add")
    public String addMenuCategory (@Valid @ModelAttribute MenuCategoryRequest request,
                                   BindingResult result,
                                   Model model,
                                   RedirectAttributes redirectAttributes,
                                   HttpSession session) {
        if (!authService.isLoggedIn(session) || !authService.isManager(session)) {
            return "redirect:/manager/login";
        }

        if (result.hasErrors()) {
            model.addAttribute("statuses", Status.values());
            return "manager/menu_categories/form";
        }

        try {
            Long branchId = authService.getCurrentUserBranchId(session);
            menuCategoryService.createMenuCategory(branchId, request);
            redirectAttributes.addFlashAttribute("successMessage", "Thêm loại menu thành công");

        }catch (Exception e){
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/manager/menu-categories";
    }

    @GetMapping("/menu-categories/{id}/edit")
    public String editMenuCategoryForm(@PathVariable Long id,
                                       Model model,
                                       HttpSession session,
                                       RedirectAttributes redirectAttributes) {
        if (!authService.isLoggedIn(session) || !authService.isManager(session)) {
            return "redirect:/manager/login";
        }

        try {
            Long branchId = authService.getCurrentUserBranchId(session);
            MenuCategoryResponse menuCategory = menuCategoryService.getMenuCategoryById(id, branchId);

            MenuCategoryRequest menuCategoryRequest = new MenuCategoryRequest();
            menuCategoryRequest.setName(menuCategory.getName());
            menuCategoryRequest.setDescription(menuCategory.getDescription());
            menuCategoryRequest.setDisplayOrder(menuCategory.getDisplayOrder());
            menuCategoryRequest.setStatus(menuCategory.getStatus());

            model.addAttribute("menuCategoryRequest", menuCategoryRequest);
            model.addAttribute("statuses", Status.values());
            model.addAttribute("menuCategoryId", id);



            return "manager/menu_categories/form";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/manager/menu-categories";
        }
    }

    @PostMapping("/menu-categories/{id}/edit")
    public String updateMenuCategory(@PathVariable Long id,
                                     @Valid @ModelAttribute MenuCategoryRequest request,
                                     BindingResult result,
                                     Model model,
                                     HttpSession session,
                                     RedirectAttributes redirectAttributes) {
        if (!authService.isLoggedIn(session) || !authService.isManager(session)) {
            return "redirect:/manager/login";
        }

        if (result.hasErrors()) {
            model.addAttribute("statuses", Status.values());
            model.addAttribute("menuCategoryId", id);
            return "manager/menu_categories/form";
        }

        try {
            Long branchId = authService.getCurrentUserBranchId(session);
            menuCategoryService.updateMenuCategory(id, branchId, request);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật loại menu thành công");
            return "redirect:/manager/menu-categories";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());

        }
        return "redirect:/manager/menu-categories";
    }

    @PostMapping("/menu-categories/{id}/delete")
    public String deleteMenuCategory(@PathVariable Long id,
                                     HttpSession session,
                                     RedirectAttributes redirectAttributes) {
        if (!authService.isLoggedIn(session) || !authService.isManager(session)) {
            return "redirect:/manager/login";
        }

        try {
            Long branchId = authService.getCurrentUserBranchId(session);
            menuCategoryService.deleteMenuCategory(id, branchId);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa loại menu thành công");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/manager/menu-categories";
    }

    @PostMapping("/menu-categories/update-order")
    public String updateDisplayOrder(@RequestParam List<Long> categoryIds,
                                     RedirectAttributes redirectAttributes,
                                     HttpSession session) {
        if (!authService.isLoggedIn(session) || !authService.isManager(session)) {
            return "redirect:/manager/login";
        }

        try {
            Long branchId = authService.getCurrentUserBranchId(session);
            menuCategoryService.updateDisplayOrder(branchId, categoryIds);
            redirectAttributes.addFlashAttribute("success", "Cập nhật thứ tự hiển thị thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/manager/menu-categories";
    }

    @PostMapping("/manager/logout")
    public String logout(HttpSession session) {
        authService.logout(session);
        return "redirect:/manager/login";
    }
}


