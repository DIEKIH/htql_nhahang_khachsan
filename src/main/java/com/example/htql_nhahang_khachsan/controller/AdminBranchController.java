package com.example.htql_nhahang_khachsan.controller;

import com.example.htql_nhahang_khachsan.dto.BranchRequest;
import com.example.htql_nhahang_khachsan.dto.BranchResponse;
import com.example.htql_nhahang_khachsan.dto.UserRequest;
import com.example.htql_nhahang_khachsan.dto.UserResponse;
import com.example.htql_nhahang_khachsan.enums.BranchStatus;
import com.example.htql_nhahang_khachsan.enums.BranchType;
import com.example.htql_nhahang_khachsan.enums.UserRole;
import com.example.htql_nhahang_khachsan.enums.UserStatus;
import com.example.htql_nhahang_khachsan.service.AuthService;
import com.example.htql_nhahang_khachsan.service.BranchService;
import com.example.htql_nhahang_khachsan.service.StaffService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminBranchController {

    private final BranchService branchService;
    private final StaffService staffService;
    private final AuthService authService;

    // === BRANCH MANAGEMENT ===
    @GetMapping("/branches")
    public String branchList(Model model, HttpSession session,
                             @RequestParam(required = false) String name,
                             @RequestParam(required = false) BranchType type,
                             @RequestParam(required = false) BranchStatus status) {
        if (!authService.isLoggedIn(session) || !authService.isAdmin(session)) {
            return "redirect:/admin/login";
        }

        List<BranchResponse> branches;
        if (name != null || type != null || status != null) {
            branches = branchService.searchBranches(name, type, status);
        } else {
            branches = branchService.getAllBranches();
        }

        model.addAttribute("branches", branches);
        model.addAttribute("branchTypes", BranchType.values());
        model.addAttribute("branchStatuses", BranchStatus.values());
        model.addAttribute("searchName", name);
        model.addAttribute("searchType", type);
        model.addAttribute("searchStatus", status);

        return "admin/branches/list";
    }

    @GetMapping("/branches/add")
    public String addBranchForm(Model model, HttpSession session) {
        if (!authService.isLoggedIn(session) || !authService.isAdmin(session)) {
            return "redirect:/admin/login";
        }

        model.addAttribute("branchRequest", new BranchRequest());
        model.addAttribute("branchTypes", BranchType.values());
        model.addAttribute("branchStatuses", BranchStatus.values());

        return "admin/branches/form";
    }

    @PostMapping("/branches/add")
    public String addBranch(@Valid @ModelAttribute BranchRequest request,
                            BindingResult result,
                            Model model,
                            RedirectAttributes redirectAttributes,
                            HttpSession session) {
        if (!authService.isLoggedIn(session) || !authService.isAdmin(session)) {
            return "redirect:/admin/login";
        }

        if (result.hasErrors()) {
            model.addAttribute("branchTypes", BranchType.values());
            model.addAttribute("branchStatuses", BranchStatus.values());
            return "admin/branches/form";
        }

        try {
            branchService.createBranch(request);
            redirectAttributes.addFlashAttribute("success", "Thêm chi nhánh thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/admin/branches";
    }

    @GetMapping("/branches/{id}/edit")
    public String editBranchForm(@PathVariable Long id, Model model, HttpSession session) {
        if (!authService.isLoggedIn(session) || !authService.isAdmin(session)) {
            return "redirect:/admin/login";
        }

        try {
            BranchResponse branch = branchService.getBranchById(id);

            BranchRequest branchRequest = new BranchRequest();
            branchRequest.setName(branch.getName());
            branchRequest.setDescription(branch.getDescription());
            branchRequest.setAddress(branch.getAddress());
            branchRequest.setPhoneNumber(branch.getPhoneNumber());
            branchRequest.setEmail(branch.getEmail());
            branchRequest.setType(branch.getType());
            branchRequest.setStatus(branch.getStatus());
            branchRequest.setImageUrl(branch.getImageUrl());
            branchRequest.setLatitude(branch.getLatitude());
            branchRequest.setLongitude(branch.getLongitude());

            model.addAttribute("branchRequest", branchRequest);
            model.addAttribute("branchId", id);
            model.addAttribute("branchTypes", BranchType.values());
            model.addAttribute("branchStatuses", BranchStatus.values());

            return "admin/branches/form";
        } catch (Exception e) {
            return "redirect:/admin/branches";
        }
    }

    @PostMapping("/branches/{id}/edit")
    public String updateBranch(@PathVariable Long id,
                               @Valid @ModelAttribute BranchRequest request,
                               BindingResult result,
                               Model model,
                               RedirectAttributes redirectAttributes,
                               HttpSession session) {
        if (!authService.isLoggedIn(session) || !authService.isAdmin(session)) {
            return "redirect:/admin/login";
        }

        if (result.hasErrors()) {
            model.addAttribute("branchId", id);
            model.addAttribute("branchTypes", BranchType.values());
            model.addAttribute("branchStatuses", BranchStatus.values());
            return "admin/branches/form";
        }

        try {
            branchService.updateBranch(id, request);
            redirectAttributes.addFlashAttribute("success", "Cập nhật chi nhánh thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/admin/branches";
    }

    @PostMapping("/branches/{id}/delete")
    public String deleteBranch(@PathVariable Long id,
                               RedirectAttributes redirectAttributes,
                               HttpSession session) {
        if (!authService.isLoggedIn(session) || !authService.isAdmin(session)) {
            return "redirect:/admin/login";
        }

        try {
            branchService.deleteBranch(id);
            redirectAttributes.addFlashAttribute("success", "Ẩn chi nhánh thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/admin/branches";
    }


    // === STAFF MANAGEMENT ===
    @GetMapping("/staff")
    public String staffList(Model model, HttpSession session,
                            @RequestParam(required = false) String role,
                            @RequestParam(required = false) Long branchId,
                            @RequestParam(required = false) UserStatus status,
                            @RequestParam(required = false) String search) {
        if (!authService.isLoggedIn(session) || !authService.isAdmin(session)) {
            return "redirect:/admin/login";
        }

        List<UserRole> roles = new ArrayList<>();
        if ("MANAGER".equals(role)) {
            roles.add(UserRole.MANAGER);
        } else if ("STAFF".equals(role)) {
            roles.add(UserRole.STAFF);
        } else if ("CASHIER_RESTAURANT".equals(role)) {
            roles.add(UserRole.CASHIER_RESTAURANT);
        } else if ("CASHIER_HOTEL".equals(role)) {
            roles.add(UserRole.CASHIER_HOTEL);
        } else {
            roles.addAll(Arrays.asList(
                    UserRole.MANAGER,
                    UserRole.STAFF,
                    UserRole.CASHIER_RESTAURANT,
                    UserRole.CASHIER_HOTEL
            ));
        }


        Map<UserStatus, String> statusLabels = Map.of(
                UserStatus.ACTIVE, "Hoạt động",
                UserStatus.LOCKED, "Bị khóa",
                UserStatus.INACTIVE, "Không hoạt động"
        );

        Map<UserRole, String> roleLabels = Map.of(
                UserRole.MANAGER, "Quản lý",
                UserRole.STAFF, "Nhân viên",
                UserRole.CASHIER_RESTAURANT, "Thu ngân Nhà hàng",
                UserRole.CASHIER_HOTEL, "Thu ngân Khách sạn"
        );


        List<UserResponse> staff = staffService.searchStaff(roles, branchId, status, search);
        List<BranchResponse> branches = branchService.getActiveBranches();

        model.addAttribute("staffList", staff);
        model.addAttribute("branches", branches);
        model.addAttribute("userStatuses", UserStatus.values());
        model.addAttribute("statusLabels", statusLabels);
        model.addAttribute("roleLabels", roleLabels);
        model.addAttribute("searchRole", role);
        model.addAttribute("searchBranchId", branchId);
        model.addAttribute("searchStatus", status);
        model.addAttribute("searchQuery", search);

        return "admin/staff/list";
    }


    @GetMapping("/staff/add")
    public String addStaffForm(Model model, HttpSession session) {
        if (!authService.isLoggedIn(session) || !authService.isAdmin(session)) {
            return "redirect:/admin/login";
        }

        Map<String, String> roleLabels = Map.of(
                "MANAGER", "Quản lý",
                "STAFF", "Nhân viên",
                "CASHIER_RESTAURANT", "Thu ngân Nhà hàng",
                "CASHIER_HOTEL", "Thu ngân Khách sạn"
        );

        model.addAttribute("userRequest", new UserRequest());
        model.addAttribute("branches", branchService.getActiveBranches());
//        model.addAttribute("staffRoles", Arrays.asList(UserRole.MANAGER, UserRole.STAFF));
        model.addAttribute("roleLabels", roleLabels);
        model.addAttribute("staffRoles", Arrays.asList(
                UserRole.MANAGER,
                UserRole.STAFF,
                UserRole.CASHIER_RESTAURANT,
                UserRole.CASHIER_HOTEL
        ));


        return "admin/staff/form";
    }

    @PostMapping("/staff/add")
    public String addStaff(@Valid @ModelAttribute UserRequest request,
                           BindingResult result,
                           Model model,
                           RedirectAttributes redirectAttributes,
                           HttpSession session) {
        if (!authService.isLoggedIn(session) || !authService.isAdmin(session)) {
            return "redirect:/admin/login";
        }

//        if (result.hasErrors()) {
//            model.addAttribute("branches", branchService.getActiveBranches());
//            model.addAttribute("staffRoles", Arrays.asList(UserRole.MANAGER, UserRole.STAFF));
//            return "admin/staff/form";
//        }

        if (result.hasErrors()) {
            Map<UserRole, String> roleLabels = Map.of(
                    UserRole.MANAGER, "Quản lý",
                    UserRole.STAFF, "Nhân viên",
                    UserRole.CASHIER_RESTAURANT, "Thu ngân Nhà hàng",
                    UserRole.CASHIER_HOTEL, "Thu ngân Khách sạn"
            );

            model.addAttribute("branches", branchService.getActiveBranches());
            model.addAttribute("staffRoles", Arrays.asList(
                    UserRole.MANAGER,
                    UserRole.STAFF,
                    UserRole.CASHIER_RESTAURANT,
                    UserRole.CASHIER_HOTEL
            ));
            model.addAttribute("roleLabels", roleLabels);

            return "admin/staff/form";
        }

        try {
            staffService.createStaff(request);
            redirectAttributes.addFlashAttribute("success",
                    "Thêm " + (request.getRole() == UserRole.MANAGER ? "quản lý" : "nhân viên") + " thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/admin/staff";
    }

    @GetMapping("/staff/{id}/edit")
    public String editStaffForm(@PathVariable Long id, Model model, HttpSession session) {
        if (!authService.isLoggedIn(session) || !authService.isAdmin(session)) {
            return "redirect:/admin/login";
        }
        Map<String, String> roleLabels = Map.of(
                "MANAGER", "Quản lý",
                "STAFF", "Nhân viên",
                "CASHIER_RESTAURANT", "Thu ngân Nhà hàng",
                "CASHIER_HOTEL", "Thu ngân Khách sạn"
        );




        try {
            UserResponse user = staffService.getStaffById(id);

            UserRequest userRequest = new UserRequest();
            userRequest.setUsername(user.getUsername());
            userRequest.setEmail(user.getEmail());
            userRequest.setFullName(user.getFullName());
            userRequest.setPhoneNumber(user.getPhoneNumber());
            userRequest.setAddress(user.getAddress());
            userRequest.setAvatarUrl(user.getAvatarUrl());
            userRequest.setRole(user.getRole());
            userRequest.setBranchId(user.getBranchId());

            model.addAttribute("userRequest", userRequest);
            model.addAttribute("userId", id);
            model.addAttribute("branches", branchService.getActiveBranches());
//            model.addAttribute("staffRoles", Arrays.asList(UserRole.MANAGER, UserRole.STAFF));
            model.addAttribute("roleLabels", roleLabels);
            model.addAttribute("staffRoles", Arrays.asList(
                    UserRole.MANAGER,
                    UserRole.STAFF,
                    UserRole.CASHIER_RESTAURANT,
                    UserRole.CASHIER_HOTEL
            ));


            return "admin/staff/form";
        } catch (Exception e) {
            return "redirect:/admin/staff";
        }
    }

    @PostMapping("/staff/{id}/edit")
    public String updateStaff(@PathVariable Long id,
                              @Valid @ModelAttribute UserRequest request,
                              BindingResult result,
                              Model model,
                              RedirectAttributes redirectAttributes,
                              HttpSession session) {
        if (!authService.isLoggedIn(session) || !authService.isAdmin(session)) {
            return "redirect:/admin/login";
        }

//        if (result.hasErrors()) {
//            model.addAttribute("userId", id);
//            model.addAttribute("branches", branchService.getActiveBranches());
//            model.addAttribute("staffRoles", Arrays.asList(UserRole.MANAGER, UserRole.STAFF));
//            return "admin/staff/form";
//        }

        if (result.hasErrors()) {
//            Map<UserRole, String> roleLabels = Map.of(
//                    UserRole.MANAGER, "Quản lý",
//                    UserRole.STAFF, "Nhân viên",
//                    UserRole.CASHIER_RESTAURANT, "Thu ngân Nhà hàng",
//                    UserRole.CASHIER_HOTEL, "Thu ngân Khách sạn"
//            );
            Map<String, String> roleLabels = Map.of(
                    "MANAGER", "Quản lý",
                    "STAFF", "Nhân viên",
                    "CASHIER_RESTAURANT", "Thu ngân Nhà hàng",
                    "CASHIER_HOTEL", "Thu ngân Khách sạn"
            );

            model.addAttribute("userId", id);
            model.addAttribute("branches", branchService.getActiveBranches());
            model.addAttribute("staffRoles", Arrays.asList(
                    UserRole.MANAGER,
                    UserRole.STAFF,
                    UserRole.CASHIER_RESTAURANT,
                    UserRole.CASHIER_HOTEL
            ));
            model.addAttribute("roleLabels", roleLabels);

            return "admin/staff/form";
        }


        try {
            staffService.updateStaff(id, request);
            redirectAttributes.addFlashAttribute("success", "Cập nhật thông tin thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/admin/staff";
    }

    @PostMapping("/staff/{id}/toggle-status")
    public String toggleStaffStatus(@PathVariable Long id,
                                    RedirectAttributes redirectAttributes,
                                    HttpSession session) {
        if (!authService.isLoggedIn(session) || !authService.isAdmin(session)) {
            return "redirect:/admin/login";
        }

        try {
            staffService.lockStaff(id);
            redirectAttributes.addFlashAttribute("success", "Thay đổi trạng thái thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/admin/staff";
    }

    // Logout
    @PostMapping("/logout")
    public String logout(HttpSession session) {
        authService.logout(session);
        return "redirect:/admin/login";
    }
}
