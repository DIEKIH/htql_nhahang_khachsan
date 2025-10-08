package com.example.htql_nhahang_khachsan.controller;

import com.example.htql_nhahang_khachsan.dto.AvailabilityResponse;
import com.example.htql_nhahang_khachsan.dto.BookingConfirmationDTO;
import com.example.htql_nhahang_khachsan.dto.BookingSessionDTO;
import com.example.htql_nhahang_khachsan.dto.RoomTypeResponse;
import com.example.htql_nhahang_khachsan.entity.RoomBookingEntity;
import com.example.htql_nhahang_khachsan.enums.PaymentMethod;
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
import java.util.Map;

@Controller
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class RoomBookingController {

    private final RoomService roomService;
    private final BookingService bookingService;
    private final VnPayService vnPayService;

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

            // ===== FIX: Set RoomType =====
            RoomTypeResponse roomType = roomService.getRoomTypeById(id);
            bookingSession.setRoomType(roomType);

            session.setAttribute("bookingSession", bookingSession);

            return "redirect:/bookings/services/" + bookingSession.getSessionId();

        } catch (Exception e) {
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

    // Xử lý hoàn tất đặt phòng
    @PostMapping("/complete")
    public String completeBooking(@RequestParam String sessionId,
                                  @RequestParam Boolean isDepositOnly,
                                  @RequestParam PaymentMethod paymentMethod,
                                  HttpSession httpSession,
                                  RedirectAttributes redirectAttributes,
                                  HttpServletRequest request) {
        System.out.println(">>> PaymentMethod: " + paymentMethod);
        System.out.println(">>> PaymentMethod received: " + paymentMethod);
        BookingSessionDTO session = (BookingSessionDTO) httpSession.getAttribute("bookingSession");

        if (session == null || !session.getSessionId().equals(sessionId)) {
            return "redirect:/";
        }

        try {
            RoomBookingEntity booking = bookingService.createBooking(session, isDepositOnly, paymentMethod);

            System.out.println(">>> PaymentMethod: " + paymentMethod);

            if (paymentMethod == PaymentMethod.VNPAY) {
                long amount = booking.getTotalAmount().longValue() * 100; // VNPay cần *100
                String orderInfo = "Thanh toan don hang " + booking.getBookingCode();

                String paymentUrl = vnPayService.createPaymentUrl(
                        amount,
                        orderInfo,
                        booking.getBookingCode(),
                        request
                );

                System.out.println(">>> VNPay amount: " + amount);
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



//    @GetMapping("/confirmation/{bookingCode}")
//    public String showConfirmation(@PathVariable String bookingCode, Model model) {
//
//        RoomBookingEntity booking = bookingService.getBookingByCode(bookingCode);
//
//        if (booking == null) {
//            return "redirect:/";
//        }
//
//        BookingConfirmationDTO confirmation = bookingService.buildConfirmationDTO(booking);
//        model.addAttribute("confirmation", confirmation);
//
//        return "customer/booking/success";
//    }
}

//@Controller
//@RequestMapping("/bookings")
//@RequiredArgsConstructor
//public class RoomBookingController {
//
//    private final RoomService roomService;
//    private final BookingService bookingService;
//
//    // API kiểm tra phòng trống
//    @GetMapping("/api/check-availability")
//    @ResponseBody
//    public ResponseEntity<AvailabilityResponse> checkAvailability(
//            @RequestParam Long roomTypeId,
//            @RequestParam String checkInDate,
//            @RequestParam String checkOutDate,
//            @RequestParam Integer numberOfRooms) {
//
//        try {
//            AvailabilityResponse availability = bookingService.checkRoomAvailability(
//                    roomTypeId, checkInDate, checkOutDate, numberOfRooms
//            );
//            return ResponseEntity.ok(availability);
//        } catch (Exception e) {
//            return ResponseEntity.badRequest().body(
//                    AvailabilityResponse.builder()
//                            .available(false)
//                            .message("Có lỗi xảy ra: " + e.getMessage())
//                            .build()
//            );
//        }
//    }
//
//    // Bước 1: Chọn phòng và ngày
////    @PostMapping("/room-type/{id}/select")
////    public String selectRoomType(
////            @PathVariable Long id,
////            @RequestParam String checkInDate,
////            @RequestParam String checkOutDate,
////            @RequestParam Integer numberOfRooms,
////            @RequestParam Integer adults,
////            @RequestParam Integer children,
////            HttpSession session) {
////
////        try {
////            // Validate dates
////            LocalDate checkIn = LocalDate.parse(checkInDate);
////            LocalDate checkOut = LocalDate.parse(checkOutDate);
////
////            if (checkIn.isBefore(LocalDate.now())) {
////                return "redirect:/room-types/" + id + "?error=invalid_checkin";
////            }
////
////            if (checkOut.isBefore(checkIn.plusDays(1))) {
////                return "redirect:/room-types/" + id + "?error=invalid_checkout";
////            }
////
////            // Kiểm tra phòng trống
////            AvailabilityResponse availability = bookingService.checkRoomAvailability(
////                    id, checkInDate, checkOutDate, numberOfRooms
////            );
////
////            if (!availability.isAvailable()) {
////                return "redirect:/room-types/" + id + "?error=not_available";
////            }
////
////            // Tạo booking session
////            BookingSessionDTO bookingSession = bookingService.createBookingSession(
////                    id, checkInDate, checkOutDate, numberOfRooms, adults, children
////            );
////
////            // Lưu vào session
////            session.setAttribute("bookingSession", bookingSession);
////
////            // Redirect to service selection page
////            return "redirect:/bookings/services/" + bookingSession.getSessionId();
////
////        } catch (Exception e) {
////            return "redirect:/room-types/" + id + "?error=booking_failed";
////        }
////    }
//
//    // Bước 1: Chọn phòng và ngày - FIXED VERSION
//    @PostMapping("/room-type/{id}/select")
//    public String selectRoomType(
//            @PathVariable Long id,
//            @RequestParam String checkInDate,
//            @RequestParam String checkOutDate,
//            @RequestParam Integer numberOfRooms,
//            @RequestParam Integer adults,
//            @RequestParam Integer children,
//            HttpSession session) {
//
//        try {
//            // Validate dates
//            LocalDate checkIn = LocalDate.parse(checkInDate);
//            LocalDate checkOut = LocalDate.parse(checkOutDate);
//
//            if (checkIn.isBefore(LocalDate.now())) {
//                return "redirect:/room-types/" + id + "?error=invalid_checkin";
//            }
//
//            if (checkOut.isBefore(checkIn.plusDays(1))) {
//                return "redirect:/room-types/" + id + "?error=invalid_checkout";
//            }
//
//            // Kiểm tra phòng trống
//            AvailabilityResponse availability = bookingService.checkRoomAvailability(
//                    id, checkInDate, checkOutDate, numberOfRooms
//            );
//
//            if (!availability.isAvailable()) {
//                return "redirect:/room-types/" + id + "?error=not_available";
//            }
//
//            // Tạo booking session
//            BookingSessionDTO bookingSession = bookingService.createBookingSession(
//                    id, checkInDate, checkOutDate, numberOfRooms, adults, children
//            );
//
//            // ===== CRITICAL FIX: Set RoomType ngay tại đây =====
//            RoomTypeResponse roomType = roomService.getRoomTypeById(id);
//            bookingSession.setRoomType(roomType);
//
//            // Lưu vào session
//            session.setAttribute("bookingSession", bookingSession);
//
//            // Redirect to service selection page
//            return "redirect:/bookings/services/" + bookingSession.getSessionId();
//
//        } catch (Exception e) {
//            return "redirect:/room-types/" + id + "?error=booking_failed";
//        }
//    }
//
//    // FIXED: Hiển thị trang chọn dịch vụ
//    @GetMapping("/services/{sessionId}")
//    public String showServicesPage(@PathVariable String sessionId,
//                                   HttpSession httpSession,
//                                   Model model) {
//
//        BookingSessionDTO session = (BookingSessionDTO) httpSession.getAttribute("bookingSession");
//
//        if (session == null || !session.getSessionId().equals(sessionId)) {
//            return "redirect:/";
//        }
//
//        // Khởi tạo giá trị mặc định cho services nếu null
//        if (session.getIncludeBreakfast() == null) {
//            session.setIncludeBreakfast(false);
//            session.setBreakfastFee(BigDecimal.ZERO);
//        }
//        if (session.getIncludeSpa() == null) {
//            session.setIncludeSpa(false);
//            session.setSpaFee(BigDecimal.ZERO);
//        }
//        if (session.getIncludeAirportTransfer() == null) {
//            session.setIncludeAirportTransfer(false);
//            session.setAirportTransferFee(BigDecimal.ZERO);
//        }
//
//        // ===== FIX: Kiểm tra và set RoomType nếu chưa có =====
//        if (session.getRoomType() == null) {
//            RoomTypeResponse roomType = roomService.getRoomTypeById(session.getRoomTypeId());
//            session.setRoomType(roomType);
//            // Lưu lại session sau khi update
//            httpSession.setAttribute("bookingSession", session);
//        }
//
//        model.addAttribute("session", session);
//        model.addAttribute("roomType", session.getRoomType()); // Pass roomType riêng cho Thymeleaf
//        return "customer/booking/services";
//    }
//
//
//    // Hiển thị trang chọn dịch vụ
////    @GetMapping("/services/{sessionId}")
////    public String showServicesPage(@PathVariable String sessionId,
////                                   HttpSession httpSession,
////                                   Model model) {
////
////        BookingSessionDTO session = (BookingSessionDTO) httpSession.getAttribute("bookingSession");
////
////        if (session == null || !session.getSessionId().equals(sessionId)) {
////            return "redirect:/";
////        }
////
////        // Lấy thông tin room type để hiển thị
////        RoomTypeResponse roomType = roomService.getRoomTypeById(session.getRoomTypeId());
////        session.setRoomType(roomType);
////
////        model.addAttribute("session", session);
////        model.addAttribute("roomType", roomType);
////        return "customer/booking/services";
////    }
//
////    @GetMapping("/services/{sessionId}")
////    public String showServicesPage(@PathVariable String sessionId,
////                                   HttpSession httpSession,
////                                   Model model) {
////
////        BookingSessionDTO session = (BookingSessionDTO) httpSession.getAttribute("bookingSession");
////
////        if (session == null || !session.getSessionId().equals(sessionId)) {
////            return "redirect:/";
////        }
////
////        try {
////            // Kiểm tra và khởi tạo các trường boolean nếu null
////            if (session.getIncludeBreakfast() == null) session.setIncludeBreakfast(false);
////            if (session.getIncludeSpa() == null) session.setIncludeSpa(false);
////            if (session.getIncludeAirportTransfer() == null) session.setIncludeAirportTransfer(false);
////
////            // Kiểm tra xem roomType đã có trong session chưa
////            if (session.getRoomType() == null) {
////                // Lấy thông tin room type và set vào session
////                RoomTypeResponse roomType = roomService.getRoomTypeById(session.getRoomTypeId());
////                session.setRoomType(roomType);
////
////                // Lưu lại session sau khi update
////                httpSession.setAttribute("bookingSession", session);
////            }
////
////            model.addAttribute("session", session);
////            return "customer/booking/services";
////
////        } catch (Exception e) {
////            System.out.println("Error loading services page for session: " + sessionId);
////            e.printStackTrace();
////            return "redirect:/";
////        }
////    }
//
////    @GetMapping("/services/{sessionId}")
////    public String showServicesPage(@PathVariable String sessionId,
////                                   HttpSession httpSession,
////                                   Model model) {
////
////        BookingSessionDTO session = (BookingSessionDTO) httpSession.getAttribute("bookingSession");
////
////        if (session == null || !session.getSessionId().equals(sessionId)) {
////            return "redirect:/";
////        }
////
////        // CRITICAL: Khởi tạo giá trị mặc định nếu null
////        if (session.getIncludeBreakfast() == null) {
////            session.setIncludeBreakfast(false);
////            session.setBreakfastFee(BigDecimal.ZERO);
////        }
////        if (session.getIncludeSpa() == null) {
////            session.setIncludeSpa(false);
////            session.setSpaFee(BigDecimal.ZERO);
////        }
////        if (session.getIncludeAirportTransfer() == null) {
////            session.setIncludeAirportTransfer(false);
////            session.setAirportTransferFee(BigDecimal.ZERO);
////        }
////
////        // Lấy thông tin room type
////        RoomTypeResponse roomType = roomService.getRoomTypeById(session.getRoomTypeId());
////        session.setRoomType(roomType);
////
////        // Lưu lại session đã được fix
////        httpSession.setAttribute("bookingSession", session);
////
////        model.addAttribute("session", session);
////        model.addAttribute("roomType", roomType);
////        return "customer/booking/services";
////    }
//
//    // API cập nhật giá khi chọn dịch vụ
//    @GetMapping("/calculate-price/{sessionId}")
//    @ResponseBody
//    public ResponseEntity<BookingSessionDTO> calculatePrice(
//            @PathVariable String sessionId,
//            @RequestParam Boolean includeBreakfast,
//            @RequestParam Boolean includeSpa,
//            @RequestParam Boolean includeAirportTransfer,
//            HttpSession httpSession) {
//
//        BookingSessionDTO session = (BookingSessionDTO) httpSession.getAttribute("bookingSession");
//
//        if (session == null || !session.getSessionId().equals(sessionId)) {
//            return ResponseEntity.badRequest().build();
//        }
//
//        // Cập nhật dịch vụ
//        session.setIncludeBreakfast(includeBreakfast);
//        session.setIncludeSpa(includeSpa);
//        session.setIncludeAirportTransfer(includeAirportTransfer);
//
//        // Tính lại giá
//        session = bookingService.recalculatePrice(session);
//
//        // Lưu lại session
//        httpSession.setAttribute("bookingSession", session);
//
//        return ResponseEntity.ok(session);
//    }
//
//    // API cập nhật dịch vụ và chuyển trang
//    @PostMapping("/update-services/{sessionId}")
//    @ResponseBody
//    public ResponseEntity<Map<String, String>> updateServices(
//            @PathVariable String sessionId,
//            @RequestBody Map<String, Boolean> services,
//            HttpSession httpSession) {
//
//        BookingSessionDTO session = (BookingSessionDTO) httpSession.getAttribute("bookingSession");
//
//        if (session == null || !session.getSessionId().equals(sessionId)) {
//            return ResponseEntity.badRequest().build();
//        }
//
//        // Cập nhật dịch vụ
//        session.setIncludeBreakfast(services.get("includeBreakfast"));
//        session.setIncludeSpa(services.get("includeSpa"));
//        session.setIncludeAirportTransfer(services.get("includeAirportTransfer"));
//
//        // Tính lại giá
//        session = bookingService.recalculatePrice(session);
//
//        // Lưu session
//        httpSession.setAttribute("bookingSession", session);
//
//        return ResponseEntity.ok(Map.of("status", "success"));
//    }
//
//    // Hiển thị trang thông tin khách hàng
//    @GetMapping("/guest-info/{sessionId}")
//    public String showGuestInfoPage(@PathVariable String sessionId,
//                                    HttpSession httpSession,
//                                    Model model) {
//
//        BookingSessionDTO session = (BookingSessionDTO) httpSession.getAttribute("bookingSession");
//
//        if (session == null || !session.getSessionId().equals(sessionId)) {
//            return "redirect:/";
//        }
//
//        // Lấy thông tin room type
//        RoomTypeResponse roomType = roomService.getRoomTypeById(session.getRoomTypeId());
//        session.setRoomType(roomType);
//
//        model.addAttribute("session", session);
//        return "customer/booking/guest-info";
//    }
//
//    // Xử lý form thông tin khách hàng
//    @PostMapping("/guest-info")
//    public String submitGuestInfo(
//            @RequestParam String sessionId,
//            @RequestParam String guestName,
//            @RequestParam String guestEmail,
//            @RequestParam String guestPhone,
//            @RequestParam(required = false) String guestIdNumber,
//            @RequestParam(required = false) String specialRequests,
//            HttpSession httpSession) {
//
//        BookingSessionDTO session = (BookingSessionDTO) httpSession.getAttribute("bookingSession");
//
//        if (session == null || !session.getSessionId().equals(sessionId)) {
//            return "redirect:/";
//        }
//
//        // Lưu thông tin khách hàng
//        session.setGuestName(guestName);
//        session.setGuestEmail(guestEmail);
//        session.setGuestPhone(guestPhone);
//        session.setGuestIdNumber(guestIdNumber);
//        session.setSpecialRequests(specialRequests);
//
//        // Lưu session
//        httpSession.setAttribute("bookingSession", session);
//
//        // Chuyển đến trang thanh toán
//        return "redirect:/bookings/payment/" + sessionId;
//    }
//
//    // Hiển thị trang thanh toán
//    @GetMapping("/payment/{sessionId}")
//    public String showPaymentPage(@PathVariable String sessionId,
//                                  HttpSession httpSession,
//                                  Model model) {
//
//        BookingSessionDTO session = (BookingSessionDTO) httpSession.getAttribute("bookingSession");
//
//        if (session == null || !session.getSessionId().equals(sessionId)) {
//            return "redirect:/";
//        }
//
//        // Kiểm tra đã có thông tin khách hàng chưa
//        if (session.getGuestName() == null || session.getGuestEmail() == null) {
//            return "redirect:/bookings/guest-info/" + sessionId;
//        }
//
//        // Lấy thông tin room type
//        RoomTypeResponse roomType = roomService.getRoomTypeById(session.getRoomTypeId());
//        session.setRoomType(roomType);
//
//        model.addAttribute("session", session);
//        return "customer/booking/confirmation";
//    }
//
//    // Xử lý thanh toán
//    @PostMapping("/complete")
//    public String completeBooking(
//            @RequestParam String sessionId,
//            @RequestParam Boolean isDepositOnly,
//            @RequestParam PaymentMethod paymentMethod,
//            HttpSession httpSession,
//            RedirectAttributes redirectAttributes) {
//
//        BookingSessionDTO session = (BookingSessionDTO) httpSession.getAttribute("bookingSession");
//
//        if (session == null || !session.getSessionId().equals(sessionId)) {
//            return "redirect:/";
//        }
//
//        try {
//            // Tạo booking
//            RoomBookingEntity booking = bookingService.createBooking(session, isDepositOnly, paymentMethod);
//
//            // Xóa session
//            httpSession.removeAttribute("bookingSession");
//
//            // Chuyển đến trang thành công
//            return "redirect:/bookings/confirmation/" + booking.getBookingCode();
//
//        } catch (Exception e) {
//            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra khi đặt phòng: " + e.getMessage());
//            return "redirect:/bookings/payment/" + sessionId;
//        }
//    }
//
//    // Hiển thị trang xác nhận
//    @GetMapping("/confirmation/{bookingCode}")
//    public String showConfirmation(@PathVariable String bookingCode, Model model) {
//
//        RoomBookingEntity booking = bookingService.getBookingByCode(bookingCode);
//
//        if (booking == null) {
//            return "redirect:/";
//        }
//
//        BookingConfirmationDTO confirmation = bookingService.buildConfirmationDTO(booking);
//        model.addAttribute("confirmation", confirmation);
//
//        return "customer/booking/success";
//    }
//}