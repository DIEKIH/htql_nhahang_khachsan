package com.example.htql_nhahang_khachsan.service;




import com.example.htql_nhahang_khachsan.dto.LoginRequest;
import com.example.htql_nhahang_khachsan.dto.RegisterRequest;
import com.example.htql_nhahang_khachsan.dto.UserResponse;
import com.example.htql_nhahang_khachsan.entity.UserEntity;
import com.example.htql_nhahang_khachsan.enums.UserRole;
import com.example.htql_nhahang_khachsan.enums.UserStatus;
import com.example.htql_nhahang_khachsan.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserResponse login(LoginRequest request, HttpSession session) {
        Optional<UserEntity> userOpt = userRepository.findByUsername(request.getUsername());

        if (userOpt.isEmpty()) {
            throw new RuntimeException("Tài khoản không tồn tại");
        }

        UserEntity user = userOpt.get();

        if (user.getStatus() == UserStatus.LOCKED) {
            throw new RuntimeException("Tài khoản đã bị khóa");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Mật khẩu không chính xác");
        }

        // Lưu thông tin user vào session
        session.setAttribute("user", user);
        session.setAttribute("userId", user.getId());
        session.setAttribute("userRole", user.getRole());

        return convertToResponse(user);
    }

    public UserResponse register(RegisterRequest request, UserRole role) {
        // Kiểm tra username đã tồn tại
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username đã tồn tại");
        }

        // Kiểm tra email đã tồn tại
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email đã tồn tại");
        }

        // Kiểm tra password khớp
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Password xác nhận không khớp");
        }

        // Tạo user mới
        UserEntity user = UserEntity.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .address(request.getAddress())
                .role(role)
                .status(UserStatus.ACTIVE)
                .build();

        UserEntity savedUser = userRepository.save(user);
        return convertToResponse(savedUser);
    }

    public void logout(HttpSession session) {
        session.invalidate();
    }

    public UserEntity getCurrentUser(HttpSession session) {
        return (UserEntity) session.getAttribute("user");
    }

    public boolean isLoggedIn(HttpSession session) {
        return session.getAttribute("user") != null;
    }

    public boolean isAdmin(HttpSession session) {
        UserRole role = (UserRole) session.getAttribute("userRole");
        return role == UserRole.ADMIN;
    }

    public boolean isManager(HttpSession session) {
        UserRole role = (UserRole) session.getAttribute("userRole");
        return role == UserRole.MANAGER;
    }

    public boolean isStaff(HttpSession session) {
        UserRole role = (UserRole) session.getAttribute("userRole");
        return role == UserRole.STAFF;
    }

    public boolean isCashierRestaurant(HttpSession session) {
        UserRole role = (UserRole) session.getAttribute("userRole");
        return role == UserRole.CASHIER_RESTAURANT;
    }

    public boolean isCashierHotel(HttpSession session) {
        UserRole role = (UserRole) session.getAttribute("userRole");
        return role == UserRole.CASHIER_HOTEL;
    }

    public boolean isCustomer(HttpSession session) {
        UserRole role = (UserRole) session.getAttribute("userRole");
        return role == UserRole.CUSTOMER;
    }

    // Kiểm tra có phải là staff hoặc cashier không
    public boolean isStaffOrCashier(HttpSession session) {
        return isStaff(session) || isCashierRestaurant(session) || isCashierHotel(session);
    }

    // Kiểm tra có phải là management level không (Admin hoặc Manager)
    public boolean isManagementLevel(HttpSession session) {
        return isAdmin(session) || isManager(session);
    }

    private UserResponse convertToResponse(UserEntity user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setPhoneNumber(user.getPhoneNumber());
        response.setAddress(user.getAddress());
        response.setAvatarUrl(user.getAvatarUrl());
        response.setRole(user.getRole());
        response.setStatus(user.getStatus());
        response.setCreatedAt(user.getCreatedAt());
        if (user.getBranch() != null) {
            response.setBranchName(user.getBranch().getName());
        }
        return response;
    }


    //lấy chi nhánh của manager


    /**
     * Lấy ID chi nhánh của user hiện tại
     * Chỉ áp dụng cho MANAGER và STAFF
     */
    public Long getCurrentUserBranchId(HttpSession session) {
        if (!isLoggedIn(session)) {
            throw new RuntimeException("User chưa đăng nhập");
        }

        // Lấy branchId từ session trước (nếu đã lưu)
        Long branchId = (Long) session.getAttribute("branchId");
        if (branchId != null) {
            return branchId;
        }

        // Nếu chưa có trong session, query từ database
        Long userId = getCurrentUserId(session);
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        if (user.getBranch() == null) {
            throw new RuntimeException("User không thuộc chi nhánh nào");
        }

        branchId = user.getBranch().getId();
        // Lưu vào session cho lần sau
        session.setAttribute("branchId", branchId);

        return branchId;
    }

    public Long getCurrentUserId(HttpSession session) {
        if (!isLoggedIn(session)) {
            throw new RuntimeException("User chưa đăng nhập");
        }

        return (Long) session.getAttribute("userId");
    }

    /**
     * Lấy role của user hiện tại
     */
    public UserRole getCurrentUserRole(HttpSession session) {
        if (!isLoggedIn(session)) {
            throw new RuntimeException("User chưa đăng nhập");
        }

        return (UserRole) session.getAttribute("userRole");
    }

    /**
     * Kiểm tra user có quyền truy cập chi nhánh không
     */
    public boolean hasAccessToBranch(HttpSession session, Long branchId) {
        if (!isLoggedIn(session)) {
            return false;
        }

        UserRole role = getCurrentUserRole(session);

        // ADMIN có thể truy cập tất cả chi nhánh
        if (role == UserRole.ADMIN) {
            return true;
        }

        // MANAGER và STAFF chỉ có thể truy cập chi nhánh của mình
        if (role == UserRole.MANAGER || role == UserRole.STAFF) {
            try {
                Long userBranchId = getCurrentUserBranchId(session);
                return userBranchId.equals(branchId);
            } catch (Exception e) {
                return false;
            }
        }

        // CUSTOMER không có quyền truy cập quản lý chi nhánh
        return false;
    }

    /**
     * Kiểm tra user có quyền với role cụ thể không
     */
    public boolean hasRole(HttpSession session, UserRole requiredRole) {
        if (!isLoggedIn(session)) {
            return false;
        }

        UserRole currentRole = getCurrentUserRole(session);
        return currentRole == requiredRole;
    }

    /**
     * Kiểm tra user có một trong các role không
     */
    public boolean hasAnyRole(HttpSession session, UserRole... roles) {
        if (!isLoggedIn(session)) {
            return false;
        }

        UserRole currentRole = getCurrentUserRole(session);
        return Arrays.asList(roles).contains(currentRole);
    }


}
