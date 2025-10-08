package com.example.htql_nhahang_khachsan.controller;

import com.example.htql_nhahang_khachsan.dto.TimelineBookingDTO;
import com.example.htql_nhahang_khachsan.dto.TimelineRoomDTO;
import com.example.htql_nhahang_khachsan.entity.RoomBookingEntity;
import com.example.htql_nhahang_khachsan.entity.RoomEntity;
import com.example.htql_nhahang_khachsan.enums.BookingStatus;
import com.example.htql_nhahang_khachsan.repository.RoomBookingRepository;
import com.example.htql_nhahang_khachsan.repository.RoomRepository;
import com.example.htql_nhahang_khachsan.service.AuthService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/manager/rooms-timeline")
@RequiredArgsConstructor
public class RoomTimelineController {

    private final AuthService authService;
    private final RoomRepository roomRepository;
    private final RoomBookingRepository bookingRepository;

    @GetMapping
    public String showTimeline(HttpSession session, Model model) {
        if (!authService.isLoggedIn(session) || !authService.isManager(session)) {
            return "redirect:/manager/login";
        }

        Long branchId = authService.getCurrentUserBranchId(session);

        // Lấy tất cả phòng của chi nhánh
        List<RoomEntity> rooms = roomRepository.findByBranchId(branchId);

        // Lấy loại phòng unique
        Set<String> roomTypes = rooms.stream()
                .map(r -> r.getRoomType().getName())
                .collect(Collectors.toSet());

        model.addAttribute("rooms", rooms);
        model.addAttribute("roomTypes", roomTypes);

        return "manager/rooms/room_realtime";
    }

    @GetMapping("/api/bookings")
    @ResponseBody
    public ResponseEntity<List<TimelineBookingDTO>> getBookings(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(required = false) Long roomTypeId,
            HttpSession session) {

        if (!authService.isLoggedIn(session) || !authService.isManager(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Long branchId = authService.getCurrentUserBranchId(session);
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);

        List<RoomBookingEntity> bookings = bookingRepository.findByBranchIdAndCheckInDateBetween(
                branchId, start, end);

        // Filter by room type if specified
        if (roomTypeId != null) {
            bookings = bookings.stream()
                    .filter(b -> b.getRoomType().getId().equals(roomTypeId))
                    .collect(Collectors.toList());
        }

        List<TimelineBookingDTO> result = bookings.stream()
                .map(this::convertToTimelineDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/api/rooms")
    @ResponseBody
    public ResponseEntity<List<TimelineRoomDTO>> getRooms(
            @RequestParam(required = false) Long roomTypeId,
            HttpSession session) {

        if (!authService.isLoggedIn(session) || !authService.isManager(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Long branchId = authService.getCurrentUserBranchId(session);
        List<RoomEntity> rooms = roomRepository.findByBranchId(branchId);

        if (roomTypeId != null) {
            rooms = rooms.stream()
                    .filter(r -> r.getRoomType().getId().equals(roomTypeId))
                    .collect(Collectors.toList());
        }

        List<TimelineRoomDTO> result = rooms.stream()
                .map(this::convertToRoomDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @PostMapping("/api/move-booking")
    @ResponseBody
    public ResponseEntity<Map<String, String>> moveBooking(
            @RequestParam Long bookingId,
            @RequestParam Long newRoomId,
            @RequestParam String newStartDate,
            HttpSession session) {

        if (!authService.isLoggedIn(session) || !authService.isManager(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            Long branchId = authService.getCurrentUserBranchId(session);
            RoomBookingEntity booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new EntityNotFoundException("Booking not found"));

            if (!booking.getBranch().getId().equals(branchId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            RoomEntity newRoom = roomRepository.findById(newRoomId)
                    .orElseThrow(() -> new EntityNotFoundException("Room not found"));

            LocalDate newStart = LocalDate.parse(newStartDate);
            int duration = (int) ChronoUnit.DAYS.between(booking.getCheckInDate(), booking.getCheckOutDate());
            LocalDate newEnd = newStart.plusDays(duration);

            // Kiểm tra xung đột
            boolean hasConflict = bookingRepository.existsBookingForRoomInDateRange(
                    newRoomId, newStart, newEnd,
                    Arrays.asList(BookingStatus.CANCELLED, BookingStatus.NO_SHOW));

            if (hasConflict) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Phòng đã được đặt trong khoảng thời gian này"));
            }

            booking.setRoom(newRoom);
            booking.setCheckInDate(newStart);
            booking.setCheckOutDate(newEnd);
            bookingRepository.save(booking);

            return ResponseEntity.ok(Map.of("success", "Đã cập nhật booking"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    private TimelineBookingDTO convertToTimelineDTO(RoomBookingEntity entity) {
        return TimelineBookingDTO.builder()
                .id(entity.getId())
                .bookingCode(entity.getBookingCode())
                .guestName(entity.getGuestName())
                .roomId(entity.getRoom() != null ? entity.getRoom().getId() : null)
                .roomNumber(entity.getRoom() != null ? entity.getRoom().getRoomNumber() : "Chưa gán")
                .roomTypeId(entity.getRoomType().getId())
                .roomTypeName(entity.getRoomType().getName())
                .checkInDate(entity.getCheckInDate())
                .checkOutDate(entity.getCheckOutDate())
                .status(entity.getStatus())
                .paymentStatus(entity.getPaymentStatus())
                .build();
    }

    private TimelineRoomDTO convertToRoomDTO(RoomEntity entity) {
        return TimelineRoomDTO.builder()
                .id(entity.getId())
                .roomNumber(entity.getRoomNumber())
                .floor(entity.getFloor())
                .roomTypeId(entity.getRoomType().getId())
                .roomTypeName(entity.getRoomType().getName())
                .status(entity.getStatus())
                .build();
    }
}
