package com.example.htql_nhahang_khachsan.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;

import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import com.example.htql_nhahang_khachsan.dto.*;
import com.example.htql_nhahang_khachsan.enums.BranchStatus;
import com.example.htql_nhahang_khachsan.enums.BranchType;
import com.example.htql_nhahang_khachsan.service.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class BranchDetailController {

    private final BranchService branchService;
    private final RoomService roomService;
    private final MenuService menuService;
    private final RestaurantTableService tableService;
    private final PromotionService promotionService;
    private final TableBookingService bookingService;
    private final AuthService authService;

//    @GetMapping("/branches/{id}")
//    public String showBranchDetail(@PathVariable Long id, Model model) {
//        try {
//            BranchResponse branch = branchService.getBranchById(id);
//
//            // Chỉ hiển thị chi nhánh đang hoạt động
//            if (branch.getStatus() != BranchStatus.ACTIVE) {
//                return "redirect:/";
//            }
//
//            model.addAttribute("branch", branch);
//
//            // Dựa vào loại chi nhánh để load dữ liệu
//            BranchType branchType = branch.getType();
//
//            // Nếu có dịch vụ khách sạn (HOTEL hoặc BOTH)
////            if (branchType == BranchType.HOTEL || branchType == BranchType.BOTH) {
////                // Lấy danh sách loại phòng với giá sau giảm giá
////                List<RoomTypeResponse> roomTypes = roomService.getRoomTypesByBranchWithPromotions(id);
////                model.addAttribute("roomTypes", roomTypes);
////
////                // Lấy danh sách phòng với thông tin loại phòng và ảnh
////                List<RoomResponse> rooms = roomService.getRoomsByBranchWithDetails(id);
////                model.addAttribute("rooms", rooms);
////            }
//
//            // Trong BranchDetailController.java
//            // Thay đổi để truyền empty list thay vì null
//            if (branchType == BranchType.HOTEL || branchType == BranchType.BOTH) {
//                List<RoomTypeResponse> roomTypes = roomService.getRoomTypesByBranchWithPromotions(id);
//                model.addAttribute("roomTypes", roomTypes != null ? roomTypes : new ArrayList<>());
//
//                List<RoomResponse> rooms = roomService.getRoomsByBranchWithDetails(id);
//                model.addAttribute("rooms", rooms != null ? rooms : new ArrayList<>());
//            } else {
//                model.addAttribute("roomTypes", new ArrayList<>());
//                model.addAttribute("rooms", new ArrayList<>());
//            }
//
//            // Nếu có dịch vụ nhà hàng (RESTAURANT hoặc BOTH)
//            if (branchType == BranchType.RESTAURANT || branchType == BranchType.BOTH) {
//                // Lấy danh sách danh mục món ăn
//                List<MenuCategoryResponse> menuCategories = menuService.getCategoriesByBranch(id);
//                model.addAttribute("menuCategories", menuCategories);
//
//                // Lấy danh sách món ăn với giá sau giảm giá
//                List<MenuItemResponse> menuItems = menuService.getMenuItemsByBranchWithPromotions(id);
//                model.addAttribute("menuItems", menuItems);
//
//                // Lấy danh sách bàn
//                List<TableResponse> tables = tableService.getTablesByBranch(id);
//                model.addAttribute("tables", tables);
//            }
//
//            // Lấy danh sách khuyến mãi đang áp dụng cho chi nhánh này
//            List<PromotionResponse> activePromotions = promotionService.getActivePromotionsByBranch(id);
//            model.addAttribute("promotions", activePromotions);
//
//            return "customer/branches/detail";
//        } catch (Exception e) {
//            return "redirect:/";
//        }
//    }

    @GetMapping("/branches/{id}")
    public String showBranchDetail(@PathVariable Long id, Model model) {
        try {
            BranchResponse branch = branchService.getBranchById(id);

            if (branch.getStatus() != BranchStatus.ACTIVE) {
                return "redirect:/";
            }

            model.addAttribute("branch", branch);
            BranchType branchType = branch.getType();

            if (branchType == BranchType.HOTEL || branchType == BranchType.BOTH) {
                List<RoomTypeResponse> roomTypes = roomService.getRoomTypesByBranchWithPromotions(id);
                model.addAttribute("roomTypes", roomTypes != null ? roomTypes : new ArrayList<>());

                List<RoomResponse> rooms = roomService.getRoomsByBranchWithDetails(id);
                model.addAttribute("rooms", rooms != null ? rooms : new ArrayList<>());
            } else {
                model.addAttribute("roomTypes", new ArrayList<>());
                model.addAttribute("rooms", new ArrayList<>());
            }

            if (branchType == BranchType.RESTAURANT || branchType == BranchType.BOTH) {
                List<MenuCategoryResponse> menuCategories = menuService.getCategoriesByBranch(id);
                model.addAttribute("menuCategories", menuCategories);

                List<MenuItemResponse> menuItems = menuService.getMenuItemsByBranchWithPromotions(id);
                model.addAttribute("menuItems", menuItems);

                List<TableResponse> tables = tableService.getTablesByBranch(id);
                model.addAttribute("tables", tables);

                // Thêm form đặt bàn rỗng
                model.addAttribute("bookingRequest", new TableBookingRequest());
            }

            List<PromotionResponse> activePromotions = promotionService.getActivePromotionsByBranch(id);
            model.addAttribute("promotions", activePromotions);

            return "customer/branches/detail";
        } catch (Exception e) {
            return "redirect:/";
        }
    }

    // API endpoint để lấy dữ liệu động qua AJAX
    @GetMapping("/api/branches/{id}/rooms")
    @ResponseBody
    public List<RoomResponse> getRoomsByBranch(@PathVariable Long id) {
        return roomService.getRoomsByBranchWithDetails(id);
    }

    @GetMapping("/api/branches/{id}/menu")
    @ResponseBody
    public List<MenuItemResponse> getMenuByBranch(@PathVariable Long id,
                                                  @RequestParam(required = false) Long categoryId) {
        if (categoryId != null) {
            return menuService.getMenuItemsByCategory(categoryId);
        }
        return menuService.getMenuItemsByBranchWithPromotions(id);
    }

    @GetMapping("/api/branches/{id}/tables")
    @ResponseBody
    public List<TableResponse> getTablesByBranch(@PathVariable Long id) {
        return tableService.getTablesByBranch(id);
    }

    @GetMapping("/api/branches/{id}/promotions")
    @ResponseBody
    public List<PromotionResponse> getPromotionsByBranch(@PathVariable Long id) {
        return promotionService.getActivePromotionsByBranch(id);
    }





    // API gợi ý bàn phù hợp
    @GetMapping("/api/branches/{id}/table-suggestions")
    @ResponseBody
    public TableSuggestionResponse getTableSuggestions(
            @PathVariable Long id,
            @RequestParam Integer partySize,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime time) {
        return bookingService.getSuggestedTables(id, partySize, date, time);
    }

    // Xử lý đặt bàn
    @PostMapping("/branches/{id}/book-table")
    public String bookTable(@PathVariable Long id,
                            @Valid @ModelAttribute("bookingRequest") TableBookingRequest request,
                            BindingResult bindingResult,
                            HttpSession session,
                            RedirectAttributes redirectAttributes,
                            Model model) {

        // Kiểm tra đăng nhập
        if (!authService.isLoggedIn(session)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng đăng nhập để đặt bàn");
            return "redirect:/login?redirect=/branches/" + id;
        }

        if (bindingResult.hasErrors()) {
            // Nếu có lỗi validation, load lại trang với thông báo lỗi
            try {
                BranchResponse branch = branchService.getBranchById(id);
                model.addAttribute("branch", branch);

                if (branch.getType() == BranchType.RESTAURANT || branch.getType() == BranchType.BOTH) {
                    List<MenuCategoryResponse> menuCategories = menuService.getCategoriesByBranch(id);
                    model.addAttribute("menuCategories", menuCategories);

                    List<MenuItemResponse> menuItems = menuService.getMenuItemsByBranchWithPromotions(id);
                    model.addAttribute("menuItems", menuItems);

                    List<TableResponse> tables = tableService.getTablesByBranch(id);
                    model.addAttribute("tables", tables);
                }

                model.addAttribute("errorMessage", "Vui lòng kiểm tra lại thông tin đặt bàn");
                return "customer/branches/detail";
            } catch (Exception e) {
                return "redirect:/branches/" + id;
            }
        }

        try {
            Long customerId = authService.getCurrentUserId(session);
            TableBookingResponse booking = bookingService.createBooking(id, request, customerId);

            redirectAttributes.addFlashAttribute("successMessage",
                    "Đặt bàn thành công! Mã đặt bàn: " + booking.getBookingCode() +
                            ". Chúng tôi sẽ xác nhận trong thời gian sớm nhất.");

            return "redirect:/branches/" + id + "?bookingSuccess=true";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Đặt bàn thất bại: " + e.getMessage());
            return "redirect:/branches/" + id;
        }
    }


}
