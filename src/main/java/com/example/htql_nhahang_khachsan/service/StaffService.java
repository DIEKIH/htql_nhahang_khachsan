package com.example.htql_nhahang_khachsan.service;


import com.example.htql_nhahang_khachsan.dto.UserRequest;
import com.example.htql_nhahang_khachsan.dto.UserResponse;
import com.example.htql_nhahang_khachsan.entity.BranchEntity;
import com.example.htql_nhahang_khachsan.entity.UserEntity;
import com.example.htql_nhahang_khachsan.enums.UserRole;
import com.example.htql_nhahang_khachsan.enums.UserStatus;
import com.example.htql_nhahang_khachsan.repository.BranchRepository;
import com.example.htql_nhahang_khachsan.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StaffService {

    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final PasswordEncoder passwordEncoder;

    public List<UserResponse> getAllStaff() {
        List<UserRole> staffRoles = Arrays.asList(UserRole.MANAGER, UserRole.STAFF);
        return userRepository.findByRoleInOrderByCreatedAtDesc(staffRoles).stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());
    }

    public List<UserResponse> getManagersOnly() {
        return userRepository.findByRole(UserRole.MANAGER).stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());
    }

    public List<UserResponse> getStaffOnly() {
        return userRepository.findByRole(UserRole.STAFF).stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());
    }

    public UserResponse getStaffById(Long id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên"));
        if (user.getRole() != UserRole.MANAGER && user.getRole() != UserRole.STAFF) {
            throw new RuntimeException("Người dùng không phải là nhân viên");
        }
        return UserResponse.from(user);
    }

    public UserResponse createStaff(UserRequest request) {
        // Validate role
        if (!(request.getRole() == UserRole.MANAGER ||
                request.getRole() == UserRole.STAFF ||
                request.getRole() == UserRole.CASHIER_RESTAURANT ||
                request.getRole() == UserRole.CASHIER_HOTEL)) {
            throw new RuntimeException("Role không hợp lệ");
        }


        // Kiểm tra username và email đã tồn tại
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Tên đăng nhập đã tồn tại");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email đã tồn tại");
        }

        // Lấy thông tin branch nếu có
        BranchEntity branch = null;
        if (request.getBranchId() != null) {
            branch = branchRepository.findById(request.getBranchId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy chi nhánh"));
        }

        UserEntity user = UserEntity.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .address(request.getAddress())
                .avatarUrl(request.getAvatarUrl())
                .role(request.getRole())
                .status(UserStatus.ACTIVE)
                .branch(branch)
                .build();

        UserEntity savedUser = userRepository.save(user);
        return UserResponse.from(savedUser);
    }

    public UserResponse updateStaff(Long id, UserRequest request) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên"));

//        if (user.getRole() != UserRole.MANAGER && user.getRole() != UserRole.STAFF) {
//            throw new RuntimeException("Người dùng không phải là nhân viên");
//        }

        if (!(request.getRole() == UserRole.MANAGER ||
                request.getRole() == UserRole.STAFF ||
                request.getRole() == UserRole.CASHIER_RESTAURANT ||
                request.getRole() == UserRole.CASHIER_HOTEL)) {
            throw new RuntimeException("Role không hợp lệ");
        }


        // Kiểm tra username và email đã tồn tại (trừ chính nó)
        if (userRepository.findByUsernameAndIdNot(request.getUsername(), id).isPresent()) {
            throw new RuntimeException("Tên đăng nhập đã tồn tại");
        }

        if (userRepository.findByEmailAndIdNot(request.getEmail(), id).isPresent()) {
            throw new RuntimeException("Email đã tồn tại");
        }

        // Lấy thông tin branch nếu có
        BranchEntity branch = null;
        if (request.getBranchId() != null) {
            branch = branchRepository.findById(request.getBranchId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy chi nhánh"));
        }

        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setAddress(request.getAddress());
        user.setAvatarUrl(request.getAvatarUrl());
        user.setRole(request.getRole());
        user.setBranch(branch);

        // Cập nhật mật khẩu nếu có
        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        UserEntity updatedUser = userRepository.save(user);
        return UserResponse.from(updatedUser);
    }

    public void lockStaff(Long id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhân viên"));

        if (user.getRole() != UserRole.MANAGER && user.getRole() != UserRole.STAFF) {
            throw new RuntimeException("Người dùng không phải là nhân viên");
        }

        user.setStatus(user.getStatus() == UserStatus.ACTIVE ? UserStatus.LOCKED : UserStatus.ACTIVE);
        userRepository.save(user);
    }

    public List<UserResponse> searchStaff(List<UserRole> roles, Long branchId, UserStatus status, String search) {
        if (roles == null || roles.isEmpty()) {
            roles = Arrays.asList(
                    UserRole.MANAGER,
                    UserRole.STAFF,
                    UserRole.CASHIER_RESTAURANT,
                    UserRole.CASHIER_HOTEL
            );
        }


        return userRepository.findStaffByFilters(roles, branchId, status, search).stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());
    }
}
