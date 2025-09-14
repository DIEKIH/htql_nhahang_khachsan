package com.example.htql_nhahang_khachsan.service;

import com.example.htql_nhahang_khachsan.entity.RoomImageEntity;
import com.example.htql_nhahang_khachsan.entity.RoomTypeEntity;
import com.example.htql_nhahang_khachsan.repository.RoomImageRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class ManagerRoomImageService {

    private final RoomImageRepository roomImageRepository;
    private final FileService fileService;

    public ManagerRoomImageService(RoomImageRepository roomImageRepository, FileService fileService) {
        this.roomImageRepository = roomImageRepository;
        this.fileService = fileService;
    }

    public List<RoomImageEntity> saveRoomImages(RoomTypeEntity roomType, List<MultipartFile> imageFiles) {
        if (imageFiles == null || imageFiles.isEmpty()) {
            return new ArrayList<>();
        }

        // Save files to disk
        List<String> imagePaths = fileService.saveImages(imageFiles);

        // Create room image entities
        List<RoomImageEntity> roomImages = new ArrayList<>();
        for (int i = 0; i < imagePaths.size(); i++) {
            String imagePath = imagePaths.get(i);

            RoomImageEntity roomImage = RoomImageEntity.builder()
                    .roomType(roomType)
                    .imageUrl(imagePath)
                    .imageTitle(extractFilename(imagePath))
                    .isPrimary(i == 0) // First image is primary
                    .displayOrder(i + 1)
                    .build();

            roomImages.add(roomImage);
        }

        return roomImageRepository.saveAll(roomImages);
    }

    public List<RoomImageEntity> updateRoomImages(RoomTypeEntity roomType,
                                                  List<MultipartFile> newImageFiles,
                                                  List<Long> deleteImageIds) {
        // Delete specified images
        if (deleteImageIds != null && !deleteImageIds.isEmpty()) {
            List<RoomImageEntity> imagesToDelete = roomImageRepository.findByIdInAndRoomType(deleteImageIds, roomType);
            for (RoomImageEntity image : imagesToDelete) {
                // Delete physical file
                fileService.deleteImage(image.getImageUrl());
                // Delete database record
                roomImageRepository.delete(image);
            }
        }

        // Add new images
        List<RoomImageEntity> newImages = new ArrayList<>();
        if (newImageFiles != null && !newImageFiles.isEmpty()) {
            // Get current max display order
            Integer maxOrder = roomImageRepository.findMaxDisplayOrderByRoomType(roomType);
            int startOrder = (maxOrder == null ? 0 : maxOrder) + 1;

            // Save new files
            List<String> imagePaths = fileService.saveImages(newImageFiles);

            // Create new room image entities
            for (int i = 0; i < imagePaths.size(); i++) {
                String imagePath = imagePaths.get(i);

                RoomImageEntity roomImage = RoomImageEntity.builder()
                        .roomType(roomType)
                        .imageUrl(imagePath)
                        .imageTitle(extractFilename(imagePath))
                        .isPrimary(false) // New images are not primary by default
                        .displayOrder(startOrder + i)
                        .build();

                newImages.add(roomImage);
            }

            newImages = roomImageRepository.saveAll(newImages);
        }

        // If no primary image exists, set first image as primary
        ensurePrimaryImage(roomType);

        return newImages;
    }

    public List<RoomImageEntity> getRoomImages(RoomTypeEntity roomType) {
        return roomImageRepository.findByRoomTypeOrderByDisplayOrder(roomType);
    }

    public void deleteAllRoomImages(RoomTypeEntity roomType) {
        List<RoomImageEntity> images = roomImageRepository.findByRoomType(roomType);
        for (RoomImageEntity image : images) {
            // Delete physical file
            fileService.deleteImage(image.getImageUrl());
        }
        // Delete all database records
        roomImageRepository.deleteByRoomType(roomType);
    }

    public void setPrimaryImage(Long imageId, RoomTypeEntity roomType) {
        // Reset all images to non-primary
        roomImageRepository.updatePrimaryStatusByRoomType(roomType, false);

        // Set selected image as primary
        RoomImageEntity image = roomImageRepository.findByIdAndRoomType(imageId, roomType)
                .orElseThrow(() -> new RuntimeException("Image not found"));
        image.setIsPrimary(true);
        roomImageRepository.save(image);
    }

    private void ensurePrimaryImage(RoomTypeEntity roomType) {
        // Check if any primary image exists
        boolean hasPrimary = roomImageRepository.existsByRoomTypeAndIsPrimary(roomType, true);

        if (!hasPrimary) {
            // Set first image as primary
            List<RoomImageEntity> images = roomImageRepository.findByRoomTypeOrderByDisplayOrder(roomType);
            if (!images.isEmpty()) {
                RoomImageEntity firstImage = images.get(0);
                firstImage.setIsPrimary(true);
                roomImageRepository.save(firstImage);
            }
        }
    }

    private String extractFilename(String imagePath) {
        if (imagePath == null || !imagePath.contains("/")) {
            return imagePath;
        }
        return imagePath.substring(imagePath.lastIndexOf("/") + 1);
    }
}
