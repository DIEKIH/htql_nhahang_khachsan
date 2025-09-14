package com.example.htql_nhahang_khachsan.controller;

import com.example.htql_nhahang_khachsan.dto.RoomRequest;
import com.example.htql_nhahang_khachsan.dto.RoomResponse;
import com.example.htql_nhahang_khachsan.dto.RoomTypeResponse;
import com.example.htql_nhahang_khachsan.enums.RoomStatus;
import com.example.htql_nhahang_khachsan.service.AuthService;
import com.example.htql_nhahang_khachsan.service.ManagerRoomService;
import com.example.htql_nhahang_khachsan.service.ManagerRoomTypeService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/manager")
@RequiredArgsConstructor
public class ManagerRoomController {

    private final AuthService authService;
    private final ManagerRoomService roomService;
    private final ManagerRoomTypeService roomTypeService;


    // === ROOM MANAGEMENT ===
    @GetMapping("/rooms")
    public String roomList(Model model, HttpSession session,
                           @RequestParam(required = false) String roomNumber,
                           @RequestParam(required = false) RoomStatus status,
                           @RequestParam(required = false) Long roomTypeId,
                           @RequestParam(required = false) Integer floor) {
        if (!authService.isLoggedIn(session) || !authService.isManager(session)) {
            return "redirect:/manager/login";
        }

        Long branchId = authService.getCurrentUserBranchId(session);

        List<RoomResponse> rooms;
        if (roomNumber != null || status != null || roomTypeId != null || floor != null) {
            rooms = roomService.searchRooms(branchId, roomNumber, status, roomTypeId, floor);
        } else {
            rooms = roomService.getAllRoomsByBranch(branchId);
        }

        List<RoomTypeResponse> roomTypes = roomTypeService.getAllActiveRoomTypesByBranch(branchId);

        model.addAttribute("rooms", rooms);
        model.addAttribute("roomTypes", roomTypes);
        model.addAttribute("statuses", RoomStatus.values());
        model.addAttribute("searchRoomNumber", roomNumber);
        model.addAttribute("searchStatus", status);
        model.addAttribute("searchRoomTypeId", roomTypeId);
        model.addAttribute("searchFloor", floor);

        return "manager/rooms/list";
    }

    @GetMapping("/rooms/add")
    public String addRoomForm(Model model, HttpSession session) {
        if (!authService.isLoggedIn(session) || !authService.isManager(session)) {
            return "redirect:/manager/login";
        }

        Long branchId = authService.getCurrentUserBranchId(session);
        List<RoomTypeResponse> roomTypes = roomTypeService.getAllActiveRoomTypesByBranch(branchId);

        model.addAttribute("roomRequest", new RoomRequest());
        model.addAttribute("roomTypes", roomTypes);
        model.addAttribute("statuses", RoomStatus.values());

        return "manager/rooms/form";
    }

    @PostMapping("/rooms/add")
    public String addRoom(@Valid @ModelAttribute RoomRequest request,
                          BindingResult result,
                          Model model,
                          RedirectAttributes redirectAttributes,
                          HttpSession session) {
        if (!authService.isLoggedIn(session) || !authService.isManager(session)) {
            return "redirect:/manager/login";
        }

        if (result.hasErrors()) {
            Long branchId = authService.getCurrentUserBranchId(session);
            List<RoomTypeResponse> roomTypes = roomTypeService.getAllActiveRoomTypesByBranch(branchId);
            model.addAttribute("roomTypes", roomTypes);
            model.addAttribute("statuses", RoomStatus.values());
            return "manager/rooms/form";
        }

        try {
            Long branchId = authService.getCurrentUserBranchId(session);
            roomService.createRoom(branchId, request);
            redirectAttributes.addFlashAttribute("success", "Thêm phòng thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/manager/rooms";
    }

    @GetMapping("/rooms/{id}/edit")
    public String editRoomForm(@PathVariable Long id, Model model, HttpSession session) {
        if (!authService.isLoggedIn(session) || !authService.isManager(session)) {
            return "redirect:/manager/login";
        }

        try {
            Long branchId = authService.getCurrentUserBranchId(session);
            RoomResponse room = roomService.getRoomById(id, branchId);
            List<RoomTypeResponse> roomTypes = roomTypeService.getAllActiveRoomTypesByBranch(branchId);

            RoomRequest roomRequest = new RoomRequest();
            roomRequest.setRoomTypeId(room.getRoomTypeId());
            roomRequest.setRoomNumber(room.getRoomNumber());
            roomRequest.setFloor(room.getFloor());
            roomRequest.setStatus(room.getStatus());
            roomRequest.setNotes(room.getNotes());

            model.addAttribute("roomRequest", roomRequest);
            model.addAttribute("roomId", id);
            model.addAttribute("roomTypes", roomTypes);
            model.addAttribute("statuses", RoomStatus.values());

            return "manager/rooms/form";
        } catch (Exception e) {
            return "redirect:/manager/rooms";
        }
    }

    @PostMapping("/rooms/{id}/edit")
    public String updateRoom(@PathVariable Long id,
                             @Valid @ModelAttribute RoomRequest request,
                             BindingResult result,
                             Model model,
                             RedirectAttributes redirectAttributes,
                             HttpSession session) {
        if (!authService.isLoggedIn(session) || !authService.isManager(session)) {
            return "redirect:/manager/login";
        }

        if (result.hasErrors()) {
            Long branchId = authService.getCurrentUserBranchId(session);
            List<RoomTypeResponse> roomTypes = roomTypeService.getAllActiveRoomTypesByBranch(branchId);
            model.addAttribute("roomId", id);
            model.addAttribute("roomTypes", roomTypes);
            model.addAttribute("statuses", RoomStatus.values());
            return "manager/rooms/form";
        }

        try {
            Long branchId = authService.getCurrentUserBranchId(session);
            roomService.updateRoom(id, branchId, request);
            redirectAttributes.addFlashAttribute("success", "Cập nhật phòng thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/manager/rooms";
    }

    @PostMapping("/rooms/{id}/delete")
    public String deleteRoom(@PathVariable Long id,
                             RedirectAttributes redirectAttributes,
                             HttpSession session) {
        if (!authService.isLoggedIn(session) || !authService.isManager(session)) {
            return "redirect:/manager/login";
        }

        try {
            Long branchId = authService.getCurrentUserBranchId(session);
            roomService.deleteRoom(id, branchId);
            redirectAttributes.addFlashAttribute("success", "Xóa phòng thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/manager/rooms";
    }

    @PostMapping("/rooms/{id}/toggle-status")
    public String toggleRoomStatus(@PathVariable Long id,
                                   RedirectAttributes redirectAttributes,
                                   HttpSession session) {
        if (!authService.isLoggedIn(session) || !authService.isManager(session)) {
            return "redirect:/manager/login";
        }

        try {
            Long branchId = authService.getCurrentUserBranchId(session);
            roomService.toggleRoomStatus(id, branchId);
            redirectAttributes.addFlashAttribute("success", "Cập nhật trạng thái phòng thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/manager/rooms";
    }


}