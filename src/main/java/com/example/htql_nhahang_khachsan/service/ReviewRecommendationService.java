//package com.example.htql_nhahang_khachsan.service;
//
//import com.example.htql_nhahang_khachsan.dto.BranchResponse;
//import com.example.htql_nhahang_khachsan.dto.MenuItemResponse;
//import com.example.htql_nhahang_khachsan.dto.RoomTypeResponse;
//import com.example.htql_nhahang_khachsan.entity.BranchEntity;
//import com.example.htql_nhahang_khachsan.entity.MenuItemEntity;
//import com.example.htql_nhahang_khachsan.entity.RoomTypeEntity;
//import com.example.htql_nhahang_khachsan.enums.ReviewStatus;
//import com.example.htql_nhahang_khachsan.repository.*;
//import lombok.AllArgsConstructor;
//import lombok.Data;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//
//import java.util.*;
//import java.util.stream.Collectors;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class ReviewRecommendationService {
//
//    private final ReviewRepository reviewRepository;
//    private final BranchRepository branchRepository;
//    private final RoomTypeRepository roomTypeRepository;
//    private final MenuItemRepository menuItemRepository;
//
//    /**
//     * Lấy top 3 chi nhánh được đánh giá cao nhất
//     * Sử dụng Bayesian Average để xử lý chi nhánh có ít đánh giá
//     */
//    public List<BranchResponse> getTop3RecommendedBranches() {
//        log.info("=== Tính toán top 3 chi nhánh được đánh giá cao nhất ===");
//
//        // Lấy thống kê đánh giá của tất cả chi nhánh
//        Map<Long, BranchRatingStats> branchStats = new HashMap<>();
//
//        List<BranchEntity> allBranches = branchRepository.findByStatus(
//                com.example.htql_nhahang_khachsan.enums.BranchStatus.ACTIVE);
//
//        for (BranchEntity branch : allBranches) {
//            Double avgRating = reviewRepository.getAverageRatingByBranchAndType(
//                    branch.getId(), null, ReviewStatus.APPROVED);
//            Long reviewCount = reviewRepository.countByBranchIdAndTypeAndStatus(
//                    branch.getId(), null, ReviewStatus.APPROVED);
//
//            if (avgRating != null && reviewCount != null && reviewCount > 0) {
//                branchStats.put(branch.getId(),
//                        new BranchRatingStats(branch.getId(), avgRating, reviewCount));
//            }
//        }
//
//        if (branchStats.isEmpty()) {
//            log.info("Không có chi nhánh nào có đánh giá");
//            return Collections.emptyList();
//        }
//
//        // Tính rating trung bình toàn hệ thống (C)
//        double totalRating = 0;
//        long totalCount = 0;
//        for (BranchRatingStats stats : branchStats.values()) {
//            totalRating += stats.getAvgRating() * stats.getReviewCount();
//            totalCount += stats.getReviewCount();
//        }
//        double C = totalCount > 0 ? totalRating / totalCount : 0;
//        final int m = 3; // Ngưỡng tối thiểu
//
//        log.info("Rating trung bình hệ thống (C): {}, Tổng reviews: {}", C, totalCount);
//
//        // Tính Bayesian Average Score cho mỗi chi nhánh
//        List<ScoredItem> scoredBranches = branchStats.values().stream()
//                .map(stats -> {
//                    double R = stats.getAvgRating();
//                    long v = stats.getReviewCount();
//                    // Công thức Bayesian: (v/(v+m)) * R + (m/(v+m)) * C
//                    double bayesScore = ((double)v / (v + m)) * R + ((double)m / (v + m)) * C;
//                    return new ScoredItem(stats.getBranchId(), bayesScore);
//                })
//                .sorted(Comparator.comparing(ScoredItem::getScore).reversed())
//                .limit(3)
//                .collect(Collectors.toList());
//
//        // Lấy thông tin chi tiết chi nhánh
//        List<Long> topBranchIds = scoredBranches.stream()
//                .map(ScoredItem::getItemId)
//                .collect(Collectors.toList());
//
//        List<BranchEntity> topBranches = branchRepository.findAllById(topBranchIds);
//
//        // Sắp xếp lại theo thứ tự score
//        Map<Long, BranchEntity> branchMap = topBranches.stream()
//                .collect(Collectors.toMap(BranchEntity::getId, b -> b));
//
//        return scoredBranches.stream()
//                .map(scored -> BranchResponse.from(branchMap.get(scored.getItemId())))
//                .filter(Objects::nonNull)
//                .collect(Collectors.toList());
//    }
//
//    /**
//     * Lấy top 3 loại phòng được đánh giá cao nhất
//     */
//    public List<RoomTypeResponse> getTop3RecommendedRoomTypes() {
//        log.info("=== Tính toán top 3 loại phòng được đánh giá cao nhất ===");
//
//        // Lấy tất cả loại phòng active
//        List<RoomTypeEntity> allRoomTypes = roomTypeRepository.findByStatusTrueOrderByPriceAsc();
//        Map<Long, RoomTypeRatingStats> roomTypeStats = new HashMap<>();
//
//        for (RoomTypeEntity roomType : allRoomTypes) {
//            Double avgRating = reviewRepository.getAverageRatingByRoomType(
//                    roomType.getId(), ReviewStatus.APPROVED);
//            Long reviewCount = reviewRepository.countByRoomTypeIdAndStatus(
//                    roomType.getId(), ReviewStatus.APPROVED);
//
//            if (avgRating != null && reviewCount != null && reviewCount > 0) {
//                roomTypeStats.put(roomType.getId(),
//                        new RoomTypeRatingStats(roomType.getId(), avgRating, reviewCount));
//            }
//        }
//
//        if (roomTypeStats.isEmpty()) {
//            log.info("Không có loại phòng nào có đánh giá");
//            return Collections.emptyList();
//        }
//
//        // Tính rating trung bình toàn hệ thống
//        double totalRating = 0;
//        long totalCount = 0;
//        for (RoomTypeRatingStats stats : roomTypeStats.values()) {
//            totalRating += stats.getAvgRating() * stats.getReviewCount();
//            totalCount += stats.getReviewCount();
//        }
//        double C = totalCount > 0 ? totalRating / totalCount : 0;
//        final int m = 2;
//
//        // Tính Bayesian Score
//        List<ScoredItem> scoredRoomTypes = roomTypeStats.values().stream()
//                .map(stats -> {
//                    double R = stats.getAvgRating();
//                    long v = stats.getReviewCount();
//                    double bayesScore = ((double)v / (v + m)) * R + ((double)m / (v + m)) * C;
//                    return new ScoredItem(stats.getRoomTypeId(), bayesScore);
//                })
//                .sorted(Comparator.comparing(ScoredItem::getScore).reversed())
//                .limit(3)
//                .collect(Collectors.toList());
//
//        // Lấy thông tin chi tiết
//        List<Long> topIds = scoredRoomTypes.stream()
//                .map(ScoredItem::getItemId)
//                .collect(Collectors.toList());
//
//        List<RoomTypeEntity> topRoomTypes = roomTypeRepository.findAllById(topIds);
//
//        // Convert sang Response và giữ thứ tự
//        Map<Long, RoomTypeEntity> roomTypeMap = topRoomTypes.stream()
//                .collect(Collectors.toMap(RoomTypeEntity::getId, rt -> rt));
//
//        return scoredRoomTypes.stream()
//                .map(scored -> {
//                    RoomTypeEntity entity = roomTypeMap.get(scored.getItemId());
//                    if (entity != null) {
//                        return RoomTypeResponse.from(entity);
//                    }
//                    return null;
//                })
//                .filter(Objects::nonNull)
//                .collect(Collectors.toList());
//    }
//
//    /**
//     * Lấy top 3 món ăn được đánh giá cao nhất
//     */
//    public List<MenuItemResponse> getTop3RecommendedMenuItems() {
//        log.info("=== Tính toán top 3 món ăn được đánh giá cao nhất ===");
//
//        // Lấy tất cả món ăn available
//        List<MenuItemEntity> allMenuItems = menuItemRepository.findByIsAvailableTrue();
//        Map<Long, MenuItemRatingStats> menuItemStats = new HashMap<>();
//
//        for (MenuItemEntity menuItem : allMenuItems) {
//            Double avgRating = reviewRepository.getAverageRatingByMenuItem(
//                    menuItem.getId(), ReviewStatus.APPROVED);
//            Long reviewCount = reviewRepository.countByMenuItemIdAndStatus(
//                    menuItem.getId(), ReviewStatus.APPROVED);
//
//            if (avgRating != null && reviewCount != null && reviewCount > 0) {
//                menuItemStats.put(menuItem.getId(),
//                        new MenuItemRatingStats(menuItem.getId(), avgRating, reviewCount));
//            }
//        }
//
//        if (menuItemStats.isEmpty()) {
//            log.info("Không có món ăn nào có đánh giá");
//            return Collections.emptyList();
//        }
//
//        // Tính rating trung bình toàn hệ thống
//        double totalRating = 0;
//        long totalCount = 0;
//        for (MenuItemRatingStats stats : menuItemStats.values()) {
//            totalRating += stats.getAvgRating() * stats.getReviewCount();
//            totalCount += stats.getReviewCount();
//        }
//        double C = totalCount > 0 ? totalRating / totalCount : 0;
//        final int m = 2;
//
//        // Tính Bayesian Score
//        List<ScoredItem> scoredMenuItems = menuItemStats.values().stream()
//                .map(stats -> {
//                    double R = stats.getAvgRating();
//                    long v = stats.getReviewCount();
//                    double bayesScore = ((double)v / (v + m)) * R + ((double)m / (v + m)) * C;
//                    return new ScoredItem(stats.getMenuItemId(), bayesScore);
//                })
//                .sorted(Comparator.comparing(ScoredItem::getScore).reversed())
//                .limit(3)
//                .collect(Collectors.toList());
//
//        // Lấy thông tin chi tiết
//        List<Long> topIds = scoredMenuItems.stream()
//                .map(ScoredItem::getItemId)
//                .collect(Collectors.toList());
//
//        List<MenuItemEntity> topMenuItems = menuItemRepository.findAllById(topIds);
//
//        // Convert sang Response và giữ thứ tự
//        Map<Long, MenuItemEntity> menuItemMap = topMenuItems.stream()
//                .collect(Collectors.toMap(MenuItemEntity::getId, mi -> mi));
//
//        return scoredMenuItems.stream()
//                .map(scored -> {
//                    MenuItemEntity entity = menuItemMap.get(scored.getItemId());
//                    if (entity != null) {
//                        return MenuItemResponse.from(entity);
//                    }
//                    return null;
//                })
//                .filter(Objects::nonNull)
//                .collect(Collectors.toList());
//    }
//
//    // ==================== INNER CLASSES ====================
//
//    @Data
//    @AllArgsConstructor
//    private static class ScoredItem {
//        private Long itemId;
//        private Double score;
//    }
//
//    @Data
//    @AllArgsConstructor
//    private static class BranchRatingStats {
//        private Long branchId;
//        private Double avgRating;
//        private Long reviewCount;
//    }
//
//    @Data
//    @AllArgsConstructor
//    private static class RoomTypeRatingStats {
//        private Long roomTypeId;
//        private Double avgRating;
//        private Long reviewCount;
//    }
//
//    @Data
//    @AllArgsConstructor
//    private static class MenuItemRatingStats {
//        private Long menuItemId;
//        private Double avgRating;
//        private Long reviewCount;
//    }
//}

package com.example.htql_nhahang_khachsan.service;

import com.example.htql_nhahang_khachsan.dto.BranchResponse;
import com.example.htql_nhahang_khachsan.dto.MenuItemResponse;
import com.example.htql_nhahang_khachsan.dto.RoomTypeResponse;
import com.example.htql_nhahang_khachsan.entity.BranchEntity;
import com.example.htql_nhahang_khachsan.entity.MenuItemEntity;
import com.example.htql_nhahang_khachsan.entity.RoomTypeEntity;
import com.example.htql_nhahang_khachsan.enums.ReviewStatus;
import com.example.htql_nhahang_khachsan.repository.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewRecommendationService {

    private final ReviewRepository reviewRepository;
    private final BranchRepository branchRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final MenuItemRepository menuItemRepository;

    /**
     * ✅ Lấy top 3 chi nhánh được đánh giá cao nhất
     * Sử dụng Bayesian Average để xử lý chi nhánh có ít đánh giá
     * KẾT QUẢ TRẢ VỀ BAO GỒM: Rating trung bình thực tế + Số lượng reviews
     */
    public List<ItemWithRating<BranchResponse>> getTop3RecommendedBranches() {
        log.info("=== Tính toán top 3 chi nhánh được đánh giá cao nhất ===");

        // Lấy thống kê đánh giá của tất cả chi nhánh
        Map<Long, BranchRatingStats> branchStats = new HashMap<>();

        List<BranchEntity> allBranches = branchRepository.findByStatus(
                com.example.htql_nhahang_khachsan.enums.BranchStatus.ACTIVE);

        for (BranchEntity branch : allBranches) {
            // ✅ Lấy rating trung bình THỰC TẾ từ database
            Double avgRating = reviewRepository.getAverageRatingByBranchAndType(
                    branch.getId(), null, ReviewStatus.APPROVED);
            Long reviewCount = reviewRepository.countByBranchIdAndTypeAndStatus(
                    branch.getId(), null, ReviewStatus.APPROVED);

            if (avgRating != null && reviewCount != null && reviewCount > 0) {
                branchStats.put(branch.getId(),
                        new BranchRatingStats(branch.getId(), avgRating, reviewCount));
            }
        }

        if (branchStats.isEmpty()) {
            log.info("Không có chi nhánh nào có đánh giá");
            return Collections.emptyList();
        }

        // Tính rating trung bình toàn hệ thống (C) - dùng cho Bayesian Average
        double totalRating = 0;
        long totalCount = 0;
        for (BranchRatingStats stats : branchStats.values()) {
            totalRating += stats.getAvgRating() * stats.getReviewCount();
            totalCount += stats.getReviewCount();
        }
        double C = totalCount > 0 ? totalRating / totalCount : 0;
        final int m = 3; // Ngưỡng tối thiểu để ổn định thống kê

        log.info("Rating trung bình hệ thống (C): {}, Tổng reviews: {}", C, totalCount);

        // Tính Bayesian Average Score để XẾP HẠNG công bằng
        // (Chi nhánh có ít review sẽ không bị thiệt thòi so với chi nhánh có nhiều review)
        List<ScoredItem> scoredBranches = branchStats.values().stream()
                .map(stats -> {
                    double R = stats.getAvgRating();
                    long v = stats.getReviewCount();
                    // Công thức Bayesian: (v/(v+m)) * R + (m/(v+m)) * C
                    double bayesScore = ((double)v / (v + m)) * R + ((double)m / (v + m)) * C;
                    return new ScoredItem(stats.getBranchId(), bayesScore);
                })
                .sorted(Comparator.comparing(ScoredItem::getScore).reversed())
                .limit(3)
                .collect(Collectors.toList());

        // Lấy thông tin chi tiết chi nhánh
        List<Long> topBranchIds = scoredBranches.stream()
                .map(ScoredItem::getItemId)
                .collect(Collectors.toList());

        List<BranchEntity> topBranches = branchRepository.findAllById(topBranchIds);

        // ✅ TRẢ VỀ KÈM RATING TRUNG BÌNH THỰC TẾ (không phải Bayesian score)
        Map<Long, BranchEntity> branchMap = topBranches.stream()
                .collect(Collectors.toMap(BranchEntity::getId, b -> b));

        return scoredBranches.stream()
                .map(scored -> {
                    BranchEntity entity = branchMap.get(scored.getItemId());
                    if (entity == null) return null;

                    BranchRatingStats stats = branchStats.get(scored.getItemId());

                    // ✅ TRẢ VỀ: BranchResponse + Rating trung bình + Số reviews
                    return new ItemWithRating<>(
                            BranchResponse.from(entity),
                            stats.getAvgRating(),  // ✅ Rating trung bình THỰC TẾ để hiển thị
                            stats.getReviewCount().intValue()
                    );
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * ✅ Lấy top 3 loại phòng được đánh giá cao nhất
     * KẾT QUẢ TRẢ VỀ BAO GỒM: Rating trung bình thực tế + Số lượng reviews
     */
    public List<ItemWithRating<RoomTypeResponse>> getTop3RecommendedRoomTypes() {
        log.info("=== Tính toán top 3 loại phòng được đánh giá cao nhất ===");

        List<RoomTypeEntity> allRoomTypes = roomTypeRepository.findByStatusTrueOrderByPriceAsc();
        Map<Long, RoomTypeRatingStats> roomTypeStats = new HashMap<>();

        for (RoomTypeEntity roomType : allRoomTypes) {
            // ✅ Lấy rating trung bình THỰC TẾ
            Double avgRating = reviewRepository.getAverageRatingByRoomType(
                    roomType.getId(), ReviewStatus.APPROVED);
            Long reviewCount = reviewRepository.countByRoomTypeIdAndStatus(
                    roomType.getId(), ReviewStatus.APPROVED);

            if (avgRating != null && reviewCount != null && reviewCount > 0) {
                roomTypeStats.put(roomType.getId(),
                        new RoomTypeRatingStats(roomType.getId(), avgRating, reviewCount));
            }
        }

        if (roomTypeStats.isEmpty()) {
            log.info("Không có loại phòng nào có đánh giá");
            return Collections.emptyList();
        }

        // Tính rating trung bình toàn hệ thống
        double totalRating = 0;
        long totalCount = 0;
        for (RoomTypeRatingStats stats : roomTypeStats.values()) {
            totalRating += stats.getAvgRating() * stats.getReviewCount();
            totalCount += stats.getReviewCount();
        }
        double C = totalCount > 0 ? totalRating / totalCount : 0;
        final int m = 2;

        // Tính Bayesian Score để XẾP HẠNG
        List<ScoredItem> scoredRoomTypes = roomTypeStats.values().stream()
                .map(stats -> {
                    double R = stats.getAvgRating();
                    long v = stats.getReviewCount();
                    double bayesScore = ((double)v / (v + m)) * R + ((double)m / (v + m)) * C;
                    return new ScoredItem(stats.getRoomTypeId(), bayesScore);
                })
                .sorted(Comparator.comparing(ScoredItem::getScore).reversed())
                .limit(3)
                .collect(Collectors.toList());

        // Lấy thông tin chi tiết
        List<Long> topIds = scoredRoomTypes.stream()
                .map(ScoredItem::getItemId)
                .collect(Collectors.toList());

        List<RoomTypeEntity> topRoomTypes = roomTypeRepository.findAllById(topIds);

        // ✅ TRẢ VỀ KÈM RATING TRUNG BÌNH THỰC TẾ
        Map<Long, RoomTypeEntity> roomTypeMap = topRoomTypes.stream()
                .collect(Collectors.toMap(RoomTypeEntity::getId, rt -> rt));

        return scoredRoomTypes.stream()
                .map(scored -> {
                    RoomTypeEntity entity = roomTypeMap.get(scored.getItemId());
                    if (entity == null) return null;

                    RoomTypeRatingStats stats = roomTypeStats.get(scored.getItemId());

                    // ✅ TRẢ VỀ: RoomTypeResponse + Rating trung bình + Số reviews
                    return new ItemWithRating<>(
                            RoomTypeResponse.from(entity),
                            stats.getAvgRating(),  // ✅ Rating trung bình THỰC TẾ
                            stats.getReviewCount().intValue()
                    );
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * ✅ Lấy top 3 món ăn được đánh giá cao nhất
     * KẾT QUẢ TRẢ VỀ BAO GỒM: Rating trung bình thực tế + Số lượng reviews
     */
    public List<ItemWithRating<MenuItemResponse>> getTop3RecommendedMenuItems() {
        log.info("=== Tính toán top 3 món ăn được đánh giá cao nhất ===");

        List<MenuItemEntity> allMenuItems = menuItemRepository.findByIsAvailableTrue();
        Map<Long, MenuItemRatingStats> menuItemStats = new HashMap<>();

        for (MenuItemEntity menuItem : allMenuItems) {
            // ✅ Lấy rating trung bình THỰC TẾ
            Double avgRating = reviewRepository.getAverageRatingByMenuItem(
                    menuItem.getId(), ReviewStatus.APPROVED);
            Long reviewCount = reviewRepository.countByMenuItemIdAndStatus(
                    menuItem.getId(), ReviewStatus.APPROVED);

            if (avgRating != null && reviewCount != null && reviewCount > 0) {
                menuItemStats.put(menuItem.getId(),
                        new MenuItemRatingStats(menuItem.getId(), avgRating, reviewCount));
            }
        }

        if (menuItemStats.isEmpty()) {
            log.info("Không có món ăn nào có đánh giá");
            return Collections.emptyList();
        }

        // Tính rating trung bình toàn hệ thống
        double totalRating = 0;
        long totalCount = 0;
        for (MenuItemRatingStats stats : menuItemStats.values()) {
            totalRating += stats.getAvgRating() * stats.getReviewCount();
            totalCount += stats.getReviewCount();
        }
        double C = totalCount > 0 ? totalRating / totalCount : 0;
        final int m = 2;

        // Tính Bayesian Score để XẾP HẠNG
        List<ScoredItem> scoredMenuItems = menuItemStats.values().stream()
                .map(stats -> {
                    double R = stats.getAvgRating();
                    long v = stats.getReviewCount();
                    double bayesScore = ((double)v / (v + m)) * R + ((double)m / (v + m)) * C;
                    return new ScoredItem(stats.getMenuItemId(), bayesScore);
                })
                .sorted(Comparator.comparing(ScoredItem::getScore).reversed())
                .limit(3)
                .collect(Collectors.toList());

        // Lấy thông tin chi tiết
        List<Long> topIds = scoredMenuItems.stream()
                .map(ScoredItem::getItemId)
                .collect(Collectors.toList());

        List<MenuItemEntity> topMenuItems = menuItemRepository.findAllById(topIds);

        // ✅ TRẢ VỀ KÈM RATING TRUNG BÌNH THỰC TẾ
        Map<Long, MenuItemEntity> menuItemMap = topMenuItems.stream()
                .collect(Collectors.toMap(MenuItemEntity::getId, mi -> mi));

        return scoredMenuItems.stream()
                .map(scored -> {
                    MenuItemEntity entity = menuItemMap.get(scored.getItemId());
                    if (entity == null) return null;

                    MenuItemRatingStats stats = menuItemStats.get(scored.getItemId());

                    // ✅ TRẢ VỀ: MenuItemResponse + Rating trung bình + Số reviews
                    return new ItemWithRating<>(
                            MenuItemResponse.from(entity),
                            stats.getAvgRating(),  // ✅ Rating trung bình THỰC TẾ
                            stats.getReviewCount().intValue()
                    );
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    // ==================== INNER CLASSES ====================

    /**
     * ✅ Wrapper class chứa: Item + Rating trung bình + Số reviews
     * Dùng để hiển thị ra ngoài giao diện
     */
    @Data
    @AllArgsConstructor
    public static class ItemWithRating<T> {
        private T item;                 // BranchResponse / RoomTypeResponse / MenuItemResponse
        private Double averageRating;   // ✅ Rating trung bình THỰC TẾ (VD: 4.8)
        private Integer reviewCount;    // ✅ Số lượng reviews (VD: 156)

        /**
         * Lấy số sao đầy đủ (VD: rating 4.8 -> 4 sao đầy)
         */
        public int getFullStars() {
            return averageRating != null ? averageRating.intValue() : 0;
        }

        /**
         * Kiểm tra có nửa sao không (VD: rating 4.5 -> có nửa sao)
         */
        public boolean hasHalfStar() {
            if (averageRating == null) return false;
            double decimal = averageRating - averageRating.intValue();
            return decimal >= 0.3 && decimal < 0.8;
        }

        /**
         * Lấy số sao rỗng
         */
        public int getEmptyStars() {
            int fullStars = getFullStars();
            int halfStar = hasHalfStar() ? 1 : 0;
            return 5 - fullStars - halfStar;
        }

        /**
         * Format rating để hiển thị (VD: 4.8 -> "4.8")
         */
        public String getFormattedRating() {
            if (averageRating == null) return "0.0";
            return String.format("%.1f", averageRating);
        }
    }

    @Data
    @AllArgsConstructor
    private static class ScoredItem {
        private Long itemId;
        private Double score;  // Bayesian score (chỉ dùng để xếp hạng, không hiển thị)
    }

    @Data
    @AllArgsConstructor
    private static class BranchRatingStats {
        private Long branchId;
        private Double avgRating;
        private Long reviewCount;
    }

    @Data
    @AllArgsConstructor
    private static class RoomTypeRatingStats {
        private Long roomTypeId;
        private Double avgRating;
        private Long reviewCount;
    }

    @Data
    @AllArgsConstructor
    private static class MenuItemRatingStats {
        private Long menuItemId;
        private Double avgRating;
        private Long reviewCount;
    }
}