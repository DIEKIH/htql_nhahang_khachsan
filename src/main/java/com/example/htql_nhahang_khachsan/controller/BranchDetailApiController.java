package com.example.htql_nhahang_khachsan.controller;

import com.example.htql_nhahang_khachsan.dto.*;
import com.example.htql_nhahang_khachsan.service.MenuService;
import com.example.htql_nhahang_khachsan.service.PromotionService;
import com.example.htql_nhahang_khachsan.service.RestaurantTableService;
import com.example.htql_nhahang_khachsan.service.RoomService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BranchDetailApiController {

    private final RoomService roomService;
    private final MenuService menuService;
    private final RestaurantTableService tableService;
    private final PromotionService promotionService;



    // Room related APIs
    @GetMapping("/rooms/{roomId}/details")
    public ResponseEntity<RoomDetailResponse> getRoomDetails(@PathVariable Long roomId) {
        try {
            RoomDetailResponse roomDetail = roomService.getRoomDetailById(roomId);
            return ResponseEntity.ok(roomDetail);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/branches/{branchId}/room-types")
    public ResponseEntity<List<RoomTypeResponse>> getRoomTypesByBranch(@PathVariable Long branchId) {
        List<RoomTypeResponse> roomTypes = roomService.getRoomTypesByBranchWithPromotions(branchId);
        return ResponseEntity.ok(roomTypes);
    }

    // Menu related APIs
    @GetMapping("/branches/{branchId}/menu/categories")
    public ResponseEntity<List<MenuCategoryResponse>> getMenuCategories(@PathVariable Long branchId) {
        List<MenuCategoryResponse> categories = menuService.getCategoriesByBranch(branchId);
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/menu-categories/{categoryId}/items")
    public ResponseEntity<List<MenuItemResponse>> getMenuItemsByCategory(@PathVariable Long categoryId) {
        List<MenuItemResponse> items = menuService.getMenuItemsByCategory(categoryId);
        return ResponseEntity.ok(items);
    }



    // Promotion related APIs
    @GetMapping("/branches/{branchId}/promotions/active")
    public ResponseEntity<List<PromotionResponse>> getActivePromotions(@PathVariable Long branchId) {
        List<PromotionResponse> promotions = promotionService.getActivePromotionsByBranch(branchId);
        return ResponseEntity.ok(promotions);
    }




//    @PostMapping("/promotions/{promotionId}/calculate")
//    public ResponseEntity<PriceCalculationResponse> calculatePromotionDiscount(@PathVariable Long promotionId,
//                                                                               @RequestBody PriceCalculationRequest request) {
//        PriceCalculationResponse response = promotionService.calculateDiscount(promotionId, request);
//        return ResponseEntity.ok(response);
//    }
}
