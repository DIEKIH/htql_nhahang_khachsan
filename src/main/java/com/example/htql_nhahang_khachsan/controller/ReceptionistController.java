package com.example.htql_nhahang_khachsan.controller;

import com.example.htql_nhahang_khachsan.dto.*;
import com.example.htql_nhahang_khachsan.entity.InvoiceEntity;
import com.example.htql_nhahang_khachsan.entity.RoomBookingEntity;
import com.example.htql_nhahang_khachsan.entity.RoomEntity;
import com.example.htql_nhahang_khachsan.entity.RoomTypeEntity;
import com.example.htql_nhahang_khachsan.enums.BookingStatus;
import com.example.htql_nhahang_khachsan.enums.PaymentMethod;
import com.example.htql_nhahang_khachsan.enums.PaymentStatus;
import com.example.htql_nhahang_khachsan.enums.RoomStatus;
import com.example.htql_nhahang_khachsan.repository.RoomRepository;
import com.example.htql_nhahang_khachsan.service.AuthService;
import com.example.htql_nhahang_khachsan.service.InvoiceService;
import com.example.htql_nhahang_khachsan.service.ReceptionistBookingService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Controller
@RequestMapping("/cashier-hotel")
@RequiredArgsConstructor
public class ReceptionistController {

    private final AuthService authService;
    private final ReceptionistBookingService bookingService;
    private final RoomRepository roomRepository;
    private final InvoiceService invoiceService;

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        if (!authService.isLoggedIn(session) || !authService.isCashierHotel(session)) {
            return "redirect:/cashier-hotel/login";
        }

        Long branchId = authService.getCurrentUserBranchId(session);
        LocalDate today = LocalDate.now();

        // Get all dashboard data
        DashboardDataDTO dashboardData = bookingService.getDashboardData(branchId, today);

        model.addAttribute("stats", dashboardData.getStats());
        model.addAttribute("todayCheckIns", dashboardData.getTodayCheckIns());
        model.addAttribute("todayCheckOuts", dashboardData.getTodayCheckOuts());
        model.addAttribute("unpaidDepositsCount", dashboardData.getUnpaidDepositsCount());
        model.addAttribute("roomsCleaningCount", dashboardData.getRoomsCleaningCount());
        model.addAttribute("monthlyBookingsCount", dashboardData.getMonthlyBookingsCount());
        model.addAttribute("occupancyRate", String.format("%.0f", dashboardData.getOccupancyRate()));
        model.addAttribute("todayRevenue", dashboardData.getTodayRevenue());

        return "staff/cashier_hotel/dashboard";
    }

    @GetMapping("/api/dashboard-data")
    @ResponseBody
    public ResponseEntity<DashboardDataDTO> getDashboardDataAPI(HttpSession session) {
        if (!authService.isLoggedIn(session) || !authService.isCashierHotel(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Long branchId = authService.getCurrentUserBranchId(session);
        LocalDate today = LocalDate.now();

        DashboardDataDTO data = bookingService.getDashboardData(branchId, today);
        return ResponseEntity.ok(data);
    }

    @GetMapping("/booking-management")
    public String bookingManagement(HttpSession session, Model model,
                                    @RequestParam(required = false) String search,
                                    @RequestParam(required = false) BookingStatus status,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (!authService.isLoggedIn(session) || !authService.isCashierHotel(session)) {
            return "redirect:/cashier-hotel/login";
        }

        Long branchId = authService.getCurrentUserBranchId(session);
        List<ReceptionistBookingDTO> bookings = bookingService.searchBookings(branchId, search, status, date);

        model.addAttribute("bookings", bookings);
        model.addAttribute("BookingStatus", BookingStatus.class);  // ADD THIS LINE
        model.addAttribute("PaymentStatus", PaymentStatus.class);  // ADD THIS LINE
        model.addAttribute("statuses", BookingStatus.values());
        model.addAttribute("searchTerm", search);
        model.addAttribute("searchStatus", status);
        model.addAttribute("searchDate", date);

        return "staff/cashier_hotel/booking_management";
    }

    @GetMapping("/walkin-booking")
    public String walkinBookingForm(HttpSession session, Model model) {
        if (!authService.isLoggedIn(session) || !authService.isCashierHotel(session)) {
            return "redirect:/cashier-hotel/login";
        }

        Long branchId = authService.getCurrentUserBranchId(session);
        List<RoomTypeEntity> roomTypes = bookingService.getAvailableRoomTypes(branchId);

        model.addAttribute("roomTypes", roomTypes);
        model.addAttribute("walkinRequest", new WalkinBookingRequest());

        return "staff/cashier_hotel/walkin_booking";
    }


    @PostMapping("/walkin-booking")
    public String createWalkinBooking(
            @Valid @ModelAttribute("walkinRequest") WalkinBookingRequest request,
            BindingResult result,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {

        Logger logger = LoggerFactory.getLogger(getClass());

        if (!authService.isLoggedIn(session) || !authService.isCashierHotel(session)) {
            return "redirect:/cashier-hotel/login";
        }

        if (result.hasErrors()) {
            Long branchId = authService.getCurrentUserBranchId(session);
            List<RoomTypeEntity> roomTypes = bookingService.getAvailableRoomTypes(branchId);
            model.addAttribute("roomTypes", roomTypes);
            model.addAttribute("walkinRequest", request);
            return "staff/cashier_hotel/walkin_booking";
        }

        try {
            Long branchId = authService.getCurrentUserBranchId(session);
            Long staffId = authService.getCurrentUserId(session);

            if (request.getRoomTypeId() == null) {
                throw new IllegalStateException("Vui lòng chọn loại phòng");
            }

            RoomBookingEntity booking = bookingService.createWalkinBooking(request, branchId, staffId);
            redirectAttributes.addFlashAttribute("success", "Đặt phòng thành công! Mã: " + booking.getBookingCode());
            return "redirect:/cashier-hotel/booking-detail/" + booking.getId();
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/cashier-hotel/walkin-booking";
        } catch (Exception e) {
            logger.error("Error creating walkin booking", e);
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
            return "redirect:/cashier-hotel/walkin-booking";
        }
    }



    @GetMapping("/booking-detail/{id}")
    public String bookingDetail(@PathVariable Long id, HttpSession session, Model model) {
        if (!authService.isLoggedIn(session) || !authService.isCashierHotel(session)) {
            return "redirect:/cashier-hotel/login";
        }

        Long branchId = authService.getCurrentUserBranchId(session);
        ReceptionistBookingDetailDTO booking = bookingService.getBookingDetail(id, branchId);

        if (booking == null) {
            return "redirect:/cashier-hotel/booking-management?error=not_found";
        }

        List<RoomEntity> availableRooms = roomRepository.findAvailableRoomsByTypeAndDateRange(
                booking.getRoomTypeId(),
                booking.getCheckInDate(),
                booking.getCheckOutDate(),
                RoomStatus.AVAILABLE,
                Arrays.asList(BookingStatus.CANCELLED, BookingStatus.NO_SHOW)
        );

        model.addAttribute("booking", booking);
        model.addAttribute("availableRooms", availableRooms);
        model.addAttribute("BookingStatus", BookingStatus.class);      // Thêm dòng này
        model.addAttribute("PaymentStatus", PaymentStatus.class);


        return "staff/cashier_hotel/booking_detail";
    }

    // ✅ THÊM endpoint xác nhận thanh toán phần còn lại
    @PostMapping("/confirm-remaining-payment/{id}")
    public String confirmRemainingPayment(@PathVariable Long id,
                                          HttpSession session,
                                          RedirectAttributes redirectAttributes) {
        if (!authService.isLoggedIn(session) || !authService.isCashierHotel(session)) {
            return "redirect:/cashier-hotel/login";
        }

        try {
            Long staffId = authService.getCurrentUserId(session);
            Long branchId = authService.getCurrentUserBranchId(session);
            bookingService.confirmRemainingPayment(id, staffId, branchId);
            redirectAttributes.addFlashAttribute("success", "Xác nhận thanh toán phần còn lại thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/cashier-hotel/booking-detail/" + id;
    }

    @PostMapping("/check-in/{id}")
    public String checkIn(@PathVariable Long id,
                          @RequestParam(required = false) String idNumber,
                          HttpSession session,
                          RedirectAttributes redirectAttributes) {
        if (!authService.isLoggedIn(session) || !authService.isCashierHotel(session)) {
            return "redirect:/cashier-hotel/login";
        }

        try {
            Long branchId = authService.getCurrentUserBranchId(session);
            Long staffId = authService.getCurrentUserId(session);

            bookingService.checkIn(id, idNumber, branchId, staffId);
            redirectAttributes.addFlashAttribute("success", "Check-in thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/cashier-hotel/booking-detail/" + id;
    }

    @PostMapping("/check-out/{id}")
    public String checkOut(@PathVariable Long id,
                           HttpSession session,
                           RedirectAttributes redirectAttributes) {
        if (!authService.isLoggedIn(session) || !authService.isCashierHotel(session)) {
            return "redirect:/cashier-hotel/login";
        }

        try {
            Long branchId = authService.getCurrentUserBranchId(session);
            Long staffId = authService.getCurrentUserId(session);

            InvoiceEntity invoice = bookingService.checkOut(id, branchId, staffId);
            redirectAttributes.addFlashAttribute("success", "Check-out thành công!");
            return "redirect:/cashier-hotel/invoice/" + invoice.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/cashier-hotel/booking-detail/" + id;
        }
    }

    @PostMapping("/collect-deposit/{id}")
    public String collectDeposit(@PathVariable Long id,
                                 @RequestParam BigDecimal amount,
                                 @RequestParam PaymentMethod method,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        if (!authService.isLoggedIn(session) || !authService.isCashierHotel(session)) {
            return "redirect:/cashier-hotel/login";
        }

        try {
            Long staffId = authService.getCurrentUserId(session);
            bookingService.collectDeposit(id, amount, method, staffId);
            redirectAttributes.addFlashAttribute("success", "Thu cọc thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/cashier-hotel/booking-detail/" + id;
    }

    @PostMapping("/change-room/{id}")
    public String changeRoom(@PathVariable Long id,
                             @RequestParam Long newRoomId,
                             @RequestParam(required = false) String reason,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        if (!authService.isLoggedIn(session) || !authService.isCashierHotel(session)) {
            return "redirect:/cashier-hotel/login";
        }

        try {
            Long branchId = authService.getCurrentUserBranchId(session);
            bookingService.changeRoom(id, newRoomId, reason, branchId);
            redirectAttributes.addFlashAttribute("success", "Đổi phòng thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/cashier-hotel/booking-detail/" + id;
    }

    @PostMapping("/extend-stay/{id}")
    public String extendStay(@PathVariable Long id,
                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate newCheckOut,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        if (!authService.isLoggedIn(session) || !authService.isCashierHotel(session)) {
            return "redirect:/cashier-hotel/login";
        }

        try {
            bookingService.extendStay(id, newCheckOut);
            redirectAttributes.addFlashAttribute("success", "Gia hạn thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/cashier-hotel/booking-detail/" + id;
    }

    @GetMapping("/invoice/{id}")
    public String viewInvoice(@PathVariable Long id, HttpSession session, Model model) {
        if (!authService.isLoggedIn(session) || !authService.isCashierHotel(session)) {
            return "redirect:/cashier-hotel/login";
        }

        InvoiceDTO invoice = invoiceService.getInvoiceById(id);
        model.addAttribute("invoice", invoice);

        return "staff/cashier_hotel/invoice";
    }


    @GetMapping("/room-status")
    public String roomStatus(HttpSession session, Model model) {
        if (!authService.isLoggedIn(session) || !authService.isCashierHotel(session)) {
            return "redirect:/receptionist/login";
        }

        Long branchId = authService.getCurrentUserBranchId(session);
        LocalDate today = LocalDate.now();

        List<RoomStatusDTO> rooms = bookingService.getRoomStatusToday(branchId, today);

        // Calculate stats
        DashboardStatsDTO stats = bookingService.getDashboardStats(branchId, today);

        model.addAttribute("rooms", rooms);
        model.addAttribute("stats", stats);

        return "staff/cashier_hotel/room_status";
    }

    @GetMapping("/api/check-availability")
    @ResponseBody
    public ResponseEntity<AvailabilityResponse> checkAvailability(
            @RequestParam Long roomTypeId,
            @RequestParam String checkIn,
            @RequestParam String checkOut,
            HttpSession session) {

        if (!authService.isLoggedIn(session) || !authService.isCashierHotel(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        AvailabilityResponse response = bookingService.checkAvailability(roomTypeId, checkIn, checkOut);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/room-status")
    @ResponseBody
    public ResponseEntity<List<RoomStatusDTO>> getRoomStatusAPI(HttpSession session) {
        if (!authService.isLoggedIn(session) || !authService.isCashierHotel(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Long branchId = authService.getCurrentUserBranchId(session);
        LocalDate today = LocalDate.now();

        List<RoomStatusDTO> rooms = bookingService.getRoomStatusToday(branchId, today);
        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/api/dashboard-stats")
    @ResponseBody
    public ResponseEntity<DashboardStatsDTO> getDashboardStatsAPI(HttpSession session) {
        if (!authService.isLoggedIn(session) || !authService.isCashierHotel(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Long branchId = authService.getCurrentUserBranchId(session);
        LocalDate today = LocalDate.now();

        DashboardStatsDTO stats = bookingService.getDashboardStats(branchId, today);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/logout")
    public String logout(HttpSession session, RedirectAttributes redirectAttributes) {
        session.invalidate();
        redirectAttributes.addFlashAttribute("success", "Đã đăng xuất thành công");
        return "redirect:/receptionist/login";
    }
}
