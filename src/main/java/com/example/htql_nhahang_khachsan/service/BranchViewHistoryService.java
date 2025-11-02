package com.example.htql_nhahang_khachsan.service;

import com.example.htql_nhahang_khachsan.dto.BranchResponse;
import com.example.htql_nhahang_khachsan.entity.BranchEntity;
import com.example.htql_nhahang_khachsan.entity.BranchViewHistoryEntity;
import com.example.htql_nhahang_khachsan.entity.UserEntity;
import com.example.htql_nhahang_khachsan.enums.BranchStatus;
import com.example.htql_nhahang_khachsan.repository.BranchRepository;
import com.example.htql_nhahang_khachsan.repository.BranchViewHistoryRepository;
import com.example.htql_nhahang_khachsan.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BranchViewHistoryService {

    private final BranchViewHistoryRepository viewHistoryRepository;
    private final BranchRepository branchRepository;
    private final UserRepository userRepository;

    /**
     * Ghi nhận lượt xem chi nhánh
     * @param branchId ID chi nhánh
     * @param userId ID người dùng (null nếu chưa đăng nhập)
     * @param sessionId Session ID (cho người chưa đăng nhập)
     */
    @Transactional
    public void recordBranchView(Long branchId, Long userId, String sessionId) {
        BranchEntity branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chi nhánh"));

        BranchViewHistoryEntity viewHistory;

        if (userId != null) {
            // Người dùng đã đăng nhập
            Optional<BranchViewHistoryEntity> existingView =
                    viewHistoryRepository.findByUserIdAndBranchId(userId, branchId);

            if (existingView.isPresent()) {
                viewHistory = existingView.get();
                viewHistory.incrementViewCount();
            } else {
                UserEntity user = userRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

                viewHistory = BranchViewHistoryEntity.builder()
                        .user(user)
                        .branch(branch)
                        .viewCount(1)
                        .lastViewedAt(LocalDateTime.now())
                        .build();
            }
        } else {
            // Người dùng chưa đăng nhập - lưu theo session
            Optional<BranchViewHistoryEntity> existingView =
                    viewHistoryRepository.findBySessionIdAndBranchId(sessionId, branchId);

            if (existingView.isPresent()) {
                viewHistory = existingView.get();
                viewHistory.incrementViewCount();
            } else {
                viewHistory = BranchViewHistoryEntity.builder()
                        .sessionId(sessionId)
                        .branch(branch)
                        .viewCount(1)
                        .lastViewedAt(LocalDateTime.now())
                        .build();
            }
        }

        viewHistoryRepository.save(viewHistory);
    }

    /**
     * Lấy danh sách chi nhánh đã xem gần đây
     * @param userId ID người dùng (null nếu chưa đăng nhập)
     * @param sessionId Session ID
     * @param limit Số lượng chi nhánh tối đa
     * @return Danh sách chi nhánh đã xem
     */
    public List<BranchResponse> getRecentlyViewedBranches(Long userId, String sessionId, int limit) {
        List<BranchViewHistoryEntity> viewHistory;

        if (userId != null) {
            viewHistory = viewHistoryRepository.findByUserIdOrderByLastViewedAtDesc(userId);
        } else {
            viewHistory = viewHistoryRepository.findBySessionIdOrderByLastViewedAtDesc(sessionId);
        }

        return viewHistory.stream()
                .limit(limit)
                .map(h -> BranchResponse.from(h.getBranch()))
                .collect(Collectors.toList());
    }

    /**
     * Lấy gợi ý chi nhánh dựa trên lịch sử xem
     * @param userId ID người dùng
     * @param sessionId Session ID
     * @param limit Số lượng gợi ý
     * @return Danh sách chi nhánh gợi ý
     */
    public List<BranchResponse> getSuggestedBranches(Long userId, String sessionId, int limit) {
        List<BranchViewHistoryEntity> viewHistory;

        if (userId != null) {
            viewHistory = viewHistoryRepository.findByUserIdOrderByLastViewedAtDesc(userId);
        } else {
            viewHistory = viewHistoryRepository.findBySessionIdOrderByLastViewedAtDesc(sessionId);
        }

        if (viewHistory.isEmpty()) {
            // Nếu chưa có lịch sử, trả về các chi nhánh phổ biến
            return branchRepository.findByStatus(com.example.htql_nhahang_khachsan.enums.BranchStatus.ACTIVE)
                    .stream()
                    .limit(limit)
                    .map(BranchResponse::from)
                    .collect(Collectors.toList());
        }

        // Lấy tỉnh/loại chi nhánh từ lịch sử xem
        String commonProvince = viewHistory.stream()
                .map(h -> h.getBranch().getProvince())
                .findFirst()
                .orElse(null);

        // Gợi ý chi nhánh cùng tỉnh hoặc cùng loại
        return branchRepository.findByStatus(com.example.htql_nhahang_khachsan.enums.BranchStatus.ACTIVE)
                .stream()
                .filter(b -> b.getProvince().equals(commonProvince) ||
                        viewHistory.stream().anyMatch(h -> h.getBranch().getType() == b.getType()))
                .filter(b -> viewHistory.stream().noneMatch(h -> h.getBranch().getId().equals(b.getId())))
                .limit(limit)
                .map(BranchResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Chuyển lịch sử xem từ session sang user khi đăng nhập
     * @param userId ID người dùng
     * @param sessionId Session ID
     */
    @Transactional
    public void mergeSessionHistoryToUser(Long userId, String sessionId) {
        List<BranchViewHistoryEntity> sessionHistory =
                viewHistoryRepository.findBySessionId(sessionId);

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        for (BranchViewHistoryEntity sessionView : sessionHistory) {
            Optional<BranchViewHistoryEntity> userView =
                    viewHistoryRepository.findByUserIdAndBranchId(userId, sessionView.getBranch().getId());

            if (userView.isPresent()) {
                // Đã có lịch sử của user, cập nhật thời gian và số lần xem
                BranchViewHistoryEntity existing = userView.get();
                existing.setViewCount(existing.getViewCount() + sessionView.getViewCount());
                existing.setLastViewedAt(sessionView.getLastViewedAt());
                viewHistoryRepository.save(existing);
                viewHistoryRepository.delete(sessionView);
            } else {
                // Chuyển session sang user
                sessionView.setUser(user);
                sessionView.setSessionId(null);
                viewHistoryRepository.save(sessionView);
            }
        }
    }

    /**
     * Kiểm tra có lịch sử xem hay không
     * @param userId ID người dùng
     * @param sessionId Session ID
     * @return true nếu có lịch sử
     */
    public boolean hasViewHistory(Long userId, String sessionId) {
        if (userId != null) {
            return !viewHistoryRepository.findByUserIdOrderByLastViewedAtDesc(userId).isEmpty();
        } else {
            return !viewHistoryRepository.findBySessionIdOrderByLastViewedAtDesc(sessionId).isEmpty();
        }
    }

    /**
     * Xóa lịch sử xem cũ (chạy tự động mỗi ngày)
     */
    @Scheduled(cron = "0 0 2 * * ?") // 2h sáng mỗi ngày
    @Transactional
    public void cleanupOldSessionHistory() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        viewHistoryRepository.deleteOldSessionHistory(cutoffDate);
    }

    /**
     * Lấy số lượt xem của chi nhánh
     * @param branchId ID chi nhánh
     * @return Tổng số lượt xem
     */
    public Long getTotalViews(Long branchId) {
        return viewHistoryRepository.countTotalViewsByBranchId(branchId);
    }


        private final AuthService authService;

        /**
         * Ghi nhận lượt xem chi nhánh
         */
        @Transactional
        public void recordBranchView(Long branchId, HttpSession session) {
            BranchEntity branch = branchRepository.findById(branchId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy chi nhánh"));

            // Kiểm tra user đã đăng nhập chưa
            if (authService.isLoggedIn(session)) {
                recordViewForLoggedInUser(branch, session);
            } else {
                recordViewForGuest(branch, session);
            }
        }

        /**
         * Ghi nhận lượt xem cho user đã đăng nhập
         */
        private void recordViewForLoggedInUser(BranchEntity branch, HttpSession session) {
            Long userId = authService.getCurrentUserId(session);

            Optional<BranchViewHistoryEntity> existingView =
                    viewHistoryRepository.findByUserIdAndBranchId(userId, branch.getId());

            if (existingView.isPresent()) {
                // Đã xem rồi, tăng số lần xem
                BranchViewHistoryEntity history = existingView.get();
                history.incrementViewCount();
                viewHistoryRepository.save(history);
            } else {
                // Lần đầu xem
                UserEntity user = userRepository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

                BranchViewHistoryEntity newHistory = BranchViewHistoryEntity.builder()
                        .user(user)
                        .branch(branch)
                        .viewCount(1)
                        .lastViewedAt(LocalDateTime.now())
                        .build();

                viewHistoryRepository.save(newHistory);
            }
        }

        /**
         * Ghi nhận lượt xem cho khách (chưa đăng nhập)
         */
        private void recordViewForGuest(BranchEntity branch, HttpSession session) {
            String sessionId = session.getId();

            Optional<BranchViewHistoryEntity> existingView =
                    viewHistoryRepository.findBySessionIdAndBranchId(sessionId, branch.getId());

            if (existingView.isPresent()) {
                BranchViewHistoryEntity history = existingView.get();
                history.incrementViewCount();
                viewHistoryRepository.save(history);
            } else {
                BranchViewHistoryEntity newHistory = BranchViewHistoryEntity.builder()
                        .sessionId(sessionId)
                        .branch(branch)
                        .viewCount(1)
                        .lastViewedAt(LocalDateTime.now())
                        .build();

                viewHistoryRepository.save(newHistory);
            }
        }

        /**
         * Lấy danh sách chi nhánh đã xem gần đây
         * @param limit số lượng chi nhánh tối đa
         */
        public List<BranchResponse> getRecentlyViewedBranches(HttpSession session, int limit) {
            List<BranchViewHistoryEntity> histories;

            if (authService.isLoggedIn(session)) {
                Long userId = authService.getCurrentUserId(session);
                histories = viewHistoryRepository.findByUserIdOrderByLastViewedAtDesc(userId);
            } else {
                String sessionId = session.getId();
                histories = viewHistoryRepository.findBySessionIdOrderByLastViewedAtDesc(sessionId);
            }

            return histories.stream()
                    .limit(limit)
                    .map(history -> history.getBranch())
                    .filter(branch -> branch.getStatus() == BranchStatus.ACTIVE)
                    .map(BranchResponse::from)
                    .collect(Collectors.toList());
        }

        /**
         * Lấy gợi ý chi nhánh dựa trên lịch sử xem
         * - Ưu tiên chi nhánh cùng tỉnh/thành phố
         * - Ưu tiên chi nhánh cùng loại (hotel/restaurant)
         */
        public List<BranchResponse> getSuggestedBranches(HttpSession session, int limit) {
            List<BranchResponse> recentlyViewed = getRecentlyViewedBranches(session, 5);

            if (recentlyViewed.isEmpty()) {
                // Nếu chưa xem chi nhánh nào, trả về các chi nhánh phổ biến
                return branchRepository.findByStatus(BranchStatus.ACTIVE).stream()
                        .limit(limit)
                        .map(BranchResponse::from)
                        .collect(Collectors.toList());
            }

            // Lấy tỉnh/thành phố từ chi nhánh gần nhất
            String recentProvince = recentlyViewed.get(0).getProvince();

            // Lấy ID các chi nhánh đã xem để loại trừ
            List<Long> viewedBranchIds = recentlyViewed.stream()
                    .map(BranchResponse::getId)
                    .collect(Collectors.toList());

            // Tìm chi nhánh cùng tỉnh/thành phố, chưa xem
            List<BranchResponse> suggestions = branchRepository.findByStatus(BranchStatus.ACTIVE).stream()
                    .filter(branch -> !viewedBranchIds.contains(branch.getId()))
                    .filter(branch -> branch.getProvince().equalsIgnoreCase(recentProvince))
                    .map(BranchResponse::from)
                    .limit(limit)
                    .collect(Collectors.toList());

            // Nếu không đủ, bổ sung thêm chi nhánh khác
            if (suggestions.size() < limit) {
                int remaining = limit - suggestions.size();
                List<BranchResponse> additionalBranches = branchRepository.findByStatus(BranchStatus.ACTIVE).stream()
                        .filter(branch -> !viewedBranchIds.contains(branch.getId()))
                        .filter(branch -> !branch.getProvince().equalsIgnoreCase(recentProvince))
                        .map(BranchResponse::from)
                        .limit(remaining)
                        .collect(Collectors.toList());

                suggestions.addAll(additionalBranches);
            }

            return suggestions;
        }

        /**
         * Kiểm tra có lịch sử xem không
         */
        public boolean hasViewHistory(HttpSession session) {
            if (authService.isLoggedIn(session)) {
                Long userId = authService.getCurrentUserId(session);
                List<BranchViewHistoryEntity> histories =
                        viewHistoryRepository.findByUserIdOrderByLastViewedAtDesc(userId);
                return !histories.isEmpty();
            } else {
                String sessionId = session.getId();
                List<BranchViewHistoryEntity> histories =
                        viewHistoryRepository.findBySessionIdOrderByLastViewedAtDesc(sessionId);
                return !histories.isEmpty();
            }
        }

}