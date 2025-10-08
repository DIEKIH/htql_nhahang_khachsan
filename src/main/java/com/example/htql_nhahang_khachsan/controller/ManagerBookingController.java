package com.example.htql_nhahang_khachsan.controller;

import com.example.htql_nhahang_khachsan.dto.BookingDetailDTO;
import com.example.htql_nhahang_khachsan.dto.BookingListDTO;
import com.example.htql_nhahang_khachsan.dto.PaymentDTO;
import com.example.htql_nhahang_khachsan.entity.RoomEntity;
import com.example.htql_nhahang_khachsan.enums.BookingStatus;
import com.example.htql_nhahang_khachsan.enums.PaymentStatus;
import com.example.htql_nhahang_khachsan.enums.RoomStatus;
import com.example.htql_nhahang_khachsan.repository.RoomRepository;
import com.example.htql_nhahang_khachsan.service.AuthService;
import com.example.htql_nhahang_khachsan.service.ManagerBookingService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/manager/bookings")
@RequiredArgsConstructor
public class ManagerBookingController {

    private final AuthService authService;
    private final ManagerBookingService bookingService;
    private final RoomRepository roomRepository;

    @GetMapping
    public String bookingList(Model model, HttpSession session,
                              @RequestParam(required = false) String bookingCode,
                              @RequestParam(required = false) BookingStatus status,
                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        if (!authService.isLoggedIn(session) || !authService.isManager(session)) {
            return "redirect:/manager/login";
        }

        Long branchId = authService.getCurrentUserBranchId(session);
        List<BookingListDTO> bookings = bookingService.searchBookings(branchId, bookingCode, status, fromDate, toDate);

        model.addAttribute("bookings", bookings);
        model.addAttribute("statuses", BookingStatus.values());
        model.addAttribute("searchCode", bookingCode);
        model.addAttribute("searchStatus", status);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);

        return "manager/bookings/list";
    }

    @GetMapping("/{id}")
    public String bookingDetail(@PathVariable Long id, Model model, HttpSession session) {
        if (!authService.isLoggedIn(session) || !authService.isManager(session)) {
            return "redirect:/manager/login";
        }

        Long branchId = authService.getCurrentUserBranchId(session);
        BookingDetailDTO booking = bookingService.getBookingDetail(id, branchId);

        if (booking == null) {
            return "redirect:/manager/bookings?error=not_found";
        }

        // Lấy danh sách phòng trống trong khoảng thời gian của booking
        List<RoomEntity> availableRooms = roomRepository.findAvailableRoomsByTypeAndDateRange(
                booking.getRoomTypeId(),
                booking.getCheckInDate(),
                booking.getCheckOutDate(),
                RoomStatus.AVAILABLE,
                Arrays.asList(BookingStatus.CANCELLED, BookingStatus.NO_SHOW)
        );

        model.addAttribute("booking", booking);
        model.addAttribute("availableRooms", availableRooms);
        model.addAttribute("statuses", BookingStatus.values());

        return "manager/bookings/detail";
    }

    @PostMapping("/{id}/assign-room")
    public String assignRoom(@PathVariable Long id,
                             @RequestParam Long roomId,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        if (!authService.isLoggedIn(session) || !authService.isManager(session)) {
            return "redirect:/manager/login";
        }

        try {
            Long branchId = authService.getCurrentUserBranchId(session);
            bookingService.assignRoom(id, roomId, branchId);
            redirectAttributes.addFlashAttribute("success", "Gán phòng thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/manager/bookings/" + id;
    }

    @PostMapping("/{id}/update-status")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam BookingStatus status,
                               @RequestParam(required = false) String note,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        if (!authService.isLoggedIn(session) || !authService.isManager(session)) {
            return "redirect:/manager/login";
        }

        try {
            Long branchId = authService.getCurrentUserBranchId(session);
            bookingService.updateBookingStatus(id, status, note, branchId);
            redirectAttributes.addFlashAttribute("success", "Cập nhật trạng thái thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/manager/bookings/" + id;
    }

    @GetMapping("/{id}/payments")
    public String paymentHistory(@PathVariable Long id, Model model, HttpSession session) {
        if (!authService.isLoggedIn(session) || !authService.isManager(session)) {
            return "redirect:/manager/login";
        }

        Long branchId = authService.getCurrentUserBranchId(session);
        List<PaymentDTO> payments = bookingService.getPaymentHistory(id, branchId);
        BookingDetailDTO booking = bookingService.getBookingDetail(id, branchId);

        // ✅ Tính tổng tiền đã thanh toán
        BigDecimal totalPaid = payments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.PAID)
                .map(PaymentDTO::getAmount) // 🔹 sửa ở đây
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("totalPaid", totalPaid);
        model.addAttribute("payments", payments);
        model.addAttribute("booking", booking);

        return "manager/bookings/payments";
    }

    @PostMapping("/{id}/confirm-payment")
    public String confirmPayment(@PathVariable Long id,
                                 @RequestParam Long paymentId,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        if (!authService.isLoggedIn(session) || !authService.isManager(session)) {
            return "redirect:/manager/login";
        }

        try {
            Long staffId = authService.getCurrentUserId(session);
            bookingService.confirmPayment(paymentId, staffId);
            redirectAttributes.addFlashAttribute("success", "Xác nhận thanh toán thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/manager/bookings/" + id + "/payments";
    }
}
