package com.example.htql_nhahang_khachsan.controller;

import com.example.htql_nhahang_khachsan.dto.TableBookingResponse;
import com.example.htql_nhahang_khachsan.enums.BookingStatus;
import com.example.htql_nhahang_khachsan.service.AuthService;
import com.example.htql_nhahang_khachsan.service.TableBookingService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/manager/table-bookings")
@RequiredArgsConstructor
public class ManagerTableBookingController {

    private final AuthService authService;
    private final TableBookingService bookingService;

    @GetMapping
    public String listBookings(Model model, HttpSession session,
                               @RequestParam(required = false) BookingStatus status) {
        if (!authService.isLoggedIn(session) || !authService.isManager(session)) {
            return "redirect:/manager/login";
        }

        Long branchId = authService.getCurrentUserBranchId(session);
        List<TableBookingResponse> bookings = bookingService.getAllBookingsByBranch(branchId);

        model.addAttribute("bookings", bookings);
        model.addAttribute("statuses", BookingStatus.values());
        model.addAttribute("selectedStatus", status);

        // counts
        Map<BookingStatus, Long> bookingCounts = bookings.stream()
                .collect(Collectors.groupingBy(TableBookingResponse::getStatus, Collectors.counting()));

        model.addAttribute("pendingCount", bookingCounts.getOrDefault(BookingStatus.PENDING, 0L));
        model.addAttribute("confirmedCount", bookingCounts.getOrDefault(BookingStatus.CONFIRMED, 0L));
        model.addAttribute("checkedInCount", bookingCounts.getOrDefault(BookingStatus.CHECKED_IN, 0L));
        model.addAttribute("checkedOutCount", bookingCounts.getOrDefault(BookingStatus.CHECKED_OUT, 0L));
        model.addAttribute("cancelledCount", bookingCounts.getOrDefault(BookingStatus.CANCELLED, 0L));
        model.addAttribute("noShowCount", bookingCounts.getOrDefault(BookingStatus.NO_SHOW, 0L));

        return "manager/table_bookings/list";
    }

    @GetMapping("/{id}")
    public String viewBookingDetail(@PathVariable Long id,
                                    HttpSession session,
                                    Model model,
                                    RedirectAttributes redirectAttributes) {
        if (!authService.isLoggedIn(session) || !authService.isManager(session)) {
            return "redirect:/manager/login";
        }

        try {
            Long branchId = authService.getCurrentUserBranchId(session);
            TableBookingResponse booking = bookingService.getBookingById(id, branchId);

            model.addAttribute("booking", booking);
            model.addAttribute("statuses", BookingStatus.values());

                return "manager/table_bookings/detail";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/manager/table-bookings";
        }
    }

    @PostMapping("/{id}/resend-email")
    public String resendEmail(@PathVariable Long id,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        if (!authService.isLoggedIn(session) || !authService.isManager(session)) {
            return "redirect:/manager/login";
        }

        try {
            Long branchId = authService.getCurrentUserBranchId(session);
            bookingService.resendBookingEmail(id, branchId);
            redirectAttributes.addFlashAttribute("successMessage", "Email đã được gửi lại cho khách hàng.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể gửi lại email: " + e.getMessage());
        }

        return "redirect:/manager/table-bookings/" + id;
    }


    @PostMapping("/{id}/update-status")
    public String updateBookingStatus(@PathVariable Long id,
                                      @RequestParam BookingStatus status,
                                      HttpSession session,
                                      RedirectAttributes redirectAttributes) {
        if (!authService.isLoggedIn(session) || !authService.isManager(session)) {
            return "redirect:/manager/login";
        }

        try {
            Long branchId = authService.getCurrentUserBranchId(session);
            bookingService.updateBookingStatus(id, branchId, status);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái thành công");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/manager/table-bookings/" + id;
    }


    @PostMapping("/{id}/confirm")
    public String confirmBooking(@PathVariable Long id,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        if (!authService.isLoggedIn(session) || !authService.isManager(session)) {
            return "redirect:/manager/login";
        }

        try {
            Long branchId = authService.getCurrentUserBranchId(session);
            bookingService.confirmBooking(id, branchId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xác nhận đặt bàn thành công");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/manager/table-bookings/" + id;
    }

    @PostMapping("/{id}/cancel")
    public String cancelBooking(@PathVariable Long id,
                                @RequestParam String reason,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        if (!authService.isLoggedIn(session) || !authService.isManager(session)) {
            return "redirect:/manager/login";
        }

        try {
            Long branchId = authService.getCurrentUserBranchId(session);
            bookingService.cancelBooking(id, branchId, reason);
            redirectAttributes.addFlashAttribute("successMessage", "Đã hủy đặt bàn");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/manager/table-bookings/" + id;
    }
}