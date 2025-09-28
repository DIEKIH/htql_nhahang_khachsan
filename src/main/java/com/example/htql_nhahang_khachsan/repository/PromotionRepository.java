package com.example.htql_nhahang_khachsan.repository;

import com.example.htql_nhahang_khachsan.entity.PromotionEntity;
import com.example.htql_nhahang_khachsan.enums.PromotionApplicability;
import com.example.htql_nhahang_khachsan.enums.PromotionScope;
import com.example.htql_nhahang_khachsan.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PromotionRepository extends JpaRepository<PromotionEntity, Long> {

    List<PromotionEntity> findAllByOrderByCreatedAtDesc();

    boolean existsByName(String name);

    @Query("SELECT p FROM PromotionEntity p WHERE " +
            "(:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
            "(:scope IS NULL OR p.scope = :scope) AND " +
            "(:applicability IS NULL OR p.applicability = :applicability) AND " +
            "(:status IS NULL OR p.status = :status) " +
            "ORDER BY p.createdAt DESC")
    List<PromotionEntity> findByFilters(@Param("name") String name,
                                        @Param("scope") PromotionScope scope,
                                        @Param("applicability") PromotionApplicability applicability,
                                        @Param("status") Status status);

    @Query("SELECT p FROM PromotionEntity p " +
            "WHERE p.scope = 'SYSTEM_WIDE' " +
            "OR (p.scope = 'BRANCH_SPECIFIC' AND EXISTS " +
            "(SELECT 1 FROM p.branches b WHERE b.id = :branchId)) " +
            "ORDER BY p.createdAt DESC")
    List<PromotionEntity> findByBranchId(@Param("branchId") Long branchId);

    @Query("SELECT p FROM PromotionEntity p " +
            "WHERE (:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
            "(:scope IS NULL OR p.scope = :scope) AND " +
            "(:applicability IS NULL OR p.applicability = :applicability) AND " +
            "(:status IS NULL OR p.status = :status) AND " +
            "(p.scope = 'SYSTEM_WIDE' OR (p.scope = 'BRANCH_SPECIFIC' AND EXISTS " +
            "(SELECT 1 FROM p.branches b WHERE b.id = :branchId))) " +
            "ORDER BY p.createdAt DESC")
    List<PromotionEntity> findByFiltersAndBranchId(@Param("name") String name,
                                                   @Param("scope") PromotionScope scope,
                                                   @Param("applicability") PromotionApplicability applicability,
                                                   @Param("status") Status status,
                                                   @Param("branchId") Long branchId);

    List<PromotionEntity> findByStatus(Status status);


    @Query("SELECT p FROM PromotionEntity p WHERE p.scope = 'SYSTEM_WIDE' AND p.status = 'ACTIVE' AND p.startDate <= :now AND p.endDate > :now AND (p.usageLimit IS NULL OR p.usedCount < p.usageLimit)")
    List<PromotionEntity> findSystemWideActivePromotions(@Param("now") LocalDateTime now);

    @Query("SELECT p FROM PromotionEntity p JOIN p.branches b WHERE b.id = :branchId AND p.scope = 'BRANCH_SPECIFIC' AND p.status = 'ACTIVE' AND p.startDate <= :now AND p.endDate > :now AND (p.usageLimit IS NULL OR p.usedCount < p.usageLimit)")
    List<PromotionEntity> findBranchSpecificActivePromotions(@Param("branchId") Long branchId,
                                                             @Param("now") LocalDateTime now);

    @Query("SELECT p FROM PromotionEntity p LEFT JOIN p.branches b WHERE (p.scope = 'SYSTEM_WIDE' OR (p.scope = 'BRANCH_SPECIFIC' AND b.id = :branchId)) AND p.status = 'ACTIVE' AND p.startDate <= :now AND p.endDate > :now AND (p.usageLimit IS NULL OR p.usedCount < p.usageLimit) AND (p.applicability = :applicability OR p.applicability = 'BOTH')")
    List<PromotionEntity> findActivePromotionsByBranchAndApplicability(@Param("branchId") Long branchId,
                                                                       @Param("applicability") PromotionApplicability applicability,
                                                                       @Param("now") LocalDateTime now);

    List<PromotionEntity> findByStatusAndEndDateAfterOrderByEndDateAsc(Status status, LocalDateTime now);


}