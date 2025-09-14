package com.example.htql_nhahang_khachsan.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class FileService {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir ;

    @Value("${file.max-size:5242880}") // 5MB
    private long maxFileSize;

    private final Set<String> allowedImageTypes  = Set.of( "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp");

    @PostConstruct
    public void init() {
        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory!", e);
        }
    }

    public List<String> saveImages(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> savedFilePaths = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;

            try {
                // Validate file
                validateImageFile(file);

                // Generate unique filename
                String originalFilename = file.getOriginalFilename();
                String extension = getFileExtension(originalFilename);
                String filename = generateUniqueFilename(extension);

                // Save file
                Path filePath = Paths.get(uploadDir, filename);
                Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

                // Return relative path for database storage
                savedFilePaths.add("/uploads/" + filename);

            } catch (IOException e) {
                throw new RuntimeException("Failed to save file: " + file.getOriginalFilename(), e);
            }
        }

        return savedFilePaths;
    }

    public String saveImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        List<String> result = saveImages(List.of(file));
        return result.isEmpty() ? null : result.get(0);
    }

    public void deleteImage(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) {
            return;
        }

        try {
            // Extract filename from path (remove /uploads/ prefix)
            String filename = imagePath.replace("/uploads/", "");
            Path filePath = Paths.get(uploadDir, filename);

            if (Files.exists(filePath)) {
                Files.delete(filePath);
            }
        } catch (IOException e) {
            // Log error but don't throw exception to avoid breaking the main operation
            e.printStackTrace();
        }
    }

    private void validateImageFile(MultipartFile file) {
        // Check file size
        if (file.getSize() > maxFileSize) {
            throw new RuntimeException("File too large: " + file.getOriginalFilename() +
                    ". Maximum size is " + (maxFileSize / 1024 / 1024) + "MB");
        }

        // Check file type
        String contentType = file.getContentType();
        if (contentType == null || !allowedImageTypes.contains(contentType.toLowerCase())) {
            throw new RuntimeException("Invalid file type: " + file.getOriginalFilename() +
                    ". Only images are allowed.");
        }

        // Check if file is actually an image
        try {
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null) {
                throw new RuntimeException("Invalid image file: " + file.getOriginalFilename());
            }
        } catch (IOException e) {
            throw new RuntimeException("Error reading image file: " + file.getOriginalFilename(), e);
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".jpg"; // default extension
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    private String generateUniqueFilename(String extension) {
        return "room_" + System.currentTimeMillis() + "_" +
                UUID.randomUUID().toString().substring(0, 8) + extension;
    }

}
