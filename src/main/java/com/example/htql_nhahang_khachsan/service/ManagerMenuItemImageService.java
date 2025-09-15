package com.example.htql_nhahang_khachsan.service;

import com.example.htql_nhahang_khachsan.entity.MenuItemEntity;
import com.example.htql_nhahang_khachsan.entity.MenuItemImageEntity;
import com.example.htql_nhahang_khachsan.repository.MenuItemImageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class ManagerMenuItemImageService {

    private final MenuItemImageRepository menuItemImageRepository;
    private final FileService fileService;

    public ManagerMenuItemImageService(MenuItemImageRepository menuItemImageRepository, FileService fileService) {
        this.menuItemImageRepository = menuItemImageRepository;
        this.fileService = fileService;
    }

    public List<MenuItemImageEntity> saveMenuItemImages(MenuItemEntity menuItem, List<MultipartFile> imageFiles) {
        if (imageFiles == null || imageFiles.isEmpty()) {
            return new ArrayList<>();
        }

        // Save files to disk
        List<String> imagePaths = fileService.saveImages(imageFiles);

        // Create menu item image entities
        List<MenuItemImageEntity> menuItemImages = new ArrayList<>();
        for (int i = 0; i < imagePaths.size(); i++) {
            String imagePath = imagePaths.get(i);

            MenuItemImageEntity menuItemImage = MenuItemImageEntity.builder()
                    .menuItem(menuItem)
                    .imageUrl(imagePath)
                    .imageTitle(extractFilename(imagePath))
                    .isPrimary(i == 0) // First image is primary
                    .build();

            menuItemImages.add(menuItemImage);
        }

        return menuItemImageRepository.saveAll(menuItemImages);
    }

    public List<MenuItemImageEntity> updateMenuItemImages(MenuItemEntity menuItem,
                                                          List<MultipartFile> newImageFiles,
                                                          List<Long> deleteImageIds) {
        // Delete specified images
        if (deleteImageIds != null && !deleteImageIds.isEmpty()) {
            List<MenuItemImageEntity> imagesToDelete = menuItemImageRepository.findByIdInAndMenuItem(deleteImageIds, menuItem);
            for (MenuItemImageEntity image : imagesToDelete) {
                // Delete physical file
                fileService.deleteImage(image.getImageUrl());
                // Delete database record
                menuItemImageRepository.delete(image);
            }
        }

        // Add new images
        List<MenuItemImageEntity> newImages = new ArrayList<>();
        if (newImageFiles != null && !newImageFiles.isEmpty()) {
            // Save new files
            List<String> imagePaths = fileService.saveImages(newImageFiles);

            // Create new menu item image entities
            for (String imagePath : imagePaths) {
                MenuItemImageEntity menuItemImage = MenuItemImageEntity.builder()
                        .menuItem(menuItem)
                        .imageUrl(imagePath)
                        .imageTitle(extractFilename(imagePath))
                        .isPrimary(false) // New images are not primary by default
                        .build();

                newImages.add(menuItemImage);
            }

            newImages = menuItemImageRepository.saveAll(newImages);
        }

        // If no primary image exists, set first image as primary
        ensurePrimaryImage(menuItem);

        return newImages;
    }

    public List<MenuItemImageEntity> getMenuItemImages(MenuItemEntity menuItem) {
        return menuItemImageRepository.findByMenuItemOrderByUploadedAt(menuItem);
    }

    public void deleteAllMenuItemImages(MenuItemEntity menuItem) {
        List<MenuItemImageEntity> images = menuItemImageRepository.findByMenuItem(menuItem);
        for (MenuItemImageEntity image : images) {
            // Delete physical file
            fileService.deleteImage(image.getImageUrl());
        }
        // Delete all database records
        menuItemImageRepository.deleteByMenuItem(menuItem);
    }

    public void setPrimaryImage(Long imageId, MenuItemEntity menuItem) {
        // Reset all images to non-primary
        menuItemImageRepository.updatePrimaryStatusByMenuItem(menuItem, false);

        // Set selected image as primary
        MenuItemImageEntity image = menuItemImageRepository.findByIdAndMenuItem(imageId, menuItem)
                .orElseThrow(() -> new RuntimeException("Image not found"));
        image.setIsPrimary(true);
        menuItemImageRepository.save(image);
    }

    private void ensurePrimaryImage(MenuItemEntity menuItem) {
        // Check if any primary image exists
        boolean hasPrimary = menuItemImageRepository.existsByMenuItemAndIsPrimary(menuItem, true);

        if (!hasPrimary) {
            // Set first image as primary
            List<MenuItemImageEntity> images = menuItemImageRepository.findByMenuItemOrderByUploadedAt(menuItem);
            if (!images.isEmpty()) {
                MenuItemImageEntity firstImage = images.get(0);
                firstImage.setIsPrimary(true);
                menuItemImageRepository.save(firstImage);
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
