package com.example.htql_nhahang_khachsan.service;

import com.example.htql_nhahang_khachsan.dto.*;
import com.example.htql_nhahang_khachsan.enums.RoomStatus;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.example.htql_nhahang_khachsan.entity.RoomEntity;
import com.example.htql_nhahang_khachsan.entity.RoomImageEntity;
import com.example.htql_nhahang_khachsan.entity.RoomTypeEntity;
import com.example.htql_nhahang_khachsan.enums.PromotionApplicability;
import com.example.htql_nhahang_khachsan.enums.Status;
import com.example.htql_nhahang_khachsan.repository.RoomImageRepository;
import com.example.htql_nhahang_khachsan.repository.RoomRepository;
import com.example.htql_nhahang_khachsan.repository.RoomTypeRepository;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomImageRepository roomImageRepository;
    private final PromotionService promotionService;

    public RoomDetailResponse getRoomDetailById(Long roomId) {
        RoomEntity room = roomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("Phòng không tồn tại"));

        RoomTypeEntity type = room.getRoomType();

        return RoomDetailResponse.builder()
                .id(room.getId())
                .roomNumber(room.getRoomNumber())
                .floor(room.getFloor())
                .status(room.getStatus())
                .statusDisplay(room.getStatus().getDisplayName())
                .notes(room.getNotes())
                .lastCleaned(room.getLastCleaned())
                .createdAt(room.getCreatedAt())
                .roomType(RoomTypeResponse.builder()
                        .id(type.getId())
                        .name(type.getName())
                        .description(type.getDescription())
                        .price(type.getPrice())

                        .maxOccupancy(type.getMaxOccupancy())
                        .bedType(type.getBedType())
                        .roomSize(type.getRoomSize())
                        .build())
                .build();
    }

    public List<RoomTypeResponse> getRoomTypesByBranchWithPromotions(Long branchId) {
        List<RoomTypeEntity> roomTypes = roomTypeRepository.findByBranchIdAndStatus(branchId, Status.ACTIVE);

        return roomTypes.stream().map(roomType -> {
            RoomTypeResponse response = RoomTypeResponse.builder()
                    .id(roomType.getId())
                    .branchId(roomType.getBranch().getId())
                    .branchName(roomType.getBranch().getName())
                    .name(roomType.getName())
                    .description(roomType.getDescription())
                    .price(roomType.getPrice())
                    .maxOccupancy(roomType.getMaxOccupancy())
                    .bedType(roomType.getBedType())
                    .roomSize(roomType.getRoomSize())
                    .amenities(roomType.getAmenities())
                    .status(roomType.getStatus())
                    .createdAt(roomType.getCreatedAt())
                    .build();

            // Lấy ảnh của loại phòng
            List<RoomImageEntity> images = roomImageRepository.findByRoomTypeIdOrderByDisplayOrder(roomType.getId());
            response.setRoomImages(images);

            // Tính giá sau giảm giá
            BigDecimal originalPrice = roomType.getPrice();
            BigDecimal discountedPrice = promotionService.calculateDiscountedPrice(
                    originalPrice, branchId, PromotionApplicability.ROOM);

            response.setPrice(discountedPrice);
            response.setFormattedPrice(formatPrice(discountedPrice));

            // Parse amenities JSON
            if (roomType.getAmenities() != null) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    List<String> amenitiesList = mapper.readValue(roomType.getAmenities(),
                            new TypeReference<List<String>>() {});
                    response.setAmenitiesList(amenitiesList);
                } catch (Exception e) {
                    response.setAmenitiesList(new ArrayList<>());
                }
            }

            return response;
        }).collect(Collectors.toList());
    }

    public List<RoomResponse> getRoomsByBranchWithDetails(Long branchId) {
        List<RoomEntity> rooms = roomRepository.findByRoomTypeBranchIdOrderByFloorAscRoomNumberAsc(branchId);

        return rooms.stream().map(room -> {
            return RoomResponse.builder()
                    .id(room.getId())
                    .roomTypeId(room.getRoomType().getId())
                    .roomTypeName(room.getRoomType().getName())
                    .roomNumber(room.getRoomNumber())
                    .floor(room.getFloor())
                    .status(room.getStatus())
                    .notes(room.getNotes())
                    .lastCleaned(room.getLastCleaned())
                    .createdAt(room.getCreatedAt())
                    .build();
        }).collect(Collectors.toList());
    }

    private String formatPrice(BigDecimal price) {
        DecimalFormat formatter = new DecimalFormat("#,###");
        return formatter.format(price) + " VNĐ";
    }



    //chi tiết loại phòng

    /**
     * Lấy thông tin chi tiết loại phòng theo ID
     */
    public RoomTypeResponse getRoomTypeById(Long id) {
        RoomTypeEntity roomType = roomTypeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Room type not found with id: " + id));

        return mapToRoomTypeResponse(roomType);
    }

    /**
     * Lấy danh sách hình ảnh của loại phòng
     */
    public List<RoomImageResponse> getRoomImagesByRoomType(Long roomTypeId) {
        List<RoomImageEntity> images = roomImageRepository.findByRoomTypeIdOrderByDisplayOrderAsc(roomTypeId);

        return images.stream()
                .map(this::mapToRoomImageResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy danh sách loại phòng tương tự trong cùng chi nhánh
     */
    public List<RoomTypeResponse> getSimilarRoomTypes(Long currentRoomTypeId, Long branchId) {
        List<RoomTypeEntity> similarRoomTypes = roomTypeRepository
                .findByBranchIdAndIdNotOrderByPriceAsc(branchId, currentRoomTypeId);

        return similarRoomTypes.stream()
                .limit(6) // Giới hạn 6 phòng tương tự
                .map(this::mapToRoomTypeResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy danh sách phòng có sẵn theo loại
     */
    public List<RoomResponse> getAvailableRoomsByType(Long roomTypeId) {
        List<RoomEntity> availableRooms = roomRepository
                .findByRoomTypeIdAndStatus(roomTypeId, RoomStatus.AVAILABLE);

        return availableRooms.stream()
                .map(this::mapToRoomResponse)
                .collect(Collectors.toList());
    }

    /**
     * Kiểm tra phòng trống theo loại và khoảng thời gian
     */
    public List<RoomResponse> getAvailableRoomsByTypeAndDate(Long roomTypeId, String checkInDate, String checkOutDate) {
        try {
            LocalDate checkIn = LocalDate.parse(checkInDate);
            LocalDate checkOut = LocalDate.parse(checkOutDate);

            // Tìm các phòng không có booking trong khoảng thời gian này
            List<RoomEntity> availableRooms = roomRepository
                    .findAvailableRoomsByTypeAndDateRange(roomTypeId, checkIn, checkOut);

            return availableRooms.stream()
                    .map(this::mapToRoomResponse)
                    .collect(Collectors.toList());

        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format");
        }
    }

    /**
     * Tính toán giá phòng theo khoảng thời gian
     */
    public RoomPricingResponse calculateRoomPricing(Long roomTypeId, String checkInDate, String checkOutDate) {
        try {
            LocalDate checkIn = LocalDate.parse(checkInDate);
            LocalDate checkOut = LocalDate.parse(checkOutDate);

            RoomTypeEntity roomType = roomTypeRepository.findById(roomTypeId)
                    .orElseThrow(() -> new EntityNotFoundException("Room type not found"));

            int nights = (int) ChronoUnit.DAYS.between(checkIn, checkOut);
            if (nights <= 0) {
                throw new IllegalArgumentException("Check-out date must be after check-in date");
            }

            // Lấy giá hiện tại (đã tính khuyến mãi)
            BigDecimal currentPrice = getCurrentPriceWithPromotion(roomType);
            BigDecimal baseTotal = currentPrice.multiply(BigDecimal.valueOf(nights));

            // Tính phí dịch vụ 10%
            BigDecimal serviceFee = baseTotal.multiply(BigDecimal.valueOf(0.1));

            // Tính VAT 10%
            BigDecimal vat = baseTotal.add(serviceFee).multiply(BigDecimal.valueOf(0.1));

            // Tổng tiền
            BigDecimal totalPrice = baseTotal.add(serviceFee).add(vat);

            return RoomPricingResponse.builder()
                    .roomTypeId(roomTypeId)
                    .basePrice(baseTotal)
                    .serviceFee(serviceFee)
                    .vat(vat)
                    .totalPrice(totalPrice)
                    .nights(nights)
                    .checkInDate(checkInDate)
                    .checkOutDate(checkOutDate)
                    .build();

        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format");
        }
    }

    // Helper methods
    private RoomTypeResponse mapToRoomTypeResponse(RoomTypeEntity entity) {
        // Lấy thông tin khuyến mãi
        BigDecimal currentPrice = getCurrentPriceWithPromotion(entity);
        BigDecimal originalPrice = null;
        String promotionName = null;

        if (currentPrice.compareTo(entity.getPrice()) < 0) {
            originalPrice = entity.getPrice();
            // Lấy thông tin promotion (implement logic tùy theo business)
        }



        // Lấy số lượng phòng available
        Integer availableRooms = roomRepository.countByRoomTypeIdAndStatus(entity.getId(), RoomStatus.AVAILABLE);

        // Lấy danh sách hình ảnh
        List<RoomImageResponse> roomImages = getRoomImagesByRoomType(entity.getId());

        return RoomTypeResponse.builder()
                .id(entity.getId())
                .branchId(entity.getBranch().getId())
                .branchName(entity.getBranch().getName())
                .name(entity.getName())
                .description(entity.getDescription())
                .price(entity.getPrice())
                .currentPrice(currentPrice)
                .originalPrice(originalPrice)
                .bedType(entity.getBedType())
                .maxOccupancy(entity.getMaxOccupancy())
                .roomSize(entity.getRoomSize())
//                .viewType(entity.getViewType())
                .amenities(entity.getAmenities())
//                .isAvailable(entity.getIsAvailable())
                .createdAt(entity.getCreatedAt())
                .roomImageResponses(roomImages)
                .availableRooms(availableRooms)
                .promotionName(promotionName)
                .build();
    }

    private RoomImageResponse mapToRoomImageResponse(RoomImageEntity entity) {
        return RoomImageResponse.builder()
                .id(entity.getId())
                .roomTypeId(entity.getRoomType().getId())
                .imageUrl(entity.getImageUrl())
                .imageTitle(entity.getImageTitle())
                .isPrimary(entity.getIsPrimary())
                .displayOrder(entity.getDisplayOrder())
                .uploadedAt(entity.getUploadedAt())
                .build();
    }

    private RoomResponse mapToRoomResponse(RoomEntity entity) {
        return RoomResponse.builder()
                .id(entity.getId())
                .roomTypeId(entity.getRoomType().getId())
                .roomTypeName(entity.getRoomType().getName())
                .roomNumber(entity.getRoomNumber())
                .floor(entity.getFloor())
                .status(entity.getStatus())
                .notes(entity.getNotes())
                .lastCleaned(entity.getLastCleaned())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private BigDecimal getCurrentPriceWithPromotion(RoomTypeEntity roomType) {
        // Logic để tính giá sau khuyến mãi
        // Tạm thời return base price, có thể implement logic promotion sau
        return roomType.getPrice();
    }
}