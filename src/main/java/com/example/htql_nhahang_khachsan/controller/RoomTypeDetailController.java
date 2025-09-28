package com.example.htql_nhahang_khachsan.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.example.htql_nhahang_khachsan.dto.*;
import com.example.htql_nhahang_khachsan.service.*;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class RoomTypeDetailController {

    private final RoomService roomService;
    private final BranchService branchService;

    @GetMapping("/room-types/{id}")
    public String showRoomTypeDetail(@PathVariable Long id, Model model) {
        try {
            // Lấy thông tin loại phòng
            RoomTypeResponse roomType = roomService.getRoomTypeById(id);
            if (roomType == null) {
                return "redirect:/";
            }

            model.addAttribute("roomType", roomType);

            // Lấy thông tin chi nhánh
            BranchResponse branch = branchService.getBranchById(roomType.getBranchId());
            model.addAttribute("branch", branch);

            // Lấy danh sách hình ảnh của loại phòng
            List<RoomImageResponse> roomImages = roomService.getRoomImagesByRoomType(id);
            model.addAttribute("roomImages", roomImages);

            // Lấy danh sách phòng cùng loại trong chi nhánh (để hiển thị phòng tương tự)
            List<RoomTypeResponse> similarRoomTypes = roomService.getSimilarRoomTypes(id, roomType.getBranchId());
            model.addAttribute("similarRoomTypes", similarRoomTypes);

            // Lấy danh sách phòng có sẵn của loại này
            List<RoomResponse> availableRooms = roomService.getAvailableRoomsByType(id);
            model.addAttribute("availableRooms", availableRooms);

            return "customer/room-types/detail";
        } catch (Exception e) {
            return "redirect:/";
        }
    }

    // API để check phòng trống theo ngày
    @GetMapping("/api/room-types/{id}/availability")
    @ResponseBody
    public List<RoomResponse> checkRoomAvailability(
            @PathVariable Long id,
            @RequestParam String checkInDate,
            @RequestParam String checkOutDate) {
        return roomService.getAvailableRoomsByTypeAndDate(id, checkInDate, checkOutDate);
    }

    // API để lấy giá phòng theo ngày
    @GetMapping("/api/room-types/{id}/pricing")
    @ResponseBody
    public RoomPricingResponse getRoomPricing(
            @PathVariable Long id,
            @RequestParam String checkInDate,
            @RequestParam String checkOutDate) {
        return roomService.calculateRoomPricing(id, checkInDate, checkOutDate);
    }
}