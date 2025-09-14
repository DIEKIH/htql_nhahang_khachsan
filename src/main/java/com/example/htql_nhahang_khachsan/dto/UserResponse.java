package com.example.htql_nhahang_khachsan.dto;

import com.example.htql_nhahang_khachsan.entity.UserEntity;
import com.example.htql_nhahang_khachsan.enums.UserRole;
import com.example.htql_nhahang_khachsan.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String phoneNumber;
    private String address;
    private String avatarUrl;
    private UserRole role;
    private UserStatus status;
    private Long branchId;
    private String branchName;
    private BranchResponse branch;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static UserResponse from(UserEntity entity) {
        return UserResponse.builder()
                .id(entity.getId())
                .username(entity.getUsername())
                .email(entity.getEmail())
                .fullName(entity.getFullName())
                .phoneNumber(entity.getPhoneNumber())
                .address(entity.getAddress())
                .avatarUrl(entity.getAvatarUrl())
                .role(entity.getRole())
                .status(entity.getStatus())
                .branchId(entity.getBranch() != null ? entity.getBranch().getId() : null)   // 👈 map thêm
                .branchName(entity.getBranch() != null ? entity.getBranch().getName() : null)
                .branch(entity.getBranch() != null ? BranchResponse.from(entity.getBranch()) : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

}