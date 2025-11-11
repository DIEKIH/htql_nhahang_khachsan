package com.example.htql_nhahang_khachsan.controller;



import com.example.htql_nhahang_khachsan.dto.BranchResponse;
import com.example.htql_nhahang_khachsan.dto.RoomTypeResponse;
import com.example.htql_nhahang_khachsan.dto.MenuItemResponse;
import com.example.htql_nhahang_khachsan.enums.BranchType;
import com.example.htql_nhahang_khachsan.service.BranchService;
import com.example.htql_nhahang_khachsan.service.ReviewRecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.example.htql_nhahang_khachsan.service.BranchViewHistoryService;
import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;


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

    // Thêm vào UserBranchController.java
    private final BranchViewHistoryService viewHistoryService;
    private final ReviewRecommendationService reviewRecommendationService;


    @GetMapping("/")
    public String index(Model model,
                        @RequestParam(required = false) String province,
                        @RequestParam(required = false) BranchType type,
                        @RequestParam(required = false) String search,
                        HttpSession session) {

        // Lấy tất cả chi nhánh đang hoạt động
        List<BranchResponse> branches = branchService.getActiveBranches();

        // Lọc theo tỉnh thành nếu có
        if (province != null && !province.isEmpty()) {
            branches = branches.stream()
                    .filter(branch -> branch.getProvince().toLowerCase()
                            .contains(province.toLowerCase()))
                    .collect(Collectors.toList());
        }

        // Lọc theo loại chi nhánh nếu có
        if (type != null) {
            branches = branches.stream()
                    .filter(branch -> branch.getType() == type)
                    .collect(Collectors.toList());
        }

        // Tìm kiếm theo tên nếu có
        if (search != null && !search.trim().isEmpty()) {
            branches = branches.stream()
                    .filter(branch -> branch.getName().toLowerCase()
                            .contains(search.toLowerCase()) ||
                            branch.getDescription().toLowerCase()
                                    .contains(search.toLowerCase()))
                    .collect(Collectors.toList());
        }

        // Lấy danh sách tỉnh thành để hiển thị filter
        List<String> provinces = branchService.getActiveBranches().stream()
                .map(BranchResponse::getProvince)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        // Lịch sử xem gần đây
        boolean hasViewHistory = viewHistoryService.hasViewHistory(session);
        List<BranchResponse> recentlyViewedBranches = new ArrayList<>();
        List<BranchResponse> suggestedBranches = new ArrayList<>();

        if (hasViewHistory) {
            recentlyViewedBranches = viewHistoryService.getRecentlyViewedBranches(session, 4);
            suggestedBranches = viewHistoryService.getSuggestedBranches(session, 4);
        }

        // ========== GỢI Ý DỰA TRÊN ĐÁNH GIÁ ==========
        // ✅ Đổi kiểu dữ liệu thành ItemWithRating<T>
        List<ReviewRecommendationService.ItemWithRating<BranchResponse>> topRatedBranches =
                reviewRecommendationService.getTop3RecommendedBranches();

        List<ReviewRecommendationService.ItemWithRating<RoomTypeResponse>> topRatedRoomTypes =
                reviewRecommendationService.getTop3RecommendedRoomTypes();

        List<ReviewRecommendationService.ItemWithRating<MenuItemResponse>> topRatedMenuItems =
                reviewRecommendationService.getTop3RecommendedMenuItems();

        // Thêm vào model
        model.addAttribute("branches", branches);
        model.addAttribute("provinces", provinces);
        model.addAttribute("branchTypes", BranchType.values());
        model.addAttribute("selectedProvince", province);
        model.addAttribute("selectedType", type);
        model.addAttribute("searchQuery", search);

        // Lịch sử xem
        model.addAttribute("hasViewHistory", hasViewHistory);
        model.addAttribute("recentlyViewedBranches", recentlyViewedBranches);
        model.addAttribute("suggestedBranches", suggestedBranches);

        // ✅ Gợi ý dựa trên đánh giá (đã bao gồm rating trung bình + số reviews)
        model.addAttribute("topRatedBranches", topRatedBranches);
        model.addAttribute("topRatedRoomTypes", topRatedRoomTypes);
        model.addAttribute("topRatedMenuItems", topRatedMenuItems);

        return "index";
    }
//    @GetMapping("/")
//    public String index(Model model,
//                        @RequestParam(required = false) String province,
//                        @RequestParam(required = false) BranchType type,
//                        @RequestParam(required = false) String search,
//                        HttpSession session) {
//
//        // Lấy tất cả chi nhánh đang hoạt động
//        List<BranchResponse> branches = branchService.getActiveBranches();
//
//        // Lọc theo tỉnh thành nếu có
//        if (province != null && !province.isEmpty()) {
//            branches = branches.stream()
//                    .filter(branch -> branch.getProvince().toLowerCase()
//                            .contains(province.toLowerCase()))
//                    .collect(Collectors.toList());
//        }
//
//        // Lọc theo loại chi nhánh nếu có
//        if (type != null) {
//            branches = branches.stream()
//                    .filter(branch -> branch.getType() == type)
//                    .collect(Collectors.toList());
//        }
//
//        // Tìm kiếm theo tên nếu có
//        if (search != null && !search.trim().isEmpty()) {
//            branches = branches.stream()
//                    .filter(branch -> branch.getName().toLowerCase()
//                            .contains(search.toLowerCase()) ||
//                            branch.getDescription().toLowerCase()
//                                    .contains(search.toLowerCase()))
//                    .collect(Collectors.toList());
//        }
//
//        // Lấy danh sách tỉnh thành để hiển thị filter
//        List<String> provinces = branchService.getActiveBranches().stream()
//                .map(BranchResponse::getProvince)
//                .filter(Objects::nonNull)
//                .distinct()
//                .sorted()
//                .collect(Collectors.toList());
//
//        // **MỚI: Lấy danh sách chi nhánh đã xem gần đây**
//        boolean hasViewHistory = viewHistoryService.hasViewHistory(session);
//        List<BranchResponse> recentlyViewedBranches = new ArrayList<>();
//        List<BranchResponse> suggestedBranches = new ArrayList<>();
//
//        if (hasViewHistory) {
//            recentlyViewedBranches = viewHistoryService.getRecentlyViewedBranches(session, 4);
//            suggestedBranches = viewHistoryService.getSuggestedBranches(session, 4);
//        }
//
//        model.addAttribute("branches", branches);
//        model.addAttribute("provinces", provinces);
//        model.addAttribute("branchTypes", BranchType.values());
//        model.addAttribute("selectedProvince", province);
//        model.addAttribute("selectedType", type);
//        model.addAttribute("searchQuery", search);
//
//        // **MỚI: Thêm lịch sử xem vào model**
//        model.addAttribute("hasViewHistory", hasViewHistory);
//        model.addAttribute("recentlyViewedBranches", recentlyViewedBranches);
//        model.addAttribute("suggestedBranches", suggestedBranches);
//
//        return "index";
//    }

//    @GetMapping("/")
//    public String index(Model model,
//                               @RequestParam(required = false) String province,
//                               @RequestParam(required = false) BranchType type,
//                               @RequestParam(required = false) String search) {
//
//        // Lấy tất cả chi nhánh đang hoạt động
//        List<BranchResponse> branches = branchService.getActiveBranches();
//
//        // Lọc theo tỉnh thành nếu có
//        if (province != null && !province.isEmpty()) {
//            branches = branches.stream()
//                    .filter(branch -> branch.getProvince().toLowerCase()
//                            .contains(province.toLowerCase()))
//                    .collect(Collectors.toList());
//        }
//
//        // Lọc theo loại chi nhánh nếu có
//        if (type != null) {
//            branches = branches.stream()
//                    .filter(branch -> branch.getType() == type)
//                    .collect(Collectors.toList());
//        }
//
//        // Tìm kiếm theo tên nếu có
//        if (search != null && !search.trim().isEmpty()) {
//            branches = branches.stream()
//                    .filter(branch -> branch.getName().toLowerCase()
//                            .contains(search.toLowerCase()) ||
//                            branch.getDescription().toLowerCase()
//                                    .contains(search.toLowerCase()))
//                    .collect(Collectors.toList());
//        }
//
//        // Lấy danh sách tỉnh thành để hiển thị filter
//        List<String> provinces = branchService.getActiveBranches().stream()
//                .map(BranchResponse::getProvince)
//                .filter(Objects::nonNull) // bỏ province null
//                .distinct()
//                .sorted()
//                .collect(Collectors.toList());
//
//
//        model.addAttribute("branches", branches);
//        model.addAttribute("provinces", provinces);
//        model.addAttribute("branchTypes", BranchType.values());
//        model.addAttribute("selectedProvince", province);
//        model.addAttribute("selectedType", type);
//        model.addAttribute("searchQuery", search);
//
//        return "index";
//    }



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