package com.example.htql_nhahang_khachsan.service;

import com.example.htql_nhahang_khachsan.dto.BranchRequest;
import com.example.htql_nhahang_khachsan.dto.BranchResponse;
import com.example.htql_nhahang_khachsan.entity.BranchEntity;
import com.example.htql_nhahang_khachsan.enums.BranchStatus;
import com.example.htql_nhahang_khachsan.enums.BranchType;
import com.example.htql_nhahang_khachsan.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BranchService {

    private final BranchRepository branchRepository;

    public List<BranchResponse> getAllBranches() {
        return branchRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(BranchResponse::from)
                .collect(Collectors.toList());
    }

    public BranchResponse getBranchById(Long id) {
        BranchEntity branch = branchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chi nhánh"));
        return BranchResponse.from(branch);
    }

    public BranchResponse createBranch(BranchRequest request) {
        // Kiểm tra tên chi nhánh đã tồn tại
        if (branchRepository.existsByName(request.getName())) {
            throw new RuntimeException("Tên chi nhánh đã tồn tại");
        }

        BranchEntity branch = BranchEntity.builder()
                .name(request.getName())
                .description(request.getDescription())
                .address(request.getAddress())
                .phoneNumber(request.getPhoneNumber())
                .email(request.getEmail())
                .type(request.getType())
                .status(request.getStatus())
                .imageUrl(request.getImageUrl())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .build();

        BranchEntity savedBranch = branchRepository.save(branch);
        return BranchResponse.from(savedBranch);
    }

    public BranchResponse updateBranch(Long id, BranchRequest request) {
        BranchEntity branch = branchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chi nhánh"));

        // Kiểm tra tên chi nhánh đã tồn tại (trừ chính nó)
        if (branchRepository.findByNameAndIdNot(request.getName(), id).isPresent()) {
            throw new RuntimeException("Tên chi nhánh đã tồn tại");
        }

        branch.setName(request.getName());
        branch.setDescription(request.getDescription());
        branch.setAddress(request.getAddress());
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
}