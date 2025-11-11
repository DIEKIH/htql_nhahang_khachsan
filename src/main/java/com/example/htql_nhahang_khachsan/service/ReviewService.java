//package com.example.htql_nhahang_khachsan.service;
//
//import com.example.htql_nhahang_khachsan.dto.ReviewRequest;
//import com.example.htql_nhahang_khachsan.dto.ReviewResponse;
//import com.example.htql_nhahang_khachsan.entity.*;
//import com.example.htql_nhahang_khachsan.enums.BookingStatus;
//import com.example.htql_nhahang_khachsan.enums.ReviewStatus;
//import com.example.htql_nhahang_khachsan.enums.ReviewType;
//import com.example.htql_nhahang_khachsan.repository.*;
//import jakarta.persistence.EntityNotFoundException;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.domain.Sort;
//import org.springframework.security.access.AccessDeniedException;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.util.List;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class ReviewService {
//
//    private final ReviewRepository reviewRepository;
//    private final UserRepository userRepository;
//    private final BranchRepository branchRepository;
//    private final RoomBookingRepository roomBookingRepository;
//    private final RoomTypeRepository roomTypeRepository;
//
//    /**
//     * Lấy danh sách đánh giá của loại phòng (chỉ lấy APPROVED)
//     */
//    public Page<ReviewResponse> getReviewsByRoomType(Long roomTypeId, int page, int size) {
//        RoomTypeEntity roomType = roomTypeRepository.findById(roomTypeId)
//                .orElseThrow(() -> new EntityNotFoundException("Room type not found"));
//
//        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
//
//        Page<ReviewEntity> reviews = reviewRepository
//                .findByBranchIdAndTypeAndStatus(roomType.getBranch().getId(),
//                        ReviewType.ROOM, ReviewStatus.APPROVED, pageable);
//
//        return reviews.map(this::mapToReviewResponse);
//    }
//
//    /**
//     * Lấy thống kê đánh giá của loại phòng
//     */
//    public ReviewStatistics getReviewStatistics(Long roomTypeId) {
//        RoomTypeEntity roomType = roomTypeRepository.findById(roomTypeId)
//                .orElseThrow(() -> new EntityNotFoundException("Room type not found"));
//
//        List<ReviewEntity> reviews = reviewRepository
//                .findByBranchIdAndTypeAndStatus(roomType.getBranch().getId(),
//                        ReviewType.ROOM, ReviewStatus.APPROVED);
//
//        if (reviews.isEmpty()) {
//            return new ReviewStatistics(0.0, 0, 0, 0, 0, 0, 0);
//        }
//
//        double averageRating = reviews.stream()
//                .mapToInt(ReviewEntity::getRating)
//                .average()
//                .orElse(0.0);
//
//        int total = reviews.size();
//        int rating5 = (int) reviews.stream().filter(r -> r.getRating() == 5).count();
//        int rating4 = (int) reviews.stream().filter(r -> r.getRating() == 4).count();
//        int rating3 = (int) reviews.stream().filter(r -> r.getRating() == 3).count();
//        int rating2 = (int) reviews.stream().filter(r -> r.getRating() == 2).count();
//        int rating1 = (int) reviews.stream().filter(r -> r.getRating() == 1).count();
//
//        return new ReviewStatistics(
//                Math.round(averageRating * 10.0) / 10.0,
//                total, rating5, rating4, rating3, rating2, rating1
//        );
//    }
//
//    /**
//     * Tạo đánh giá mới
//     * Tự động link với booking đã hoàn thành qua email/phone
//     */
//    @Transactional
//    public ReviewResponse createReview(ReviewRequest request, String username) {
//        UserEntity customer = userRepository.findByUsername(username)
//                .orElseThrow(() -> new EntityNotFoundException("User not found"));
//
//        BranchEntity branch = branchRepository.findById(request.getBranchId())
//                .orElseThrow(() -> new EntityNotFoundException("Branch not found"));
//
//        // Kiểm tra đã review chưa
//        if (reviewRepository.existsByCustomerIdAndBranchIdAndType(
//                customer.getId(), branch.getId(), request.getType())) {
//            throw new IllegalStateException("Bạn đã đánh giá chi nhánh này rồi");
//        }
//
//        // Tìm booking đã hoàn thành qua email/phone để link
//        RoomBookingEntity linkedBooking = null;
//        if (request.getType() == ReviewType.ROOM) {
//            List<RoomBookingEntity> completedBookings = roomBookingRepository
//                    .findCompletedBookingsByEmailOrPhone(
//                            customer.getEmail(),
//                            customer.getPhoneNumber());
//
//            // Lấy booking đầu tiên chưa được review
//            for (RoomBookingEntity booking : completedBookings) {
//                if (booking.getBranch().getId().equals(branch.getId()) &&
//                        !reviewRepository.existsByRoomBookingIdAndCustomerId(
//                                booking.getId(), customer.getId())) {
//                    linkedBooking = booking;
//                    break;
//                }
//            }
//        }
//
//        ReviewEntity review = ReviewEntity.builder()
//                .customer(customer)
//                .branch(branch)
//                .type(request.getType())
//                .rating(request.getRating())
//                .comment(request.getComment())
//                .status(ReviewStatus.APPROVED) // Auto approve
//                .createdAt(LocalDateTime.now())
//                .build();
//
//        if (linkedBooking != null) {
//            review.setRoomBooking(linkedBooking);
//        }
//
//        ReviewEntity savedReview = reviewRepository.save(review);
//        return mapToReviewResponse(savedReview);
//    }
//
//    /**
//     * Cập nhật đánh giá
//     */
//    @Transactional
//    public ReviewResponse updateReview(Long reviewId, ReviewRequest request, String username) {
//        ReviewEntity review = reviewRepository.findById(reviewId)
//                .orElseThrow(() -> new EntityNotFoundException("Review not found"));
//
//        UserEntity customer = userRepository.findByUsername(username)
//                .orElseThrow(() -> new EntityNotFoundException("User not found"));
//
//        // Chỉ cho phép chủ nhân đánh giá sửa
//        if (!review.getCustomer().getId().equals(customer.getId())) {
//            throw new AccessDeniedException("Bạn chỉ có thể sửa đánh giá của mình");
//        }
//
//        review.setRating(request.getRating());
//        review.setComment(request.getComment());
//
//        ReviewEntity updatedReview = reviewRepository.save(review);
//        return mapToReviewResponse(updatedReview);
//    }
//
//    /**
//     * Xóa đánh giá
//     */
//    @Transactional
//    public void deleteReview(Long reviewId, String username) {
//        ReviewEntity review = reviewRepository.findById(reviewId)
//                .orElseThrow(() -> new EntityNotFoundException("Review not found"));
//
//        UserEntity customer = userRepository.findByUsername(username)
//                .orElseThrow(() -> new EntityNotFoundException("User not found"));
//
//        // Chỉ cho phép chủ nhân đánh giá xóa
//        if (!review.getCustomer().getId().equals(customer.getId())) {
//            throw new AccessDeniedException("Bạn chỉ có thể xóa đánh giá của mình");
//        }
//
//        reviewRepository.delete(review);
//    }
//
//    /**
//     * ✅ FIXED: Kiểm tra xem user có thể đánh giá không
//     * - So sánh email/phone của user với guestEmail/guestPhone trong booking
//     * - Kiểm tra booking đã COMPLETED và checkOutDate < hôm nay
//     */
//    public boolean canUserReview(Long roomTypeId, String username) {
//        log.info("=== Kiểm tra quyền đánh giá ===");
//        log.info("RoomTypeId: {}, Username: {}", roomTypeId, username);
//
//        UserEntity customer = userRepository.findByUsername(username)
//                .orElse(null);
//
//        if (customer == null) {
//            log.warn("User không tồn tại");
//            return false;
//        }
//
//        log.info("Customer Email: {}", customer.getEmail());
//        log.info("Customer Phone: {}", customer.getPhoneNumber());
//
//        RoomTypeEntity roomType = roomTypeRepository.findById(roomTypeId)
//                .orElseThrow(() -> new EntityNotFoundException("Room type not found"));
//
//        Long branchId = roomType.getBranch().getId();
//        log.info("BranchId: {}", branchId);
//
//        // Kiểm tra xem đã review branch này chưa
//        boolean hasReviewed = reviewRepository.existsByCustomerIdAndBranchIdAndType(
//                customer.getId(), branchId, ReviewType.ROOM);
//
//        if (hasReviewed) {
//            log.info("Đã review branch này rồi");
//            return false;
//        }
//
//        // ✅ FIX: So sánh với guestEmail và guestPhone trong booking
//        String userEmail = customer.getEmail();
//        String userPhone = customer.getPhoneNumber();
//
//        // Lấy tất cả booking completed của branch này
//        List<RoomBookingEntity> completedBookings = roomBookingRepository
//                .findCompletedBookingsByBranchAndStatus(branchId, BookingStatus.CONFIRMED);
//
//        log.info("Tổng số booking COMPLETED của branch: {}", completedBookings.size());
//
//        // Kiểm tra có booking nào match với email hoặc phone không
//        boolean hasCompletedBooking = completedBookings.stream().anyMatch(booking -> {
//            boolean emailMatch = booking.getGuestEmail() != null &&
//                    booking.getGuestEmail().equalsIgnoreCase(userEmail);
//            boolean phoneMatch = booking.getGuestPhone() != null &&
//                    booking.getGuestPhone().equals(userPhone);
//            boolean isCheckoutPassed = booking.getCheckOutDate().isBefore(LocalDate.now());
//
//            log.info("Booking #{}: email={}, phone={}, checkOut={}, emailMatch={}, phoneMatch={}, checkoutPassed={}",
//                    booking.getId(),
//                    booking.getGuestEmail(),
//                    booking.getGuestPhone(),
//                    booking.getCheckOutDate(),
//                    emailMatch,
//                    phoneMatch,
//                    isCheckoutPassed);
//
//            return (emailMatch || phoneMatch) && isCheckoutPassed;
//        });
//
//        log.info("Kết quả kiểm tra: canReview={}", hasCompletedBooking);
//        return hasCompletedBooking;
//    }
//
//    /**
//     * Lấy đánh giá của user cho loại phòng
//     */
//    public ReviewResponse getUserReviewForRoomType(Long roomTypeId, String username) {
//        UserEntity customer = userRepository.findByUsername(username)
//                .orElse(null);
//
//        if (customer == null) {
//            return null;
//        }
//
//        RoomTypeEntity roomType = roomTypeRepository.findById(roomTypeId)
//                .orElseThrow(() -> new EntityNotFoundException("Room type not found"));
//
//        // Tìm review của user này cho branch này
//        ReviewEntity review = reviewRepository
//                .findByCustomerIdAndBranchIdAndType(
//                        customer.getId(),
//                        roomType.getBranch().getId(),
//                        ReviewType.ROOM)
//                .orElse(null);
//
//        return review != null ? mapToReviewResponse(review) : null;
//    }
//
//    // Helper methods
//    private ReviewResponse mapToReviewResponse(ReviewEntity entity) {
//        return ReviewResponse.builder()
//                .id(entity.getId())
//                .customerId(entity.getCustomer().getId())
//                .customerName(entity.getCustomer().getFullName())
//                .customerAvatar(entity.getCustomer().getAvatarUrl())
//                .branchId(entity.getBranch().getId())
//                .type(entity.getType())
//                .rating(entity.getRating())
//                .comment(entity.getComment())
//                .staffResponse(entity.getStaffResponse())
//                .status(entity.getStatus())
//                .createdAt(entity.getCreatedAt())
//                .responseDate(entity.getResponseDate())
//                .build();
//    }
//
//    // Inner class for statistics
//    public static class ReviewStatistics {
//        public double averageRating;
//        public int totalReviews;
//        public int rating5Count;
//        public int rating4Count;
//        public int rating3Count;
//        public int rating2Count;
//        public int rating1Count;
//
//        public ReviewStatistics(double averageRating, int totalReviews,
//                                int rating5Count, int rating4Count, int rating3Count,
//                                int rating2Count, int rating1Count) {
//            this.averageRating = averageRating;
//            this.totalReviews = totalReviews;
//            this.rating5Count = rating5Count;
//            this.rating4Count = rating4Count;
//            this.rating3Count = rating3Count;
//            this.rating2Count = rating2Count;
//            this.rating1Count = rating1Count;
//        }
//
//        // Getters
//        public double getAverageRating() { return averageRating; }
//        public int getTotalReviews() { return totalReviews; }
//        public int getRating5Count() { return rating5Count; }
//        public int getRating4Count() { return rating4Count; }
//        public int getRating3Count() { return rating3Count; }
//        public int getRating2Count() { return rating2Count; }
//        public int getRating1Count() { return rating1Count; }
//    }
//}


package com.example.htql_nhahang_khachsan.service;

import com.example.htql_nhahang_khachsan.dto.ReviewRequest;
import com.example.htql_nhahang_khachsan.dto.ReviewResponse;
import com.example.htql_nhahang_khachsan.entity.*;
import com.example.htql_nhahang_khachsan.enums.BookingStatus;
import com.example.htql_nhahang_khachsan.enums.ReviewStatus;
import com.example.htql_nhahang_khachsan.enums.ReviewType;
import com.example.htql_nhahang_khachsan.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final RoomBookingRepository roomBookingRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final MenuItemRepository menuItemRepository;

    // ==================== LOẠI PHÒNG ====================

    /**
     * ✅ Lấy danh sách đánh giá của LOẠI PHÒNG (có phân trang)
     */
    public Page<ReviewResponse> getReviewsByRoomType(Long roomTypeId, int page, int size) {
        roomTypeRepository.findById(roomTypeId)
                .orElseThrow(() -> new EntityNotFoundException("Room type not found"));

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<ReviewEntity> reviews = reviewRepository
                .findByRoomTypeIdAndStatus(roomTypeId, ReviewStatus.APPROVED, pageable);

        return reviews.map(this::mapToReviewResponse);
    }

    /**
     * ✅ Lấy thống kê đánh giá của LOẠI PHÒNG
     */
    public ReviewStatistics getReviewStatistics(Long roomTypeId) {
        roomTypeRepository.findById(roomTypeId)
                .orElseThrow(() -> new EntityNotFoundException("Room type not found"));

        List<ReviewEntity> reviews = reviewRepository
                .findByRoomTypeIdAndStatus(roomTypeId, ReviewStatus.APPROVED);

        return calculateStatistics(reviews);
    }

    /**
     * ✅ Lấy đánh giá của user cho LOẠI PHÒNG
     */
    public ReviewResponse getUserReviewForRoomType(Long roomTypeId, String username) {
        UserEntity customer = userRepository.findByUsername(username).orElse(null);
        if (customer == null) {
            return null;
        }

        roomTypeRepository.findById(roomTypeId)
                .orElseThrow(() -> new EntityNotFoundException("Room type not found"));

        ReviewEntity review = reviewRepository
                .findByCustomerIdAndRoomTypeId(customer.getId(), roomTypeId)
                .orElse(null);

        return review != null ? mapToReviewResponse(review) : null;
    }

    /**
     * ✅ Kiểm tra có thể đánh giá LOẠI PHÒNG không
     */
    public boolean canUserReview(Long roomTypeId, String username) {
        log.info("=== Kiểm tra quyền đánh giá loại phòng ===");
        log.info("RoomTypeId: {}, Username: {}", roomTypeId, username);

        UserEntity customer = userRepository.findByUsername(username).orElse(null);
        if (customer == null) {
            log.warn("User không tồn tại");
            return false;
        }

        // Kiểm tra đã review loại phòng này chưa
        if (reviewRepository.existsByCustomerIdAndRoomTypeId(customer.getId(), roomTypeId)) {
            log.info("Đã review loại phòng này rồi");
            return false;
        }

        RoomTypeEntity roomType = roomTypeRepository.findById(roomTypeId)
                .orElseThrow(() -> new EntityNotFoundException("Room type not found"));

        Long branchId = roomType.getBranch().getId();
        String userEmail = customer.getEmail();
        String userPhone = customer.getPhoneNumber();

        List<RoomBookingEntity> completedBookings = roomBookingRepository
                .findCompletedBookingsByBranchAndStatus(branchId, BookingStatus.CONFIRMED);

        log.info("Tổng booking completed của branch: {}", completedBookings.size());

        boolean hasCompletedBooking = completedBookings.stream().anyMatch(booking -> {
            boolean emailMatch = booking.getGuestEmail() != null &&
                    booking.getGuestEmail().equalsIgnoreCase(userEmail);
            boolean phoneMatch = booking.getGuestPhone() != null &&
                    booking.getGuestPhone().equals(userPhone);
            boolean isCheckoutPassed = booking.getCheckOutDate().isBefore(LocalDate.now());

            return (emailMatch || phoneMatch) && isCheckoutPassed;
        });

        log.info("Kết quả: canReview={}", hasCompletedBooking);
        return hasCompletedBooking;
    }

    // ==================== MÓN ĂN ====================

    /**
     * ✅ Lấy danh sách đánh giá của MÓN ĂN (có phân trang)
     */
    public Page<ReviewResponse> getReviewsByMenuItem(Long menuItemId, int page, int size) {
        menuItemRepository.findById(menuItemId)
                .orElseThrow(() -> new EntityNotFoundException("Menu item not found"));

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<ReviewEntity> reviews = reviewRepository
                .findByMenuItemIdAndStatus(menuItemId, ReviewStatus.APPROVED, pageable);

        return reviews.map(this::mapToReviewResponse);
    }

    /**
     * ✅ Lấy thống kê đánh giá của MÓN ĂN
     */
    public ReviewStatistics getMenuItemReviewStatistics(Long menuItemId) {
        menuItemRepository.findById(menuItemId)
                .orElseThrow(() -> new EntityNotFoundException("Menu item not found"));

        List<ReviewEntity> reviews = reviewRepository
                .findByMenuItemIdAndStatus(menuItemId, ReviewStatus.APPROVED);

        return calculateStatistics(reviews);
    }

    /**
     * ✅ Lấy đánh giá của user cho MÓN ĂN
     */
    public ReviewResponse getUserReviewForMenuItem(Long menuItemId, String username) {
        UserEntity customer = userRepository.findByUsername(username).orElse(null);
        if (customer == null) {
            return null;
        }

        menuItemRepository.findById(menuItemId)
                .orElseThrow(() -> new EntityNotFoundException("Menu item not found"));

        ReviewEntity review = reviewRepository
                .findByCustomerIdAndMenuItemId(customer.getId(), menuItemId)
                .orElse(null);

        return review != null ? mapToReviewResponse(review) : null;
    }

    /**
     * ✅ Kiểm tra có thể đánh giá MÓN ĂN không
     */
    public boolean canUserReviewMenuItem(Long menuItemId, String username) {
        log.info("=== Kiểm tra quyền đánh giá món ăn ===");
        log.info("MenuItemId: {}, Username: {}", menuItemId, username);

        UserEntity customer = userRepository.findByUsername(username).orElse(null);
        if (customer == null) {
            return false;
        }

        // Kiểm tra đã review món này chưa
        if (reviewRepository.existsByCustomerIdAndMenuItemId(customer.getId(), menuItemId)) {
            log.info("Đã review món ăn này rồi");
            return false;
        }

        // TODO: Kiểm tra đã order món này chưa (cần OrderDetailRepository)
        // Tạm thời cho phép đánh giá nếu chưa review
        return true;
    }

    // ==================== TẠO/SỬA/XÓA REVIEW ====================

    /**
     * ✅ Tạo đánh giá mới
     */
    @Transactional
    public ReviewResponse createReview(ReviewRequest request, String username) {
        UserEntity customer = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        BranchEntity branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new EntityNotFoundException("Branch not found"));

        ReviewEntity review = ReviewEntity.builder()
                .customer(customer)
                .branch(branch)
                .type(request.getType())
                .rating(request.getRating())
                .comment(request.getComment())
                .status(ReviewStatus.APPROVED)
                .createdAt(LocalDateTime.now())
                .build();

        if (request.getType() == ReviewType.ROOM) {
            // Đánh giá LOẠI PHÒNG
            if (request.getRoomTypeId() == null) {
                throw new IllegalStateException("Room type ID is required for room review");
            }

            RoomTypeEntity roomType = roomTypeRepository.findById(request.getRoomTypeId())
                    .orElseThrow(() -> new EntityNotFoundException("Room type not found"));

            // Kiểm tra đã review loại phòng này chưa
            if (reviewRepository.existsByCustomerIdAndRoomTypeId(customer.getId(), roomType.getId())) {
                throw new IllegalStateException("Bạn đã đánh giá loại phòng này rồi");
            }

            review.setRoomType(roomType);

            // Link với booking nếu có
            List<RoomBookingEntity> completedBookings = roomBookingRepository
                    .findCompletedBookingsByEmailOrPhone(customer.getEmail(), customer.getPhoneNumber());

            for (RoomBookingEntity booking : completedBookings) {
                if (booking.getBranch().getId().equals(branch.getId()) &&
                        !reviewRepository.existsByRoomBookingIdAndCustomerId(booking.getId(), customer.getId())) {
                    review.setRoomBooking(booking);
                    break;
                }
            }

        } else if (request.getType() == ReviewType.RESTAURANT) {
            // Đánh giá MÓN ĂN
            if (request.getMenuItemId() == null) {
                throw new IllegalStateException("Menu item ID is required for restaurant review");
            }

            MenuItemEntity menuItem = menuItemRepository.findById(request.getMenuItemId())
                    .orElseThrow(() -> new EntityNotFoundException("Menu item not found"));

            // Kiểm tra đã review món này chưa
            if (reviewRepository.existsByCustomerIdAndMenuItemId(customer.getId(), menuItem.getId())) {
                throw new IllegalStateException("Bạn đã đánh giá món ăn này rồi");
            }

            review.setMenuItem(menuItem);
        }

        ReviewEntity savedReview = reviewRepository.save(review);
        return mapToReviewResponse(savedReview);
    }

    /**
     * ✅ Cập nhật đánh giá
     */
    @Transactional
    public ReviewResponse updateReview(Long reviewId, ReviewRequest request, String username) {
        ReviewEntity review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("Review not found"));

        UserEntity customer = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (!review.getCustomer().getId().equals(customer.getId())) {
            throw new AccessDeniedException("Bạn chỉ có thể sửa đánh giá của mình");
        }

        review.setRating(request.getRating());
        review.setComment(request.getComment());

        ReviewEntity updatedReview = reviewRepository.save(review);
        return mapToReviewResponse(updatedReview);
    }

    /**
     * ✅ Xóa đánh giá
     */
    @Transactional
    public void deleteReview(Long reviewId, String username) {
        ReviewEntity review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("Review not found"));

        UserEntity customer = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (!review.getCustomer().getId().equals(customer.getId())) {
            throw new AccessDeniedException("Bạn chỉ có thể xóa đánh giá của mình");
        }

        reviewRepository.delete(review);
    }

    // ==================== HELPER METHODS ====================

    private ReviewResponse mapToReviewResponse(ReviewEntity entity) {
        return ReviewResponse.builder()
                .id(entity.getId())
                .customerId(entity.getCustomer().getId())
                .customerName(entity.getCustomer().getFullName())
                .customerAvatar(entity.getCustomer().getAvatarUrl())
                .branchId(entity.getBranch().getId())
                .type(entity.getType())
                .rating(entity.getRating())
                .comment(entity.getComment())
                .staffResponse(entity.getStaffResponse())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .responseDate(entity.getResponseDate())
                .build();
    }

    /**
     * ✅ Tính toán thống kê từ danh sách review
     */
    private ReviewStatistics calculateStatistics(List<ReviewEntity> reviews) {
        if (reviews.isEmpty()) {
            return new ReviewStatistics(0.0, 0, 0, 0, 0, 0, 0);
        }

        double averageRating = reviews.stream()
                .mapToInt(ReviewEntity::getRating)
                .average()
                .orElse(0.0);

        int total = reviews.size();
        int rating5 = (int) reviews.stream().filter(r -> r.getRating() == 5).count();
        int rating4 = (int) reviews.stream().filter(r -> r.getRating() == 4).count();
        int rating3 = (int) reviews.stream().filter(r -> r.getRating() == 3).count();
        int rating2 = (int) reviews.stream().filter(r -> r.getRating() == 2).count();
        int rating1 = (int) reviews.stream().filter(r -> r.getRating() == 1).count();

        return new ReviewStatistics(
                Math.round(averageRating * 10.0) / 10.0,
                total, rating5, rating4, rating3, rating2, rating1
        );
    }

    // ==================== INNER CLASS ====================

    public static class ReviewStatistics {
        public double averageRating;
        public int totalReviews;
        public int rating5Count;
        public int rating4Count;
        public int rating3Count;
        public int rating2Count;
        public int rating1Count;

        public ReviewStatistics(double averageRating, int totalReviews,
                                int rating5Count, int rating4Count, int rating3Count,
                                int rating2Count, int rating1Count) {
            this.averageRating = averageRating;
            this.totalReviews = totalReviews;
            this.rating5Count = rating5Count;
            this.rating4Count = rating4Count;
            this.rating3Count = rating3Count;
            this.rating2Count = rating2Count;
            this.rating1Count = rating1Count;
        }

        public double getAverageRating() { return averageRating; }
        public int getTotalReviews() { return totalReviews; }
        public int getRating5Count() { return rating5Count; }
        public int getRating4Count() { return rating4Count; }
        public int getRating3Count() { return rating3Count; }
        public int getRating2Count() { return rating2Count; }
        public int getRating1Count() { return rating1Count; }
    }
}