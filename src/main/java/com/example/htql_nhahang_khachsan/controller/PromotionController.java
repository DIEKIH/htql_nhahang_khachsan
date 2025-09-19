package com.example.htql_nhahang_khachsan.controller;

import com.example.htql_nhahang_khachsan.dto.BranchResponse;
import com.example.htql_nhahang_khachsan.dto.PromotionRequest;
import com.example.htql_nhahang_khachsan.dto.PromotionResponse;
import com.example.htql_nhahang_khachsan.enums.PromotionApplicability;
import com.example.htql_nhahang_khachsan.enums.PromotionScope;
import com.example.htql_nhahang_khachsan.enums.PromotionType;
import com.example.htql_nhahang_khachsan.enums.Status;
import com.example.htql_nhahang_khachsan.enums.UserRole;
import com.example.htql_nhahang_khachsan.service.AuthService;
import com.example.htql_nhahang_khachsan.service.BranchService;
import com.example.htql_nhahang_khachsan.service.PromotionService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class PromotionController {

    private final PromotionService promotionService;
    private final BranchService branchService;
    private final AuthService authService;

    @GetMapping("/promotions")
    public String promotionList(Model model, HttpSession session,
                                @RequestParam(required = false) String name,
                                @RequestParam(required = false) PromotionScope scope,
                                @RequestParam(required = false) PromotionApplicability applicability,
                                @RequestParam(required = false) Status status) {

        if (!authService.isLoggedIn(session)) {
            return "redirect:/admin/login";
        }

        UserRole userRole = authService.getCurrentUserRole(session);
        if (!authService.isAdmin(session) && !authService.isManager(session)) {
            return "redirect:/admin/login";
        }

        List<PromotionResponse> promotions;

        if (authService.isAdmin(session)) {
            // Admin xem tất cả khuyến mãi
            if (name != null || scope != null || applicability != null || status != null) {
                promotions = promotionService.searchPromotions(name, scope, applicability, status);
            } else {
                promotions = promotionService.getAllPromotions();
            }
        } else {
            // Manager chỉ xem khuyến mãi của chi nhánh mình
            Long branchId = authService.getCurrentUserBranchId(session);
            if (name != null || scope != null || applicability != null || status != null) {
                promotions = promotionService.searchPromotionsByBranch(name, scope, applicability, status, branchId);
            } else {
                promotions = promotionService.getPromotionsByBranch(branchId);
            }
        }

        // Prepare labels for display
        Map<String, String> typeLabels = Map.of(
                "PERCENTAGE", "Phần trăm (%)",
                "FIXED_AMOUNT", "Số tiền cố định",
                "BOGO", "Mua 1 tặng 1"
        );

        Map<String, String> scopeLabels = Map.of(
                "SYSTEM_WIDE", "Toàn hệ thống",
                "BRANCH_SPECIFIC", "Chi nhánh cụ thể"
        );

        Map<String, String> applicabilityLabels = Map.of(
                "ROOM", "Khách sạn",
                "RESTAURANT", "Nhà hàng",
                "BOTH", "Cả hai"
        );

        Map<String, String> statusLabels = Map.of(
                "ACTIVE", "Hoạt động",
                "INACTIVE", "Không hoạt động"
        );

        boolean admin = authService.isAdmin(session);
        boolean manager = authService.isManager(session);

        model.addAttribute("isAdmin", admin);
        model.addAttribute("isManager", manager);
        model.addAttribute("currentUserRole", userRole);


        model.addAttribute("promotions", promotions);
        model.addAttribute("promotionTypes", PromotionType.values());
        model.addAttribute("promotionScopes", PromotionScope.values());
        model.addAttribute("promotionApplicabilities", PromotionApplicability.values());
        model.addAttribute("promotionStatuses", Status.values());
        model.addAttribute("typeLabels", typeLabels);
        model.addAttribute("scopeLabels", scopeLabels);
        model.addAttribute("applicabilityLabels", applicabilityLabels);
        model.addAttribute("statusLabels", statusLabels);
        model.addAttribute("searchName", name);
        model.addAttribute("searchScope", scope);
        model.addAttribute("searchApplicability", applicability);
        model.addAttribute("searchStatus", status);
//        model.addAttribute("isAdmin", authService.isAdmin(session));
//        model.addAttribute("isManager", authService.isManager(session));
        model.addAttribute("currentUserRole", userRole);

        return "admin/promotions/list";
    }

    @GetMapping("/promotions/add")
    public String addPromotionForm(Model model, HttpSession session) {
        if (!authService.isLoggedIn(session)) {
            return "redirect:/admin/login";
        }

        if (!authService.isAdmin(session) && !authService.isManager(session)) {
            return "redirect:/admin/login";
        }

        Map<String, String> typeLabels = Map.of(
                "PERCENTAGE", "Phần trăm (%)",
                "FIXED_AMOUNT", "Số tiền cố định",
                "BOGO", "Mua 1 tặng 1"
        );

        Map<String, String> scopeLabels = Map.of(
                "SYSTEM_WIDE", "Toàn hệ thống",
                "BRANCH_SPECIFIC", "Chi nhánh cụ thể"
        );

        Map<String, String> applicabilityLabels = Map.of(
                "ROOM", "Khách sạn",
                "RESTAURANT", "Nhà hàng",
                "BOTH", "Cả hai"
        );

        Map<String, String> statusLabels = Map.of(
                "ACTIVE", "Hoạt động",
                "INACTIVE", "Không hoạt động"
        );

        // --- TẠO promotionRequest và set mặc định cho Manager ---
        PromotionRequest promotionRequest = new PromotionRequest();


        model.addAttribute("promotionRequest", new PromotionRequest());
        model.addAttribute("promotionTypes", PromotionType.values());
        model.addAttribute("promotionScopes", PromotionScope.values());
        model.addAttribute("promotionApplicabilities", PromotionApplicability.values());
        model.addAttribute("promotionStatuses", Status.values());
        model.addAttribute("typeLabels", typeLabels);
        model.addAttribute("scopeLabels", scopeLabels);
        model.addAttribute("applicabilityLabels", applicabilityLabels);
        model.addAttribute("statusLabels", statusLabels);
        model.addAttribute("isAdmin", authService.isAdmin(session));
        model.addAttribute("isManager", authService.isManager(session));


        // Admin thì hiển thị danh sách chi nhánh, Manager thì không
//        if (authService.isAdmin(session)) {
////            model.addAttribute("branches", branchService.getActiveBranches());
////        }

//        if (authService.isAdmin(session)) {
//            // Admin thì hiển thị danh sách chi nhánh
//            model.addAttribute("branches", branchService.getActiveBranches());
//        } else if (authService.isManager(session)) {
//            // Manager thì gắn branchId để Thymeleaf hidden input
//            Long branchId = authService.getCurrentUserBranchId(session);
//            model.addAttribute("managerBranchId", branchId);
//        }

        if (authService.isAdmin(session)) {
            // Admin thì hiển thị danh sách chi nhánh
            model.addAttribute("branches", branchService.getActiveBranches());
        } else if (authService.isManager(session)) {
            // Manager: set default scope = BRANCH_SPECIFIC và gắn branchId vào request
            Long branchId = authService.getCurrentUserBranchId(session);

            promotionRequest.setScope(PromotionScope.BRANCH_SPECIFIC);
            Set<Long> branchIds = new HashSet<>();
            branchIds.add(branchId);
            promotionRequest.setBranchIds(branchIds);

            model.addAttribute("managerBranchId", branchId);
            // Nếu bạn có method lấy tên chi nhánh:
            model.addAttribute("managerBranchName", branchService.getBranchNameById(branchId));
        }

        model.addAttribute("promotionRequest", promotionRequest);

        return "admin/promotions/form";
    }

    @PostMapping("/promotions/add")
    public String addPromotion(@Valid @ModelAttribute PromotionRequest request,
                               BindingResult result,
                               Model model,
                               RedirectAttributes redirectAttributes,
                               HttpSession session) {

        if (!authService.isLoggedIn(session)) {
            return "redirect:/admin/login";
        }

        if (!authService.isAdmin(session) && !authService.isManager(session)) {
            return "redirect:/admin/login";
        }

        if (result.hasErrors()) {
            prepareFormModel(model, session);
            return "admin/promotions/form";
        }

        try {
            Long userId = authService.getCurrentUserId(session);

            if (authService.isAdmin(session)) {
                promotionService.createPromotion(request, userId);
            } else {
                // Manager
                Long branchId = authService.getCurrentUserBranchId(session);
                promotionService.createPromotionForManager(request, userId, branchId);
            }

            redirectAttributes.addFlashAttribute("success", "Thêm khuyến mãi thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/admin/promotions";
    }

    @GetMapping("/promotions/{id}/edit")
    public String editPromotionForm(@PathVariable Long id,
                                    Model model,
                                    HttpSession session,
                                    RedirectAttributes redirectAttributes) {
        if (!authService.isLoggedIn(session)) {
            return "redirect:/admin/login";
        }

        if (!authService.isAdmin(session) && !authService.isManager(session)) {
            return "redirect:/admin/login";
        }

        try {
            PromotionResponse promotion = promotionService.getPromotionById(id);

            // Kiểm tra quyền truy cập cho Manager
            if (authService.isManager(session)) {
                Long branchId = authService.getCurrentUserBranchId(session);
                boolean hasPermission = promotion.getScope() == PromotionScope.BRANCH_SPECIFIC &&
                        promotion.getBranchIds() != null &&
                        promotion.getBranchIds().contains(branchId);

                if (!hasPermission) {
                    redirectAttributes.addFlashAttribute("error", "Bạn không có quyền sửa khuyến mãi này");
                    return "redirect:/admin/promotions";
                }
            }

            PromotionRequest promotionRequest = new PromotionRequest();
            promotionRequest.setName(promotion.getName());
            promotionRequest.setDescription(promotion.getDescription());
            promotionRequest.setType(promotion.getType());
            promotionRequest.setDiscountValue(promotion.getDiscountValue());
            promotionRequest.setMinAmount(promotion.getMinAmount());
            promotionRequest.setMaxDiscount(promotion.getMaxDiscount());
            promotionRequest.setStartDate(promotion.getStartDate());
            promotionRequest.setEndDate(promotion.getEndDate());
            promotionRequest.setUsageLimit(promotion.getUsageLimit());
            promotionRequest.setScope(promotion.getScope());
            promotionRequest.setBranchIds(promotion.getBranchIds());
            promotionRequest.setApplicability(promotion.getApplicability());
            promotionRequest.setStatus(promotion.getStatus());

            model.addAttribute("promotionRequest", promotionRequest);
            model.addAttribute("promotionId", id);

            prepareFormModel(model, session);

            return "admin/promotions/form";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/promotions";
        }
    }


    @PostMapping("/promotions/{id}/edit")
    public String updatePromotion(@PathVariable Long id,
                                  @Valid @ModelAttribute PromotionRequest request,
                                  BindingResult result,
                                  Model model,
                                  RedirectAttributes redirectAttributes,
                                  HttpSession session) {

        if (!authService.isLoggedIn(session)) {
            return "redirect:/admin/login";
        }

        if (!authService.isAdmin(session) && !authService.isManager(session)) {
            return "redirect:/admin/login";
        }

        if (result.hasErrors()) {
            model.addAttribute("promotionId", id);
            prepareFormModel(model, session);
            return "admin/promotions/form";
        }

        try {
            if (authService.isAdmin(session)) {
                promotionService.updatePromotion(id, request);
            } else {
                // Manager
                Long branchId = authService.getCurrentUserBranchId(session);
                promotionService.updatePromotionForManager(id, request, branchId);
            }

            redirectAttributes.addFlashAttribute("success", "Cập nhật khuyến mãi thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/admin/promotions";
    }

    @PostMapping("/promotions/{id}/delete")
    public String deletePromotion(@PathVariable Long id,
                                  RedirectAttributes redirectAttributes,
                                  HttpSession session) {

        if (!authService.isLoggedIn(session)) {
            return "redirect:/admin/login";
        }

        if (!authService.isAdmin(session) && !authService.isManager(session)) {
            return "redirect:/admin/login";
        }

        try {
            if (authService.isAdmin(session)) {
                promotionService.deletePromotion(id);
            } else {
                // Manager
                Long branchId = authService.getCurrentUserBranchId(session);
                promotionService.deletePromotionForManager(id, branchId);
            }

            redirectAttributes.addFlashAttribute("success", "Ẩn khuyến mãi thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/admin/promotions";
    }

    private void prepareFormModel(Model model, HttpSession session) {
        Map<String, String> typeLabels = Map.of(
                "PERCENTAGE", "Phần trăm (%)",
                "FIXED_AMOUNT", "Số tiền cố định",
                "BOGO", "Mua 1 tặng 1"
        );

        Map<String, String> scopeLabels = Map.of(
                "SYSTEM_WIDE", "Toàn hệ thống",
                "BRANCH_SPECIFIC", "Chi nhánh cụ thể"
        );

        Map<String, String> applicabilityLabels = Map.of(
                "ROOM", "Khách sạn",
                "RESTAURANT", "Nhà hàng",
                "BOTH", "Cả hai"
        );

        Map<String, String> statusLabels = Map.of(
                "ACTIVE", "Hoạt động",
                "INACTIVE", "Không hoạt động"
        );

        model.addAttribute("promotionTypes", PromotionType.values());
        model.addAttribute("promotionScopes", PromotionScope.values());
        model.addAttribute("promotionApplicabilities", PromotionApplicability.values());
        model.addAttribute("promotionStatuses", Status.values());
        model.addAttribute("typeLabels", typeLabels);
        model.addAttribute("scopeLabels", scopeLabels);
        model.addAttribute("applicabilityLabels", applicabilityLabels);
        model.addAttribute("statusLabels", statusLabels);
        model.addAttribute("isAdmin", authService.isAdmin(session));
        model.addAttribute("isManager", authService.isManager(session));


//        if (authService.isAdmin(session)) {
//            model.addAttribute("branches", branchService.getActiveBranches());
//        }

        if (authService.isAdmin(session)) {
            model.addAttribute("branches", branchService.getActiveBranches());
        } else if (authService.isManager(session)) {
            Long branchId = authService.getCurrentUserBranchId(session);
            model.addAttribute("managerBranchId", branchId);
            model.addAttribute("managerBranchName", branchService.getBranchNameById(branchId));
        }
    }
}