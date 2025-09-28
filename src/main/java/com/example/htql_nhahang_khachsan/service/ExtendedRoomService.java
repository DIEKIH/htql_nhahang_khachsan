//package com.example.htql_nhahang_khachsan.service;
//
//import com.example.htql_nhahang_khachsan.dto.CurrentBookingInfo;
//import com.example.htql_nhahang_khachsan.dto.MaintenanceRecord;
//import com.example.htql_nhahang_khachsan.dto.RoomDetailResponse;
//import com.example.htql_nhahang_khachsan.dto.RoomTypeResponse;
//import com.example.htql_nhahang_khachsan.entity.RoomEntity;
//import com.example.htql_nhahang_khachsan.enums.RoomStatus;
//import com.example.htql_nhahang_khachsan.repository.RoomRepository;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//
//
//@Service
//@RequiredArgsConstructor
//public class ExtendedRoomService {
//
//    private final RoomRepository roomRepository;
//    private final MaintenanceRepository maintenanceRepository;
//    private final BookingRepository bookingRepository;
//
//    public RoomDetailResponse getRoomDetailById(Long roomId) {
//        RoomEntity room = roomRepository.findById(roomId)
//                .orElseThrow(() -> new EntityNotFoundException("Room not found"));
//
//        RoomDetailResponse response = RoomDetailResponse.builder()
//                .id(room.getId())
//                .roomNumber(room.getRoomNumber())
//                .floor(room.getFloor())
//                .status(room.getStatus())
//                .statusDisplay(room.getStatus().getDisplayName())
//                .notes(room.getNotes())
//                .lastCleaned(room.getLastCleaned())
//                .createdAt(room.getCreatedAt())
//                .build();
//
//        // Get room type information
//        RoomTypeResponse roomTypeResponse = RoomTypeResponse.builder()
//                .id(room.getRoomType().getId())
//                .name(room.getRoomType().getName())
//                .description(room.getRoomType().getDescription())
//                .price(room.getRoomType().getPrice())
//                .maxOccupancy(room.getRoomType().getMaxOccupancy())
//                .bedType(room.getRoomType().getBedType())
//                .roomSize(room.getRoomType().getRoomSize())
//                .build();
//        response.setRoomType(roomTypeResponse);
//
//        // Get current booking if room is occupied
//        if (room.getStatus() == RoomStatus.OCCUPIED) {
//            CurrentBookingInfo booking = getCurrentBookingByRoom(roomId);
//            response.setCurrentBooking(booking);
//        }
//
//        // Get maintenance history
//        List<MaintenanceRecord> maintenanceHistory = getMaintenanceHistory(roomId);
//        response.setMaintenanceHistory(maintenanceHistory);
//
//        return response;
//    }
//
//    private CurrentBookingInfo getCurrentBookingByRoom(Long roomId) {
//        // Implementation to get current booking
//        return CurrentBookingInfo.builder()
//                .bookingId(1L)
//                .guestName("Nguyen Van A")
//                .checkIn(LocalDateTime.now().minusHours(2))
//                .checkOut(LocalDateTime.now().plusDays(1))
//                .bookingStatus("ACTIVE")
//                .build();
//    }
//
//    private List<MaintenanceRecord> getMaintenanceHistory(Long roomId) {
//        // Implementation to get maintenance history
//        return Arrays.asList(
//                MaintenanceRecord.builder()
//                        .date(LocalDateTime.now().minusDays(3))
//                        .type("Vệ sinh")
//                        .description("Vệ sinh tổng quát phòng")
//                        .technician("Nguyen Van B")
//                        .build(),
//                MaintenanceRecord.builder()
//                        .date(LocalDateTime.now().minusDays(7))
//                        .type("Sửa chữa")
//                        .description("Thay bóng đèn")
//                        .technician("Le Thi C")
//                        .build()
//        );
//    }
//}