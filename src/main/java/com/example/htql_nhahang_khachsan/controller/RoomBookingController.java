package com.example.htql_nhahang_khachsan.controller;

import com.example.htql_nhahang_khachsan.dto.AvailabilityResponse;
import com.example.htql_nhahang_khachsan.dto.BookingConfirmationDTO;
import com.example.htql_nhahang_khachsan.dto.BookingSessionDTO;
import com.example.htql_nhahang_khachsan.dto.RoomTypeResponse;
import com.example.htql_nhahang_khachsan.entity.ChatbotBookingDraftEntity;
import com.example.htql_nhahang_khachsan.entity.RoomBookingEntity;
import com.example.htql_nhahang_khachsan.enums.PaymentMethod;
import com.example.htql_nhahang_khachsan.repository.ChatbotBookingDraftRepository;
import com.example.htql_nhahang_khachsan.service.BookingService;
import com.example.htql_nhahang_khachsan.service.RoomService;
import com.example.htql_nhahang_khachsan.service.VnPayService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Controller
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class RoomBookingController {

    private final RoomService roomService;
    private final BookingService bookingService;
    private final VnPayService vnPayService;
    private final ChatbotBookingDraftRepository chatbotBookingDraftRepository;

    @GetMapping("/api/check-availability")
    @ResponseBody
    public ResponseEntity<AvailabilityResponse> checkAvailability(
            @RequestParam Long roomTypeId,
            @RequestParam String checkInDate,
            @RequestParam String checkOutDate,
            @RequestParam Integer numberOfRooms) {

        try {
            AvailabilityResponse availability = bookingService.checkRoomAvailability(
                    roomTypeId, checkInDate, checkOutDate, numberOfRooms
            );
            return ResponseEntity.ok(availability);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    AvailabilityResponse.builder()
                            .available(false)
                            .message("Có lỗi xảy ra: " + e.getMessage())
                            .build()
            );
        }
    }

    @PostMapping("/room-type/{id}/select")
    public String selectRoomType(
            @PathVariable Long id,
            @RequestParam String checkInDate,
            @RequestParam String checkOutDate,
            @RequestParam Integer numberOfRooms,
            @RequestParam Integer adults,
            @RequestParam Integer children,
            HttpSession session) {

        try {
            LocalDate checkIn = LocalDate.parse(checkInDate);
            LocalDate checkOut = LocalDate.parse(checkOutDate);

            if (checkIn.isBefore(LocalDate.now())) {
                return "redirect:/room-types/" + id + "?error=invalid_checkin";
            }

            if (checkOut.isBefore(checkIn.plusDays(1))) {
                return "redirect:/room-types/" + id + "?error=invalid_checkout";
            }

            AvailabilityResponse availability = bookingService.checkRoomAvailability(
                    id, checkInDate, checkOutDate, numberOfRooms
            );

            if (!availability.isAvailable()) {
                return "redirect:/room-types/" + id + "?error=not_available";
            }

            BookingSessionDTO bookingSession = bookingService.createBookingSession(
                    id, checkInDate, checkOutDate, numberOfRooms, adults, children
            );

            // ✅ THÊM log để kiểm tra session sau khi tạo
            System.out.println("=== BOOKING SESSION CREATED ===");
            System.out.println("Session ID: " + bookingSession.getSessionId());
            System.out.println("Total Amount: " + bookingSession.getTotalAmount());
            System.out.println("Deposit Amount: " + bookingSession.getDepositAmount());
            System.out.println("Remaining Amount: " + bookingSession.getRemainingAmount());

            RoomTypeResponse roomType = roomService.getRoomTypeById(id);
            bookingSession.setRoomType(roomType);

            session.setAttribute("bookingSession", bookingSession);

            return "redirect:/bookings/services/" + bookingSession.getSessionId();

        } catch (Exception e) {
            e.printStackTrace(); // ✅ THÊM để xem lỗi chi tiết
            return "redirect:/room-types/" + id + "?error=booking_failed";
        }
    }

    @GetMapping("/services/{sessionId}")
    public String showServicesPage(@PathVariable String sessionId,
                                   HttpSession httpSession,
                                   Model model) {

        BookingSessionDTO session = (BookingSessionDTO) httpSession.getAttribute("bookingSession");

        if (session == null || !session.getSessionId().equals(sessionId)) {
            return "redirect:/";
        }

        if (session.getIncludeBreakfast() == null) {
            session.setIncludeBreakfast(false);
            session.setBreakfastFee(BigDecimal.ZERO);
        }
        if (session.getIncludeSpa() == null) {
            session.setIncludeSpa(false);
            session.setSpaFee(BigDecimal.ZERO);
        }
        if (session.getIncludeAirportTransfer() == null) {
            session.setIncludeAirportTransfer(false);
            session.setAirportTransferFee(BigDecimal.ZERO);
        }

        // ===== FIX: Đảm bảo RoomType luôn có =====
        if (session.getRoomType() == null) {
            RoomTypeResponse roomType = roomService.getRoomTypeById(session.getRoomTypeId());
            session.setRoomType(roomType);
            httpSession.setAttribute("bookingSession", session);
        }

        model.addAttribute("bookingSession", session);

        model.addAttribute("roomType", session.getRoomType());

        System.out.println("SESSION in showServicesPage: " + session);

        return "customer/booking/services";


    }

    @GetMapping("/calculate-price/{sessionId}")
    @ResponseBody
    public ResponseEntity<BookingSessionDTO> calculatePrice(
            @PathVariable String sessionId,
            @RequestParam Boolean includeBreakfast,
            @RequestParam Boolean includeSpa,
            @RequestParam Boolean includeAirportTransfer,
            HttpSession httpSession) {

        BookingSessionDTO session = (BookingSessionDTO) httpSession.getAttribute("bookingSession");

        if (session == null || !session.getSessionId().equals(sessionId)) {
            return ResponseEntity.badRequest().build();
        }

        session.setIncludeBreakfast(includeBreakfast);
        session.setIncludeSpa(includeSpa);
        session.setIncludeAirportTransfer(includeAirportTransfer);

        session = bookingService.recalculatePrice(session);
        httpSession.setAttribute("bookingSession", session);

        return ResponseEntity.ok(session);
    }

    @PostMapping("/update-services/{sessionId}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> updateServices(
            @PathVariable String sessionId,
            @RequestBody Map<String, Boolean> services,
            HttpSession httpSession) {

        BookingSessionDTO session = (BookingSessionDTO) httpSession.getAttribute("bookingSession");

        if (session == null || !session.getSessionId().equals(sessionId)) {
            return ResponseEntity.badRequest().build();
        }

        session.setIncludeBreakfast(services.get("includeBreakfast"));
        session.setIncludeSpa(services.get("includeSpa"));
        session.setIncludeAirportTransfer(services.get("includeAirportTransfer"));

        session = bookingService.recalculatePrice(session);
        httpSession.setAttribute("bookingSession", session);

        return ResponseEntity.ok(Map.of("status", "success"));
    }

    @GetMapping("/guest-info/{sessionId}")
    public String showGuestInfoPage(@PathVariable String sessionId,
                                    HttpSession httpSession,
                                    Model model) {

        BookingSessionDTO session = (BookingSessionDTO) httpSession.getAttribute("bookingSession");

        if (session == null || !session.getSessionId().equals(sessionId)) {
            return "redirect:/";
        }

        RoomTypeResponse roomType = roomService.getRoomTypeById(session.getRoomTypeId());
        session.setRoomType(roomType);

        model.addAttribute("bookingSession", session);
        return "customer/booking/guest-info";
    }

    @PostMapping("/guest-info")
    public String submitGuestInfo(
            @RequestParam String sessionId,
            @RequestParam String guestName,
            @RequestParam String guestEmail,
            @RequestParam String guestPhone,
            @RequestParam(required = false) String guestIdNumber,
            @RequestParam(required = false) String specialRequests,
            HttpSession httpSession) {

        BookingSessionDTO session = (BookingSessionDTO) httpSession.getAttribute("bookingSession");

        if (session == null || !session.getSessionId().equals(sessionId)) {
            return "redirect:/";
        }

        session.setGuestName(guestName);
        session.setGuestEmail(guestEmail);
        session.setGuestPhone(guestPhone);
        session.setGuestIdNumber(guestIdNumber);
        session.setSpecialRequests(specialRequests);

        httpSession.setAttribute("bookingSession", session);

        return "redirect:/bookings/payment/" + sessionId;
    }


    // Trang xác nhận đặt phòng
    @GetMapping("/payment/{sessionId}")
    public String showPaymentPage(@PathVariable String sessionId,
                                  HttpSession httpSession,
                                  Model model) {
        BookingSessionDTO session = (BookingSessionDTO) httpSession.getAttribute("bookingSession");

        if (session == null || !session.getSessionId().equals(sessionId)) {
            return "redirect:/";
        }

        if (session.getGuestName() == null || session.getGuestEmail() == null) {
            return "redirect:/bookings/guest-info/" + sessionId;
        }

        RoomTypeResponse roomType = roomService.getRoomTypeById(session.getRoomTypeId());
        session.setRoomType(roomType);

        bookingService.recalculatePrice(session);

        model.addAttribute("bookingSession", session);
        return "customer/booking/confirmation";
    }

    @PostMapping("/complete")
    public String completeBooking(@RequestParam String sessionId,
                                  @RequestParam Boolean isDepositOnly,
                                  @RequestParam PaymentMethod paymentMethod,
                                  HttpSession httpSession,
                                  RedirectAttributes redirectAttributes,
                                  HttpServletRequest request) {
        System.out.println(">>> PaymentMethod: " + paymentMethod);
        System.out.println(">>> isDepositOnly = " + isDepositOnly);

        BookingSessionDTO session = (BookingSessionDTO) httpSession.getAttribute("bookingSession");

        if (session == null || !session.getSessionId().equals(sessionId)) {
            return "redirect:/";
        }

        try {
            RoomBookingEntity booking = bookingService.createBooking(session, isDepositOnly, paymentMethod);

            System.out.println(">>> PaymentMethod: " + paymentMethod);

            if (paymentMethod == PaymentMethod.VNPAY) {
                // ✅ SỬA: Tính đúng amount dựa vào isDepositOnly
                BigDecimal paymentAmount = isDepositOnly ? booking.getDepositAmount() : booking.getTotalAmount();
                long amount = paymentAmount.longValue() * 100; // VNPay cần *100

                String orderInfo = "Thanh toan don hang " + booking.getBookingCode();

                String paymentUrl = vnPayService.createPaymentUrl(
                        amount,
                        orderInfo,
                        booking.getBookingCode(),
                        request
                );

                System.out.println(">>> isDepositOnly: " + isDepositOnly);
                System.out.println(">>> Payment amount: " + paymentAmount);
                System.out.println(">>> VNPay amount (*100): " + amount);
                System.out.println(">>> VNPay URL: " + paymentUrl);

                return "redirect:" + paymentUrl;
            }

            httpSession.removeAttribute("bookingSession");
            return "redirect:/bookings/confirmation/" + booking.getBookingCode();

        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra khi đặt phòng: " + e.getMessage());
            return "redirect:/bookings/payment/" + sessionId;
        }
    }

    // VNPay callback
    @GetMapping("/payment/vnpay-return")
    public String vnPayReturn(HttpServletRequest request, Model model) {
        String responseCode = request.getParameter("vnp_ResponseCode");
        String txnRef = request.getParameter("vnp_TxnRef"); // bookingCode

        if ("00".equals(responseCode)) {
            bookingService.updatePaymentStatus(txnRef, true);
            model.addAttribute("message", "Thanh toán thành công!");
        } else {
            bookingService.updatePaymentStatus(txnRef, false);
            model.addAttribute("message", "Thanh toán thất bại!");
        }

        RoomBookingEntity booking = bookingService.getBookingByCode(txnRef);
        model.addAttribute("confirmation", bookingService.buildConfirmationDTO(booking));

        return "customer/booking/success";
    }


    // ✅ THÊM vào RoomBookingController

    @GetMapping("/from-chatbot/{draftCode}")
    public String createBookingFromChatbot(
            @PathVariable String draftCode,
            HttpSession httpSession,
            RedirectAttributes redirectAttributes) {

        try {
            // Tìm draft
            ChatbotBookingDraftEntity draft = chatbotBookingDraftRepository
                    .findByDraftCode(draftCode)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin đặt phòng"));

            // Check expired
            if (draft.getExpiresAt().isBefore(LocalDateTime.now())) {
                redirectAttributes.addFlashAttribute("error",
                        "Phiên đặt phòng đã hết hạn. Vui lòng thử lại.");
                return "redirect:/";
            }

            // Convert draft -> BookingSessionDTO
            BookingSessionDTO session = bookingService.createBookingSessionFromDraft(draft);

            // Lưu vào session
            httpSession.setAttribute("bookingSession", session);

            // Redirect đến trang thanh toán
            return "redirect:/bookings/payment/" + session.getSessionId();

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/";
        }
    }
}