package com.example.htql_nhahang_khachsan.service;

import com.example.htql_nhahang_khachsan.dto.PromotionRequest;


import com.example.htql_nhahang_khachsan.dto.PromotionResponse;
import com.example.htql_nhahang_khachsan.entity.BranchEntity;
import com.example.htql_nhahang_khachsan.entity.PromotionEntity;
import com.example.htql_nhahang_khachsan.enums.PromotionApplicability;
import com.example.htql_nhahang_khachsan.enums.PromotionScope;
import com.example.htql_nhahang_khachsan.enums.Status;
import com.example.htql_nhahang_khachsan.enums.UserRole;
import com.example.htql_nhahang_khachsan.repository.BranchRepository;
import com.example.htql_nhahang_khachsan.repository.PromotionRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class PromotionService {

    private final PromotionRepository promotionRepository;
    private final BranchRepository branchRepository;

    // Lấy tất cả khuyến mãi (Admin)
    public List<PromotionResponse> getAllPromotions() {
        return promotionRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(PromotionResponse::from)
                .collect(Collectors.toList());
    }

    // Lấy khuyến mãi theo chi nhánh (Manager)
    public List<PromotionResponse> getPromotionsByBranch(Long branchId) {
        return promotionRepository.findByBranchId(branchId).stream()
                .map(PromotionResponse::from)
                .collect(Collectors.toList());
    }

    // Tìm kiếm khuyến mãi (Admin)
    public List<PromotionResponse> searchPromotions(String name, PromotionScope scope,
                                                    PromotionApplicability applicability, Status status) {
        return promotionRepository.findByFilters(name, scope, applicability, status).stream()
                .map(PromotionResponse::from)
                .collect(Collectors.toList());
    }

    // Tìm kiếm khuyến mãi theo chi nhánh (Manager)
    public List<PromotionResponse> searchPromotionsByBranch(String name, PromotionScope scope,
                                                            PromotionApplicability applicability,
                                                            Status status, Long branchId) {
        return promotionRepository.findByFiltersAndBranchId(name, scope, applicability, status, branchId).stream()
                .map(PromotionResponse::from)
                .collect(Collectors.toList());
    }

    public PromotionResponse getPromotionById(Long id) {
        PromotionEntity promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khuyến mãi"));
        return PromotionResponse.from(promotion);
    }

//    @Transactional
//    public PromotionResponse createPromotion(PromotionRequest request, Long createdBy) {
//        // Validate
//        validatePromotionRequest(request);
//
//        // Kiểm tra tên đã tồn tại
//        if (promotionRepository.existsByName(request.getName())) {
//            throw new RuntimeException("Tên khuyến mãi đã tồn tại");
//        }
//
//        PromotionEntity promotion = buildPromotionEntity(request, createdBy);
//
////        // Set branches nếu là BRANCH_SPECIFIC
////        if (request.getScope() == PromotionScope.BRANCH_SPECIFIC &&
////                request.getBranchIds() != null && !request.getBranchIds().isEmpty()) {
////            Set<BranchEntity> branches = new HashSet<>(
////                    branchRepository.findAllById(request.getBranchIds())
////            );
////            if (branches.size() != request.getBranchIds().size()) {
////                throw new RuntimeException("Một số chi nhánh không tồn tại");
////            }
////            promotion.setBranches(branches);
////        }
//
//        // Set branches nếu là BRANCH_SPECIFIC
//        if (request.getScope() == PromotionScope.BRANCH_SPECIFIC &&
//                request.getBranchIds() != null && !request.getBranchIds().isEmpty()) {
//
//            List<BranchEntity> foundBranches = branchRepository.findAllById(request.getBranchIds());
//
//            // SỬA: Chỉ cần check có tồn tại các branch được chọn, không cần bằng nhau
//            if (foundBranches.isEmpty()) {
//                throw new RuntimeException("Không tìm thấy chi nhánh nào được chọn");
//            }
//
//            // Check nếu có branch không tồn tại
//            Set<Long> foundIds = foundBranches.stream().map(BranchEntity::getId).collect(Collectors.toSet());
//            boolean hasInvalidBranch = request.getBranchIds().stream()
//                    .anyMatch(id -> !foundIds.contains(id));
//
//            if (hasInvalidBranch) {
//                throw new RuntimeException("Một số chi nhánh được chọn không tồn tại");
//            }
//
//            promotion.setBranches(new HashSet<>(foundBranches));
//        }
//
//        PromotionEntity savedPromotion = promotionRepository.save(promotion);
//        return PromotionResponse.from(savedPromotion);
//    }

    @Transactional
    public PromotionResponse createPromotion(PromotionRequest request, Long createdBy) {
        // Validate chung
        validatePromotionRequest(request);

        // CHỈ CHO ADMIN: nếu là BRANCH_SPECIFIC thì phải chọn ít nhất 1 chi nhánh
        if (request.getScope() == PromotionScope.BRANCH_SPECIFIC) {
            if (request.getBranchIds() == null || request.getBranchIds().isEmpty()) {
                throw new RuntimeException("Phải chọn ít nhất một chi nhánh khi áp dụng cho chi nhánh cụ thể");
            }
        }

        // Kiểm tra tên đã tồn tại
        if (promotionRepository.existsByName(request.getName())) {
            throw new RuntimeException("Tên khuyến mãi đã tồn tại");
        }

        PromotionEntity promotion = buildPromotionEntity(request, createdBy);

        // Set branches nếu là BRANCH_SPECIFIC (giữ nguyên logic đã có)
        if (request.getScope() == PromotionScope.BRANCH_SPECIFIC &&
                request.getBranchIds() != null && !request.getBranchIds().isEmpty()) {

            List<BranchEntity> foundBranches = branchRepository.findAllById(request.getBranchIds());

            if (foundBranches.isEmpty()) {
                throw new RuntimeException("Không tìm thấy chi nhánh nào được chọn");
            }

            Set<Long> foundIds = foundBranches.stream().map(BranchEntity::getId).collect(Collectors.toSet());
            boolean hasInvalidBranch = request.getBranchIds().stream()
                    .anyMatch(id -> !foundIds.contains(id));

            if (hasInvalidBranch) {
                throw new RuntimeException("Một số chi nhánh được chọn không tồn tại");
            }

            promotion.setBranches(new HashSet<>(foundBranches));
        }

        PromotionEntity savedPromotion = promotionRepository.save(promotion);
        return PromotionResponse.from(savedPromotion);
    }


    @Transactional
    public PromotionResponse createPromotionForManager(PromotionRequest request, Long createdBy, Long branchId) {
        // Validate
        validatePromotionRequest(request);

        // Manager chỉ có thể tạo khuyến mãi cho chi nhánh của mình
        if (request.getScope() == PromotionScope.SYSTEM_WIDE) {
            throw new RuntimeException("Manager chỉ có thể tạo khuyến mãi cho chi nhánh cụ thể");
        }

        // Kiểm tra tên đã tồn tại
        if (promotionRepository.existsByName(request.getName())) {
            throw new RuntimeException("Tên khuyến mãi đã tồn tại");
        }

        PromotionEntity promotion = buildPromotionEntity(request, createdBy);


        // Set branch cho manager
        BranchEntity branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chi nhánh"));
        Set<BranchEntity> branches = new HashSet<>();
        branches.add(branch);
        promotion.setBranches(branches);
        promotion.setScope(PromotionScope.BRANCH_SPECIFIC);

        PromotionEntity savedPromotion = promotionRepository.save(promotion);
        return PromotionResponse.from(savedPromotion);
    }

    @Transactional
    public PromotionResponse updatePromotion(Long id, PromotionRequest request) {
        PromotionEntity promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khuyến mãi"));

        // Validate
        validatePromotionRequest(request);

//        // Kiểm tra tên đã tồn tại (trừ chính nó)
//        PromotionEntity existingPromotion = promotionRepository.findAll().stream()
//                .filter(p -> p.getName().equals(request.getName()) && !p.getId().equals(id))
//                .findFirst()
//                .orElse(null);
//
//        if (existingPromotion != null) {
//            throw new RuntimeException("Tên khuyến mãi đã tồn tại");
//        }

        // CHỈ CHO ADMIN: nếu là BRANCH_SPECIFIC thì phải chọn ít nhất 1 chi nhánh
        if (request.getScope() == PromotionScope.BRANCH_SPECIFIC) {
            if (request.getBranchIds() == null || request.getBranchIds().isEmpty()) {
                throw new RuntimeException("Phải chọn ít nhất một chi nhánh khi áp dụng cho chi nhánh cụ thể");
            }
        }

        updatePromotionEntity(promotion, request);

        // Update branches
//        if (request.getScope() == PromotionScope.BRANCH_SPECIFIC &&
//                request.getBranchIds() != null && !request.getBranchIds().isEmpty()) {
//            Set<BranchEntity> branches = new HashSet<>(
//                    branchRepository.findAllById(request.getBranchIds())
//            );
//            if (branches.size() != request.getBranchIds().size()) {
//                throw new RuntimeException("Một số chi nhánh không tồn tại");
//            }
//            promotion.setBranches(branches);
//        } else if (request.getScope() == PromotionScope.SYSTEM_WIDE) {
//            promotion.getBranches().clear();
//        }

        // Update branches
        if (request.getScope() == PromotionScope.BRANCH_SPECIFIC &&
                request.getBranchIds() != null && !request.getBranchIds().isEmpty()) {

            List<BranchEntity> foundBranches = branchRepository.findAllById(request.getBranchIds());

            if (foundBranches.isEmpty()) {
                throw new RuntimeException("Không tìm thấy chi nhánh nào được chọn");
            }

            // Check nếu có branch không tồn tại
            Set<Long> foundIds = foundBranches.stream().map(BranchEntity::getId).collect(Collectors.toSet());
            boolean hasInvalidBranch = request.getBranchIds().stream()
                    .anyMatch(branchId -> !foundIds.contains(branchId));


            if (hasInvalidBranch) {
                throw new RuntimeException("Một số chi nhánh được chọn không tồn tại");
            }

            promotion.setBranches(new HashSet<>(foundBranches));
        } else if (request.getScope() == PromotionScope.SYSTEM_WIDE) {
            promotion.getBranches().clear();
        }

        PromotionEntity updatedPromotion = promotionRepository.save(promotion);
        return PromotionResponse.from(updatedPromotion);
    }

    @Transactional
    public PromotionResponse updatePromotionForManager(Long id, PromotionRequest request, Long branchId) {
        PromotionEntity promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khuyến mãi"));

        // Kiểm tra manager có quyền sửa không (khuyến mãi phải thuộc về chi nhánh của manager)
        boolean hasPermission = promotion.getScope() == PromotionScope.BRANCH_SPECIFIC &&
                promotion.getBranches().stream()
                        .anyMatch(branch -> branch.getId().equals(branchId));

        if (!hasPermission) {
            throw new RuntimeException("Bạn không có quyền sửa khuyến mãi này");
        }

        // Manager chỉ có thể tạo khuyến mãi cho chi nhánh của mình
        if (request.getScope() == PromotionScope.SYSTEM_WIDE) {
            throw new RuntimeException("Manager chỉ có thể tạo khuyến mãi cho chi nhánh cụ thể");
        }

        // Validate
        validatePromotionRequest(request);

        // Kiểm tra tên đã tồn tại (trừ chính nó)
        PromotionEntity existingPromotion = promotionRepository.findAll().stream()
                .filter(p -> p.getName().equals(request.getName()) && !p.getId().equals(id))
                .findFirst()
                .orElse(null);

        if (existingPromotion != null) {
            throw new RuntimeException("Tên khuyến mãi đã tồn tại");
        }

        updatePromotionEntity(promotion, request);

        // Manager chỉ có thể set cho chi nhánh của mình
        BranchEntity branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chi nhánh"));
        Set<BranchEntity> branches = new HashSet<>();
        branches.add(branch);
        promotion.setBranches(branches);
        promotion.setScope(PromotionScope.BRANCH_SPECIFIC);

        PromotionEntity updatedPromotion = promotionRepository.save(promotion);
        return PromotionResponse.from(updatedPromotion);
    }

    public void deletePromotion(Long id) {
        PromotionEntity promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khuyến mãi"));

        // Soft delete - đổi status thành INACTIVE
        promotion.setStatus(Status.INACTIVE);
        promotionRepository.save(promotion);
    }

    public void deletePromotionForManager(Long id, Long branchId) {
        PromotionEntity promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khuyến mãi"));

        // Kiểm tra manager có quyền xóa không
        boolean hasPermission = promotion.getScope() == PromotionScope.BRANCH_SPECIFIC &&
                promotion.getBranches().stream()
                        .anyMatch(branch -> branch.getId().equals(branchId));

        if (!hasPermission) {
            throw new RuntimeException("Bạn không có quyền xóa khuyến mãi này");
        }

        // Soft delete
        promotion.setStatus(Status.INACTIVE);
        promotionRepository.save(promotion);
    }

//    private void validatePromotionRequest(PromotionRequest request) {
//        if (request.getStartDate().isAfter(request.getEndDate())) {
//            throw new RuntimeException("Ngày bắt đầu phải trước ngày kết thúc");
//        }
//
//        if (request.getEndDate().isBefore(LocalDateTime.now())) {
//            throw new RuntimeException("Ngày kết thúc phải sau thời điểm hiện tại");
//        }
//
//        // Validate discount value based on type
//        if (request.getType() == com.example.htql_nhahang_khachsan.enums.PromotionType.PERCENTAGE) {
//            if (request.getDiscountValue().compareTo(java.math.BigDecimal.valueOf(100)) > 0) {
//                throw new RuntimeException("Phần trăm giảm giá không được vượt quá 100%");
//            }
//        }
//    }

    private void validatePromotionRequest(PromotionRequest request) {
        // Validation hiện tại...
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new RuntimeException("Ngày bắt đầu phải trước ngày kết thúc");
        }

        if (request.getEndDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Ngày kết thúc phải sau thời điểm hiện tại");
        }

        // Validate discount value based on type
        if (request.getType() == com.example.htql_nhahang_khachsan.enums.PromotionType.PERCENTAGE) {
            if (request.getDiscountValue().compareTo(java.math.BigDecimal.valueOf(100)) > 0) {
                throw new RuntimeException("Phần trăm giảm giá không được vượt quá 100%");
            }
        }

        // ===== THÊM VALIDATION MỚI =====
//        // Validate branch selection cho BRANCH_SPECIFIC
//        if (request.getScope() == PromotionScope.BRANCH_SPECIFIC) {
//            if (request.getBranchIds() == null || request.getBranchIds().isEmpty()) {
//                throw new RuntimeException("Phải chọn ít nhất một chi nhánh khi áp dụng cho chi nhánh cụ thể");
//            }
//        }
    }

    private PromotionEntity buildPromotionEntity(PromotionRequest request, Long createdBy) {
        return PromotionEntity.builder()
                .name(request.getName())
                .description(request.getDescription())
                .type(request.getType())
                .discountValue(request.getDiscountValue())
                .minAmount(request.getMinAmount())
                .maxDiscount(request.getMaxDiscount())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .usageLimit(request.getUsageLimit())
                .usedCount(0)
                .scope(request.getScope())
                .applicability(request.getApplicability())
                .status(request.getStatus())
                .createdBy(createdBy)
                .build();
    }

    private void updatePromotionEntity(PromotionEntity promotion, PromotionRequest request) {
        promotion.setName(request.getName());
        promotion.setDescription(request.getDescription());
        promotion.setType(request.getType());
        promotion.setDiscountValue(request.getDiscountValue());
        promotion.setMinAmount(request.getMinAmount());
        promotion.setMaxDiscount(request.getMaxDiscount());
        promotion.setStartDate(request.getStartDate());
        promotion.setEndDate(request.getEndDate());
        promotion.setUsageLimit(request.getUsageLimit());
        promotion.setScope(request.getScope());
        promotion.setApplicability(request.getApplicability());
        promotion.setStatus(request.getStatus());
    }
}