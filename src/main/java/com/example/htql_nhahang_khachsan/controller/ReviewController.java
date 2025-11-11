package com.example.htql_nhahang_khachsan.controller;

import com.example.htql_nhahang_khachsan.dto.ReviewRequest;
import com.example.htql_nhahang_khachsan.dto.ReviewResponse;
import com.example.htql_nhahang_khachsan.entity.UserEntity;
import com.example.htql_nhahang_khachsan.service.AuthService;
import com.example.htql_nhahang_khachsan.service.ReviewService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final AuthService authService;

    /**
     * Lấy danh sách đánh giá của loại phòng
     */
    @GetMapping("/room-type/{roomTypeId}")
    public ResponseEntity<Page<ReviewResponse>> getReviewsByRoomType(
            @PathVariable Long roomTypeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<ReviewResponse> reviews = reviewService.getReviewsByRoomType(roomTypeId, page, size);
        return ResponseEntity.ok(reviews);
    }

    /**
     * Lấy thống kê đánh giá
     */
    @GetMapping("/room-type/{roomTypeId}/statistics")
    public ResponseEntity<ReviewService.ReviewStatistics> getReviewStatistics(
            @PathVariable Long roomTypeId) {

        ReviewService.ReviewStatistics stats = reviewService.getReviewStatistics(roomTypeId);
        return ResponseEntity.ok(stats);
    }

    /**
     * ✅ FIXED: Tạo đánh giá mới - kiểm tra isCustomer
     */
    @PostMapping
    public ResponseEntity<?> createReview(
            @Valid @RequestBody ReviewRequest request,
            HttpSession session) {

        // Kiểm tra đăng nhập
        if (!authService.isLoggedIn(session)) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Bạn cần đăng nhập để đánh giá");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        // ✅ Kiểm tra là CUSTOMER
        if (!authService.isCustomer(session)) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Chỉ khách hàng mới có thể đánh giá");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        }

        try {
            UserEntity currentUser = authService.getCurrentUser(session);

            // Kiểm tra có quyền đánh giá không (đã trải nghiệm chưa)
            if (!reviewService.canUserReview(request.getBranchId(), currentUser.getUsername())) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Bạn cần trải nghiệm dịch vụ trước khi đánh giá");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }

            ReviewResponse review = reviewService.createReview(request, currentUser.getUsername());
            return ResponseEntity.status(HttpStatus.CREATED).body(review);
        } catch (IllegalStateException e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Có lỗi xảy ra: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * ✅ FIXED: Cập nhật đánh giá - kiểm tra isCustomer
     */
    @PutMapping("/{reviewId}")
    public ResponseEntity<?> updateReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewRequest request,
            HttpSession session) {

        if (!authService.isLoggedIn(session)) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Bạn cần đăng nhập");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        // ✅ Kiểm tra là CUSTOMER
        if (!authService.isCustomer(session)) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Chỉ khách hàng mới có thể sửa đánh giá");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        }

        try {
            UserEntity currentUser = authService.getCurrentUser(session);
            ReviewResponse review = reviewService.updateReview(reviewId, request, currentUser.getUsername());
            return ResponseEntity.ok(review);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * ✅ FIXED: Xóa đánh giá - kiểm tra isCustomer
     */
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<?> deleteReview(
            @PathVariable Long reviewId,
            HttpSession session) {

        if (!authService.isLoggedIn(session)) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Bạn cần đăng nhập");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        // ✅ Kiểm tra là CUSTOMER
        if (!authService.isCustomer(session)) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Chỉ khách hàng mới có thể xóa đánh giá");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        }

        try {
            UserEntity currentUser = authService.getCurrentUser(session);
            reviewService.deleteReview(reviewId, currentUser.getUsername());
            Map<String, String> response = new HashMap<>();
            response.put("message", "Xóa đánh giá thành công");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * ✅ FIXED: Kiểm tra user có thể đánh giá không - kiểm tra isCustomer
     */
    @GetMapping("/room-type/{roomTypeId}/can-review")
    public ResponseEntity<Map<String, Boolean>> canUserReview(
            @PathVariable Long roomTypeId,
            HttpSession session) {

        Map<String, Boolean> response = new HashMap<>();

        if (!authService.isLoggedIn(session) || !authService.isCustomer(session)) {
            response.put("canReview", false);
            return ResponseEntity.ok(response);
        }

        UserEntity currentUser = authService.getCurrentUser(session);
        boolean canReview = reviewService.canUserReview(roomTypeId, currentUser.getUsername());
        response.put("canReview", canReview);
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy đánh giá của user cho loại phòng
     */
    @GetMapping("/room-type/{roomTypeId}/user-review")
    public ResponseEntity<?> getUserReview(
            @PathVariable Long roomTypeId,
            HttpSession session) {

        if (!authService.isLoggedIn(session) || !authService.isCustomer(session)) {
            return ResponseEntity.ok(null);
        }

        UserEntity currentUser = authService.getCurrentUser(session);
        ReviewResponse review = reviewService.getUserReviewForRoomType(roomTypeId, currentUser.getUsername());
        return ResponseEntity.ok(review);
    }
}