package com.example.htql_nhahang_khachsan.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.example.htql_nhahang_khachsan.dto.*;
import com.example.htql_nhahang_khachsan.entity.UserEntity;
import com.example.htql_nhahang_khachsan.service.*;
import org.springframework.data.domain.Page;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class RoomTypeDetailController {

    private final RoomService roomService;
    private final BranchService branchService;
    private final ReviewService reviewService;
    private final AuthService authService;

    @GetMapping("/room-types/{id}")
    public String showRoomTypeDetail(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            Model model,
            HttpSession session) {
        try {
            // Lấy thông tin loại phòng
            RoomTypeResponse roomType = roomService.getRoomTypeById(id);
            if (roomType == null) {
                return "redirect:/";
            }

            model.addAttribute("roomType", roomType);

            // Lấy thông tin chi nhánh
            BranchResponse branch = branchService.getBranchById(roomType.getBranchId());
            model.addAttribute("branch", branch);

            // Lấy danh sách hình ảnh của loại phòng
            List<RoomImageResponse> roomImages = roomService.getRoomImagesByRoomType(id);
            model.addAttribute("roomImages", roomImages);

            // Lấy danh sách phòng cùng loại trong chi nhánh
            List<RoomTypeResponse> similarRoomTypes = roomService.getSimilarRoomTypes(id, roomType.getBranchId());
            model.addAttribute("similarRoomTypes", similarRoomTypes);

            // Lấy danh sách phòng có sẵn của loại này
            List<RoomResponse> availableRooms = roomService.getAvailableRoomsByType(id);
            model.addAttribute("availableRooms", availableRooms);

            // ✅ QUAN TRỌNG: Lấy thống kê đánh giá theo LOẠI PHÒNG (không phải branch)
            ReviewService.ReviewStatistics reviewStats = reviewService.getReviewStatistics(id);
            model.addAttribute("reviewStats", reviewStats);

            System.out.println("=== DEBUG REVIEW STATS ===");
            System.out.println("Room Type ID: " + id);
            System.out.println("Total Reviews: " + reviewStats.getTotalReviews());
            System.out.println("Average Rating: " + reviewStats.getAverageRating());

            // ✅ QUAN TRỌNG: Lấy danh sách đánh giá theo LOẠI PHÒNG với phân trang
            Page<ReviewResponse> reviews = reviewService.getReviewsByRoomType(id, page, 5);
            model.addAttribute("reviews", reviews);

            System.out.println("=== DEBUG REVIEWS PAGE ===");
            System.out.println("Total Elements: " + reviews.getTotalElements());
            System.out.println("Number of Elements: " + reviews.getNumberOfElements());
            System.out.println("Has Content: " + reviews.hasContent());
            System.out.println("Content Size: " + reviews.getContent().size());

            // ✅ Kiểm tra user đã đăng nhập VÀ là CUSTOMER
            boolean isLoggedIn = authService.isLoggedIn(session);
            boolean isCustomer = authService.isCustomer(session);

            model.addAttribute("isLoggedIn", isLoggedIn);
            model.addAttribute("isCustomer", isCustomer);

            if (isLoggedIn && isCustomer) {
                UserEntity currentUser = authService.getCurrentUser(session);
                model.addAttribute("currentUser", currentUser);
                model.addAttribute("currentUsername", currentUser.getUsername());

                // Kiểm tra có thể review không
                boolean canReview = reviewService.canUserReview(id, currentUser.getUsername());
                model.addAttribute("canReview", canReview);

                // Lấy review của user nếu có
                ReviewResponse userReview = reviewService.getUserReviewForRoomType(id, currentUser.getUsername());
                model.addAttribute("userReview", userReview);

                System.out.println("=== DEBUG USER REVIEW ===");
                System.out.println("Username: " + currentUser.getUsername());
                System.out.println("Can Review: " + canReview);
                System.out.println("User Review: " + (userReview != null ? userReview.getId() : "null"));
            } else {
                model.addAttribute("canReview", false);
                model.addAttribute("currentUsername", "");
            }

            return "customer/room-types/detail";
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("=== ERROR IN CONTROLLER ===");
            System.err.println(e.getMessage());
            return "redirect:/";
        }
    }

    // API để lấy giá phòng theo ngày
    @GetMapping("/api/room-types/{id}/pricing")
    @ResponseBody
    public RoomPricingResponse getRoomPricing(
            @PathVariable Long id,
            @RequestParam String checkInDate,
            @RequestParam String checkOutDate) {
        return roomService.calculateRoomPricing(id, checkInDate, checkOutDate);
    }
}