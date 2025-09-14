package com.example.htql_nhahang_khachsan.controller;

import com.example.htql_nhahang_khachsan.dto.RoomTypeRequest;
import com.example.htql_nhahang_khachsan.dto.RoomTypeResponse;
import com.example.htql_nhahang_khachsan.enums.Status;
import com.example.htql_nhahang_khachsan.service.AuthService;
import com.example.htql_nhahang_khachsan.service.ManagerRoomTypeService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/manager")
@RequiredArgsConstructor
public class ManagerRoomTypeController {

    private final AuthService authService;
    private final ManagerRoomTypeService roomTypeService;

    // === ROOM TYPE MANAGEMENT ===
    @GetMapping("/room-types")
    public String roomTypeList(Model model, HttpSession session,
                               @RequestParam(required = false) String name,
                               @RequestParam(required = false) Status status) {
        if (!authService.isLoggedIn(session) || !authService.isManager(session)) {
            return "redirect:/manager/login";
        }

        // Lấy chi nhánh của manager
        Long branchId = authService.getCurrentUserBranchId(session);

        List<RoomTypeResponse> roomTypes;
        if (name != null || status != null) {
            roomTypes = roomTypeService.searchRoomTypes(branchId, name, status);
        } else {
            roomTypes = roomTypeService.getAllRoomTypesByBranch(branchId);
        }

        model.addAttribute("roomTypes", roomTypes);
        model.addAttribute("statuses", Status.values());
        model.addAttribute("searchName", name);
        model.addAttribute("searchStatus", status);

        return "manager/room_types/list";
    }

    @GetMapping("/room-types/add")
    public String addRoomTypeForm(Model model, HttpSession session) {
        if (!authService.isLoggedIn(session) || !authService.isManager(session)) {
            return "redirect:/manager/login";
        }

        model.addAttribute("roomTypeRequest", new RoomTypeRequest());
        model.addAttribute("statuses", Status.values());

        return "manager/room_types/form";
    }

    @PostMapping("/room-types/add")
    public String addRoomType(@Valid @ModelAttribute RoomTypeRequest request,
                              BindingResult result,
                              @RequestParam(value = "imageFiles", required = false) List<MultipartFile> imageFiles,
                              Model model,
                              RedirectAttributes redirectAttributes,
                              HttpSession session) {
        if (!authService.isLoggedIn(session) || !authService.isManager(session)) {
            return "redirect:/manager/login";
        }

        if (result.hasErrors()) {
            model.addAttribute("statuses", Status.values());
            return "manager/room_types/form";
        }

        try {
            Long branchId = authService.getCurrentUserBranchId(session);
            roomTypeService.createRoomType(branchId, request, imageFiles);
            redirectAttributes.addFlashAttribute("success", "Thêm loại phòng thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/manager/room-types";
    }

    @GetMapping("/room-types/{id}/edit")
    public String editRoomTypeForm(@PathVariable Long id, Model model, HttpSession session) {
        if (!authService.isLoggedIn(session) || !authService.isManager(session)) {
            return "redirect:/manager/login";
        }

        try {
            Long branchId = authService.getCurrentUserBranchId(session);
            RoomTypeResponse roomType = roomTypeService.getRoomTypeById(id, branchId);

            RoomTypeRequest roomTypeRequest = new RoomTypeRequest();
            roomTypeRequest.setName(roomType.getName());
            roomTypeRequest.setDescription(roomType.getDescription());
            roomTypeRequest.setPrice(roomType.getPrice());
            roomTypeRequest.setMaxOccupancy(roomType.getMaxOccupancy());
            roomTypeRequest.setBedType(roomType.getBedType());
            roomTypeRequest.setRoomSize(roomType.getRoomSize());
            roomTypeRequest.setAmenities(roomType.getAmenities());
            roomTypeRequest.setStatus(roomType.getStatus());

            model.addAttribute("roomTypeRequest", roomTypeRequest);
            model.addAttribute("roomTypeId", id);
            model.addAttribute("statuses", Status.values());
            model.addAttribute("existingImages", roomType.getRoomImages());

            return "manager/room_types/form";
        } catch (Exception e) {
            return "redirect:/manager/room-types";
        }
    }

    @PostMapping("/room-types/{id}/edit")
    public String updateRoomType(@PathVariable Long id,
                                 @Valid @ModelAttribute RoomTypeRequest request,
                                 BindingResult result,
                                 @RequestParam(value = "imageFiles", required = false) List<MultipartFile> imageFiles,
                                 @RequestParam(value = "deleteImageIds", required = false) List<Long> deleteImageIds,
                                 Model model,
                                 RedirectAttributes redirectAttributes,
                                 HttpSession session) {
        if (!authService.isLoggedIn(session) || !authService.isManager(session)) {
            return "redirect:/manager/login";
        }

        if (result.hasErrors()) {
            model.addAttribute("roomTypeId", id);
            model.addAttribute("statuses", Status.values());

            // Reload existing images for display
            try {
                Long branchId = authService.getCurrentUserBranchId(session);
                RoomTypeResponse roomType = roomTypeService.getRoomTypeById(id, branchId);
                model.addAttribute("existingImages", roomType.getRoomImages());
            } catch (Exception e) {
                // Handle silently
            }

            return "manager/room_types/form";
        }

        try {
            Long branchId = authService.getCurrentUserBranchId(session);
            roomTypeService.updateRoomType(id, branchId, request, imageFiles, deleteImageIds);
            redirectAttributes.addFlashAttribute("success", "Cập nhật loại phòng thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/manager/room-types";
    }

    @PostMapping("/room-types/{id}/delete")
    public String deleteRoomType(@PathVariable Long id,
                                 RedirectAttributes redirectAttributes,
                                 HttpSession session) {
        if (!authService.isLoggedIn(session) || !authService.isManager(session)) {
            return "redirect:/manager/login";
        }

        try {
            Long branchId = authService.getCurrentUserBranchId(session);
            roomTypeService.deleteRoomType(id, branchId);
            redirectAttributes.addFlashAttribute("success", "Ẩn loại phòng thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/manager/room-types";
    }
}
