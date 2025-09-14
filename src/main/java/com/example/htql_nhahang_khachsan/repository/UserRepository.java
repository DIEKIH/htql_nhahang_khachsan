package com.example.htql_nhahang_khachsan.repository;



import com.example.htql_nhahang_khachsan.entity.BranchEntity;
import com.example.htql_nhahang_khachsan.entity.UserEntity;
import com.example.htql_nhahang_khachsan.enums.UserRole;
import com.example.htql_nhahang_khachsan.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByUsername(String username);
    Optional<UserEntity> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    List<UserEntity> findByRole(UserRole role);


    // Existing methods...
    Optional<UserEntity> findByUsernameAndIdNot(String username, Long id);
    Optional<UserEntity> findByEmailAndIdNot(String email, Long id);


    List<UserEntity> findByStatus(UserStatus status);
    List<UserEntity> findByBranch(BranchEntity branch);
    List<UserEntity> findByRoleInOrderByCreatedAtDesc(List<UserRole> roles);

    @Query("SELECT u FROM UserEntity u WHERE u.role IN :roles AND " +
            "(:branchId IS NULL OR u.branch.id = :branchId) AND " +
            "(:status IS NULL OR u.status = :status) AND " +
            "(:search IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<UserEntity> findStaffByFilters(@Param("roles") List<UserRole> roles,
                                        @Param("branchId") Long branchId,
                                        @Param("status") UserStatus status,
                                        @Param("search") String search);
}