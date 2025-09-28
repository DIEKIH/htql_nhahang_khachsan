package com.example.htql_nhahang_khachsan.service;

import com.example.htql_nhahang_khachsan.dto.BranchRequest;
import com.example.htql_nhahang_khachsan.dto.BranchResponse;
import com.example.htql_nhahang_khachsan.entity.BranchEntity;
import com.example.htql_nhahang_khachsan.entity.RoomEntity;
import com.example.htql_nhahang_khachsan.entity.RoomTypeEntity;
import com.example.htql_nhahang_khachsan.enums.BranchStatus;
import com.example.htql_nhahang_khachsan.enums.BranchType;
import com.example.htql_nhahang_khachsan.repository.BranchRepository;
import com.example.htql_nhahang_khachsan.repository.RoomRepository;
import com.example.htql_nhahang_khachsan.repository.RoomTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BranchService {

    private final BranchRepository branchRepository;

    private final FileService fileService;

    public List<BranchResponse> getAllBranches() {
        return branchRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(BranchResponse::from)
                .collect(Collectors.toList());
    }




    public String getBranchNameById(Long id) {
        return branchRepository.findById(id)
                .map(BranchEntity::getName)
                .orElse("Không tìm thấy chi nhánh");
    }
    public BranchResponse getBranchById(Long id) {
        BranchEntity branch = branchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chi nhánh"));
        return BranchResponse.from(branch);
    }

    public BranchResponse createBranch(BranchRequest request, MultipartFile imageFile) {
        // Kiểm tra tên chi nhánh đã tồn tại
        if (branchRepository.existsByName(request.getName())) {
            throw new RuntimeException("Tên chi nhánh đã tồn tại");
        }

        // Lưu ảnh (nếu có upload)
        String imageUrl = null;
        if (imageFile != null && !imageFile.isEmpty()) {
            imageUrl = fileService.saveImage(imageFile);
        } else {
            imageUrl = request.getImageUrl(); // fallback nếu muốn cho nhập URL trực tiếp
        }

        BranchEntity branch = BranchEntity.builder()
                .name(request.getName())
                .description(request.getDescription())
//                .address(request.getAddress())
                .address(
                        request.getStreetAddress() + ", " +
                        request.getDistrict() + ", " +
                        request.getProvince()
                )
                .streetAddress(request.getStreetAddress())
                .district(request.getDistrict())
                .province(request.getProvince())
                .phoneNumber(request.getPhoneNumber())
                .email(request.getEmail())
                .type(request.getType())
                .status(request.getStatus())
//                .imageUrl(request.getImageUrl())
                .imageUrl(imageUrl)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .build();

        BranchEntity savedBranch = branchRepository.save(branch);
        return BranchResponse.from(savedBranch);
    }

    public BranchResponse updateBranch(Long id, BranchRequest request,MultipartFile imageFile) {
        BranchEntity branch = branchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chi nhánh"));

        // Kiểm tra tên chi nhánh đã tồn tại (trừ chính nó)
        if (branchRepository.findByNameAndIdNot(request.getName(), id).isPresent()) {
            throw new RuntimeException("Tên chi nhánh đã tồn tại");
        }

        // Nếu có upload file mới thì thay, nếu không thì giữ nguyên
        if (imageFile != null && !imageFile.isEmpty()) {
            // Xóa ảnh cũ (nếu có)
            fileService.deleteImage(branch.getImageUrl());
            String newImageUrl = fileService.saveImage(imageFile);
            branch.setImageUrl(newImageUrl);
        } else if (request.getImageUrl() != null) {
            branch.setImageUrl(request.getImageUrl());
        }

        branch.setName(request.getName());
        branch.setDescription(request.getDescription());
//        branch.setAddress(request.getAddress());
        branch.setAddress(
                request.getStreetAddress() + ", " +
                        request.getDistrict() + ", " +
                        request.getProvince()
        );
        branch.setStreetAddress(request.getStreetAddress());
        branch.setDistrict(request.getDistrict());
        branch.setProvince(request.getProvince());
        branch.setPhoneNumber(request.getPhoneNumber());
        branch.setEmail(request.getEmail());
        branch.setType(request.getType());
        branch.setStatus(request.getStatus());
        branch.setImageUrl(request.getImageUrl());
        branch.setLatitude(request.getLatitude());
        branch.setLongitude(request.getLongitude());

        BranchEntity updatedBranch = branchRepository.save(branch);
        return BranchResponse.from(updatedBranch);
    }

    public void deleteBranch(Long id) {
        BranchEntity branch = branchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chi nhánh"));

        // Thay vì xóa, đổi status thành INACTIVE
        branch.setStatus(BranchStatus.INACTIVE);
        branchRepository.save(branch);
    }

    public List<BranchResponse> searchBranches(String name, BranchType type, BranchStatus status) {
        return branchRepository.findByFilters(name, type, status).stream()
                .map(BranchResponse::from)
                .collect(Collectors.toList());
    }

    public List<BranchResponse> getActiveBranches() {
        return branchRepository.findByStatus(BranchStatus.ACTIVE).stream()
                .map(BranchResponse::from)
                .collect(Collectors.toList());
    }

    // Thêm các method này vào BranchService.java hiện tại

    public List<String> getAllProvinces() {
        return branchRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(BranchEntity::getProvince)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    public List<BranchResponse> getBranchesByProvince(String province) {
        return branchRepository.findByStatus(BranchStatus.ACTIVE).stream()
                .filter(branch -> branch.getProvince().toLowerCase().contains(province.toLowerCase()))
                .map(BranchResponse::from)
                .collect(Collectors.toList());
    }

    public List<BranchResponse> getBranchesByType(BranchType type) {
        return branchRepository.findByTypeAndStatus(type, BranchStatus.ACTIVE).stream()
                .map(BranchResponse::from)
                .collect(Collectors.toList());
    }

    public List<BranchResponse> searchActiveBranches(String keyword) {
        return branchRepository.findActiveByKeyword(keyword).stream()
                .map(BranchResponse::from)
                .collect(Collectors.toList());
    }

    public long getTotalActiveBranches() {
        return branchRepository.countByStatus(BranchStatus.ACTIVE);
    }

    public List<BranchResponse> getFeaturedBranches(int limit) {
        return branchRepository.findByStatus(BranchStatus.ACTIVE).stream()
                .limit(limit)
                .map(BranchResponse::from)
                .collect(Collectors.toList());
    }


    private final RoomRepository roomRepository;
    private final RoomTypeRepository roomTypeRepository;

    public List<RoomTypeEntity> getRoomTypesByBranchId(Long branchId) {
        return roomTypeRepository.findByBranchId(branchId);
    }

    public List<RoomEntity> getRoomsByBranchId(Long branchId) {
        return roomRepository.findByRoomTypeBranchId(branchId);
    }

}