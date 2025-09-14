package com.example.htql_nhahang_khachsan.repository;

import com.example.htql_nhahang_khachsan.entity.BranchEntity;
import com.example.htql_nhahang_khachsan.enums.BranchStatus;
import com.example.htql_nhahang_khachsan.enums.BranchType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BranchRepository extends JpaRepository<BranchEntity, Long> {

    List<BranchEntity> findAllByOrderByCreatedAtDesc();

    Optional<BranchEntity> findByNameAndIdNot(String name, Long id);

    boolean existsByName(String name);

    List<BranchEntity> findByStatus(BranchStatus status);

    List<BranchEntity> findByType(BranchType type);

    @Query("SELECT b FROM BranchEntity b WHERE " +
            "(:name IS NULL OR LOWER(b.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
            "(:type IS NULL OR b.type = :type) AND " +
            "(:status IS NULL OR b.status = :status)")
    List<BranchEntity> findByFilters(@Param("name") String name,
                                     @Param("type") BranchType type,
                                     @Param("status") BranchStatus status);
}
