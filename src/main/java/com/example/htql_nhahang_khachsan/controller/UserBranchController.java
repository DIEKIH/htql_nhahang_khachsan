package com.example.htql_nhahang_khachsan.controller;



import com.example.htql_nhahang_khachsan.dto.BranchResponse;
import com.example.htql_nhahang_khachsan.enums.BranchStatus;
import com.example.htql_nhahang_khachsan.enums.BranchType;
import com.example.htql_nhahang_khachsan.service.BranchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class UserBranchController {

    private final BranchService branchService;

//    @GetMapping("/")
//    public String index(Model model) {
//        return "redirect:/branches";
//    }

    @GetMapping("/")
    public String index(Model model,
                        @RequestParam(required = false) String province,
                        @RequestParam(required = false) BranchType type,
                        @RequestParam(required = false) String search) {

        // Lấy tất cả chi nhánh đang hoạt động
        List<BranchResponse> branches = branchService.getActiveBranches();

        // Lọc theo tỉnh thành nếu có
        if (province != null && !province.isEmpty()) {
            branches = branches.stream()
                    .filter(branch -> branch.getProvince() != null &&
                            branch.getProvince().toLowerCase().contains(province.toLowerCase()))
                    .collect(Collectors.toList());
        }

        // Lọc theo loại chi nhánh nếu có
        if (type != null) {
            branches = branches.stream()
                    .filter(branch -> branch.getType() == type)
                    .collect(Collectors.toList());
        }

        // Tìm kiếm theo tên hoặc mô tả nếu có
        if (search != null && !search.trim().isEmpty()) {
            String keyword = search.toLowerCase();
            branches = branches.stream()
                    .filter(branch -> (branch.getName() != null && branch.getName().toLowerCase().contains(keyword)) ||
                            (branch.getDescription() != null && branch.getDescription().toLowerCase().contains(keyword)))
                    .collect(Collectors.toList());
        }

        // Lấy danh sách tỉnh thành để hiển thị filter
        List<String> provinces = branchService.getActiveBranches().stream()
                .map(BranchResponse::getProvince)
                .filter(Objects::nonNull) // bỏ province null
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        // Đẩy dữ liệu xuống view
        model.addAttribute("branches", branches);
        model.addAttribute("provinces", provinces);
        model.addAttribute("branchTypes", BranchType.values());
        model.addAttribute("selectedProvince", province);
        model.addAttribute("selectedType", type);
        model.addAttribute("searchQuery", search);

        return "index";
    }


    @GetMapping("/branches/{id}")
    public String showBranchDetail(@PathVariable Long id, Model model) {
        try {
            BranchResponse branch = branchService.getBranchById(id);

            // Chỉ hiển thị chi nhánh đang hoạt động
            if (branch.getStatus() != BranchStatus.ACTIVE) {
                return "redirect:/branches";
            }

            model.addAttribute("branch", branch);

            // Lấy các chi nhánh liên quan (cùng tỉnh hoặc cùng loại)
            List<BranchResponse> relatedBranches = branchService.getActiveBranches().stream()
                    .filter(b -> !b.getId().equals(id) &&
                            (b.getProvince().equals(branch.getProvince()) ||
                                    b.getType() == branch.getType()))
                    .limit(4)
                    .collect(Collectors.toList());

            model.addAttribute("relatedBranches", relatedBranches);

            return "user/branches/detail";
        } catch (Exception e) {
            return "redirect:/branches";
        }
    }

    @GetMapping("/api/branches")
    @ResponseBody
    public List<BranchResponse> getBranchesApi(@RequestParam(required = false) String province,
                                               @RequestParam(required = false) BranchType type) {
        List<BranchResponse> branches = branchService.getActiveBranches();

        if (province != null && !province.isEmpty()) {
            branches = branches.stream()
                    .filter(branch -> branch.getProvince().toLowerCase()
                            .contains(province.toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (type != null) {
            branches = branches.stream()
                    .filter(branch -> branch.getType() == type)
                    .collect(Collectors.toList());
        }

        return branches;
    }
}