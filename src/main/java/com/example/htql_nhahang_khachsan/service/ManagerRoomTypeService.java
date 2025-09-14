package com.example.htql_nhahang_khachsan.service;

import com.example.htql_nhahang_khachsan.dto.RoomTypeRequest;
import com.example.htql_nhahang_khachsan.dto.RoomTypeResponse;
import com.example.htql_nhahang_khachsan.entity.BranchEntity;
import com.example.htql_nhahang_khachsan.entity.RoomImageEntity;
import com.example.htql_nhahang_khachsan.entity.RoomTypeEntity;
import com.example.htql_nhahang_khachsan.enums.Status;
import com.example.htql_nhahang_khachsan.repository.BranchRepository;
import com.example.htql_nhahang_khachsan.repository.RoomTypeRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ManagerRoomTypeService {
    private final RoomTypeRepository roomTypeRepository;
    private final BranchRepository branchRepository;
    private final ManagerRoomImageService roomImageService;
    private final ObjectMapper objectMapper;

    public ManagerRoomTypeService(RoomTypeRepository roomTypeRepository,
                                  BranchRepository branchRepository,
                                  ManagerRoomImageService roomImageService,
                                  ObjectMapper objectMapper) {
        this.roomTypeRepository = roomTypeRepository;
        this.branchRepository = branchRepository;
        this.roomImageService = roomImageService;
        this.objectMapper = objectMapper;
    }

    public List<RoomTypeResponse> getAllRoomTypesByBranch(Long branchId) {
        List<RoomTypeEntity> roomTypes = roomTypeRepository.findByBranchIdOrderByCreatedAtDesc(branchId);
        return roomTypes.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    private RoomTypeResponse convertToResponse(RoomTypeEntity entity) {
        RoomTypeResponse response = RoomTypeResponse.builder()
                .id(entity.getId())
                .branchId(entity.getBranch().getId())
                .branchName(entity.getBranch().getName())
                .name(entity.getName())
                .description(entity.getDescription())
                .price(entity.getPrice())
                .maxOccupancy(entity.getMaxOccupancy())
                .bedType(entity.getBedType())
                .roomSize(entity.getRoomSize())
                .amenities(entity.getAmenities())
                .imageUrls(entity.getImageUrls())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();

        // Format price
        if (entity.getPrice() != null) {
            DecimalFormat df = new DecimalFormat("#,###");
            response.setFormattedPrice(df.format(entity.getPrice()) + " VNĐ");
        }

        // Parse amenities JSON to list
        if (entity.getAmenities() != null) {
            try {
                response.setAmenitiesList(objectMapper.readValue(
                        entity.getAmenities(), new TypeReference<List<String>>() {}));
            } catch (Exception e) {
                response.setAmenitiesList(new ArrayList<>());
            }
        }

        // Get room images from RoomImageEntity instead of JSON
        List<RoomImageEntity> roomImages = roomImageService.getRoomImages(entity);
        List<String> imageUrlsList = roomImages.stream()
                .map(RoomImageEntity::getImageUrl)
                .collect(Collectors.toList());
        response.setImageUrlsList(imageUrlsList);
        response.setRoomImages(roomImages); // Add room images with metadata

        return response;
    }

    public List<RoomTypeResponse> searchRoomTypes(Long branchId, String name, Status status) {
        List<RoomTypeEntity> roomTypes;

        if (name != null && status != null) {
            roomTypes = roomTypeRepository.findByBranchIdAndNameContainingIgnoreCaseAndStatusOrderByCreatedAtDesc(
                    branchId, name, status);
        } else if (name != null) {
            roomTypes = roomTypeRepository.findByBranchIdAndNameContainingIgnoreCaseOrderByCreatedAtDesc(
                    branchId, name);
        } else if (status != null) {
            roomTypes = roomTypeRepository.findByBranchIdAndStatusOrderByCreatedAtDesc(
                    branchId, status);
        } else {
            roomTypes = roomTypeRepository.findByBranchIdOrderByCreatedAtDesc(branchId);
        }

        return roomTypes.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public RoomTypeResponse getRoomTypeById(Long id, Long branchId) {
        RoomTypeEntity roomType = roomTypeRepository.findByIdAndBranchId(id, branchId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy loại phòng"));
        return convertToResponse(roomType);
    }

    public RoomTypeResponse createRoomType(Long branchId,
                                           RoomTypeRequest request,
                                           List<MultipartFile> imageFiles) {
        BranchEntity branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chi nhánh"));

        // Kiểm tra trùng tên trong cùng chi nhánh
        if (roomTypeRepository.existsByBranchIdAndNameIgnoreCase(branchId, request.getName())) {
            throw new RuntimeException("Tên loại phòng đã tồn tại trong chi nhánh này");
        }

        RoomTypeEntity roomType = RoomTypeEntity.builder()
                .branch(branch)
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .maxOccupancy(request.getMaxOccupancy())
                .bedType(request.getBedType())
                .roomSize(request.getRoomSize())
                .amenities(request.getAmenities())
                .imageUrls("[]") // Keep empty JSON for backward compatibility
                .status(request.getStatus())
                .build();

        roomType = roomTypeRepository.save(roomType);

        // Save room images
        if (imageFiles != null && !imageFiles.isEmpty()) {
            roomImageService.saveRoomImages(roomType, imageFiles);
        }

        return convertToResponse(roomType);
    }

    public RoomTypeResponse updateRoomType(Long id,
                                           Long branchId,
                                           RoomTypeRequest request,
                                           List<MultipartFile> imageFiles,
                                           List<Long> deleteImageIds) {
        RoomTypeEntity roomType = roomTypeRepository.findByIdAndBranchId(id, branchId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy loại phòng"));

        // Kiểm tra trùng tên (trừ chính nó)
        if (roomTypeRepository.existsByBranchIdAndNameIgnoreCaseAndIdNot(
                branchId, request.getName(), id)) {
            throw new RuntimeException("Tên loại phòng đã tồn tại trong chi nhánh này");
        }

        roomType.setName(request.getName());
        roomType.setDescription(request.getDescription());
        roomType.setPrice(request.getPrice());
        roomType.setMaxOccupancy(request.getMaxOccupancy());
        roomType.setBedType(request.getBedType());
        roomType.setRoomSize(request.getRoomSize());
        roomType.setAmenities(request.getAmenities());
        roomType.setStatus(request.getStatus());

        roomType = roomTypeRepository.save(roomType);

        // Update room images
        roomImageService.updateRoomImages(roomType, imageFiles, deleteImageIds);

        return convertToResponse(roomType);
    }

    public void deleteRoomType(Long id, Long branchId) {
        RoomTypeEntity roomType = roomTypeRepository.findByIdAndBranchId(id, branchId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy loại phòng"));

        // Kiểm tra xem có phòng nào đang sử dụng loại phòng này không
        // TODO: Implement check for existing rooms

        // Delete all images
        roomImageService.deleteAllRoomImages(roomType);

        roomType.setStatus(Status.INACTIVE);
        roomTypeRepository.save(roomType);
    }

    public List<RoomTypeResponse> getAllActiveRoomTypesByBranch(Long branchId) {
        List<RoomTypeEntity> roomTypes = roomTypeRepository.findByBranchIdAndStatusOrderByCreatedAtDesc(
                branchId, Status.ACTIVE);
        return roomTypes.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

}
