package com.example.htql_nhahang_khachsan.service;

import com.example.htql_nhahang_khachsan.entity.RoomBookingEntity;
import com.example.htql_nhahang_khachsan.enums.PaymentMethod;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.name}")
    private String appName;

    @Value("${app.email}")
    private String fromEmail;

    @Value("${app.support.phone}")
    private String supportPhone;

    @Value("${app.support.email}")
    private String supportEmail;

    /**
     * Gửi email xác nhận đặt phòng
     */
    public void sendBookingConfirmation(RoomBookingEntity booking) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, appName);
            helper.setTo(booking.getGuestEmail());
            helper.setSubject("Xác nhận đặt phòng #" + booking.getBookingCode());

            String htmlContent = buildBookingConfirmationEmail(booking);
            helper.setText(htmlContent, true);

            mailSender.send(message);

        } catch (Exception e) {
            throw new RuntimeException("Failed to send booking confirmation email", e);
        }
    }

    /**
     * Xây dựng nội dung email HTML
     */
    private String buildBookingConfirmationEmail(RoomBookingEntity booking) {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>");
        html.append("<html lang='vi'>");
        html.append("<head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        html.append("<style>");
        html.append("body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; }");
        html.append(".header { background: linear-gradient(135deg, #c9a96e, #d4b079); color: white; padding: 30px; text-align: center; }");
        html.append(".content { padding: 30px; background: #f8f9fa; }");
        html.append(".booking-card { background: white; border-radius: 10px; padding: 20px; margin-bottom: 20px; box-shadow: 0 2px 5px rgba(0,0,0,0.1); }");
        html.append(".info-row { display: flex; justify-content: space-between; padding: 10px 0; border-bottom: 1px solid #eee; }");
        html.append(".info-row:last-child { border-bottom: none; }");
        html.append(".highlight { background: #fff3cd; padding: 15px; border-radius: 5px; margin: 20px 0; }");
        html.append(".total { font-size: 1.3em; font-weight: bold; color: #c9a96e; text-align: right; }");
        html.append(".footer { background: #2c3e50; color: white; padding: 20px; text-align: center; }");
        html.append(".button { display: inline-block; padding: 12px 30px; background: #c9a96e; color: white; text-decoration: none; border-radius: 5px; margin: 10px 0; }");
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");

        // Header
        html.append("<div class='header'>");
        html.append("<h1>").append(appName).append("</h1>");
        html.append("<h2>Xác nhận đặt phòng</h2>");
        html.append("</div>");

        // Content
        html.append("<div class='content'>");

        // Greeting
        html.append("<p>Kính gửi <strong>").append(booking.getGuestName()).append("</strong>,</p>");
        html.append("<p>Cảm ơn bạn đã chọn ").append(appName).append(". Đặt phòng của bạn đã được xác nhận thành công!</p>");

        // Booking Code
        html.append("<div class='highlight'>");
        html.append("<h3 style='margin: 0; color: #c9a96e;'>Mã đặt phòng: ").append(booking.getBookingCode()).append("</h3>");
        html.append("<p style='margin: 5px 0 0 0;'>Vui lòng mang theo mã này khi check-in</p>");
        html.append("</div>");

        // Booking Details
        html.append("<div class='booking-card'>");
        html.append("<h3>Thông tin đặt phòng</h3>");

        html.append("<div class='info-row'>");
        html.append("<span><strong>Loại phòng:</strong></span>");
        html.append("<span>").append(booking.getRoomType().getName()).append("</span>");
        html.append("</div>");

        html.append("<div class='info-row'>");
        html.append("<span><strong>Chi nhánh:</strong></span>");
        html.append("<span>").append(booking.getBranch().getName()).append("</span>");
        html.append("</div>");

        html.append("<div class='info-row'>");
        html.append("<span><strong>Nhận phòng:</strong></span>");
        html.append("<span>").append(formatDate(booking.getCheckInDate())).append(" (15:00)</span>");
        html.append("</div>");

        html.append("<div class='info-row'>");
        html.append("<span><strong>Trả phòng:</strong></span>");
        html.append("<span>").append(formatDate(booking.getCheckOutDate())).append(" (12:00)</span>");
        html.append("</div>");

        html.append("<div class='info-row'>");
        html.append("<span><strong>Số đêm:</strong></span>");
        html.append("<span>").append(booking.getNumberOfNights()).append(" đêm</span>");
        html.append("</div>");

        html.append("<div class='info-row'>");
        html.append("<span><strong>Số phòng:</strong></span>");
        html.append("<span>").append(booking.getNumberOfRooms()).append(" phòng</span>");
        html.append("</div>");

        html.append("<div class='info-row'>");
        html.append("<span><strong>Khách:</strong></span>");
        html.append("<span>").append(booking.getAdults()).append(" người lớn");
        if (booking.getChildren() > 0) {
            html.append(", ").append(booking.getChildren()).append(" trẻ em");
        }
        html.append("</span>");
        html.append("</div>");

        html.append("</div>");

        // Services
        if (Boolean.TRUE.equals(booking.getIncludeBreakfast()) ||
                Boolean.TRUE.equals(booking.getIncludeSpa()) ||
                Boolean.TRUE.equals(booking.getIncludeAirportTransfer())) {

            html.append("<div class='booking-card'>");
            html.append("<h3>Dịch vụ bổ sung</h3>");

            if (Boolean.TRUE.equals(booking.getIncludeBreakfast())) {
                html.append("<div class='info-row'>");
                html.append("<span>✓ Buffet sáng</span>");
                html.append("<span>").append(formatCurrency(booking.getBreakfastFee())).append("</span>");
                html.append("</div>");
            }

            if (Boolean.TRUE.equals(booking.getIncludeSpa())) {
                html.append("<div class='info-row'>");
                html.append("<span>✓ Spa package</span>");
                html.append("<span>").append(formatCurrency(booking.getSpaFee())).append("</span>");
                html.append("</div>");
            }

            if (Boolean.TRUE.equals(booking.getIncludeAirportTransfer())) {
                html.append("<div class='info-row'>");
                html.append("<span>✓ Đưa đón sân bay</span>");
                html.append("<span>").append(formatCurrency(booking.getAirportTransferFee())).append("</span>");
                html.append("</div>");
            }

            html.append("</div>");
        }

        // Payment Info
        html.append("<div class='booking-card'>");
        html.append("<h3>Thông tin thanh toán</h3>");

        html.append("<div class='info-row'>");
        html.append("<span>Tổng tiền:</span>");
        html.append("<span>").append(formatCurrency(booking.getTotalAmount())).append("</span>");
        html.append("</div>");

        html.append("<div class='info-row'>");
        html.append("<span>Đã thanh toán:</span>");
        html.append("<span style='color: #28a745; font-weight: bold;'>").append(formatCurrency(booking.getDepositAmount())).append("</span>");
        html.append("</div>");

        if (booking.getRemainingAmount().compareTo(BigDecimal.ZERO) > 0) {
            html.append("<div class='info-row'>");
            html.append("<span>Còn lại (thanh toán khi check-in):</span>");
            html.append("<span style='color: #ffc107; font-weight: bold;'>").append(formatCurrency(booking.getRemainingAmount())).append("</span>");
            html.append("</div>");
        }

        html.append("<div class='info-row'>");
        html.append("<span>Phương thức thanh toán:</span>");
        html.append("<span>").append(getPaymentMethodName(booking.getPaymentMethod())).append("</span>");
        html.append("</div>");

        html.append("</div>");

        // Special Requests
        if (booking.getSpecialRequests() != null && !booking.getSpecialRequests().isEmpty()) {
            html.append("<div class='booking-card'>");
            html.append("<h3>Yêu cầu đặc biệt</h3>");
            html.append("<p>").append(booking.getSpecialRequests()).append("</p>");
            html.append("</div>");
        }

        // Important Info
        html.append("<div class='highlight'>");
        html.append("<h3>Lưu ý quan trọng:</h3>");
        html.append("<ul>");
        html.append("<li>Vui lòng mang theo CMND/CCCD và mã đặt phòng khi check-in</li>");
        html.append("<li>Giờ nhận phòng: 15:00 | Giờ trả phòng: 12:00</li>");
        html.append("<li><strong>Chính sách hủy:</strong> Miễn phí hủy trước 24 giờ. Hủy trong vòng 24 giờ: tính phí 50%. No-show: tính 100%</li>");
        html.append("</ul>");
        html.append("</div>");

        // Contact Info
        html.append("<div class='booking-card'>");
        html.append("<h3>Cần hỗ trợ?</h3>");
        html.append("<p>Liên hệ với chúng tôi:</p>");
        html.append("<p>📞 Hotline: ").append(supportPhone).append("</p>");
        html.append("<p>✉️ Email: ").append(supportEmail).append("</p>");
        html.append("<p>📍 Địa chỉ: ").append(booking.getBranch().getAddress()).append("</p>");
        html.append("</div>");

        html.append("<p>Chúng tôi rất mong được phục vụ bạn!</p>");
        html.append("<p>Trân trọng,<br><strong>").append(appName).append("</strong></p>");

        html.append("</div>");

        // Footer
        html.append("<div class='footer'>");
        html.append("<p>© 2024 ").append(appName).append(". All rights reserved.</p>");
        html.append("<p>Email này được gửi tự động, vui lòng không trả lời trực tiếp.</p>");
        html.append("</div>");

        html.append("</body>");
        html.append("</html>");

        return html.toString();
    }

    // Helper methods
    private String formatDate(LocalDate date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return date.format(formatter);
    }

    private String formatCurrency(BigDecimal amount) {
        return String.format("%,.0fđ", amount.doubleValue());
    }

    private String getPaymentMethodName(PaymentMethod method) {
        switch (method) {
            case VNPAY: return "VNPay";
            case BANK_TRANSFER: return "Chuyển khoản ngân hàng";
            case CREDIT_CARD: return "Thẻ tín dụng";
            case CASH: return "Tiền mặt";
            case MOMO: return "MoMo";
            case ZALOPAY: return "ZaloPay";
            default: return method.name();
        }
    }

    /**
     * Gửi email nhắc nhở trước check-in
     */
    public void sendCheckInReminder(RoomBookingEntity booking) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, appName);
            helper.setTo(booking.getGuestEmail());
            helper.setSubject("Nhắc nhở check-in - Đặt phòng #" + booking.getBookingCode());

            String htmlContent = buildCheckInReminderEmail(booking);
            helper.setText(htmlContent, true);

            mailSender.send(message);

        } catch (Exception e) {
            throw new RuntimeException("Failed to send check-in reminder email", e);
        }
    }

    private String buildCheckInReminderEmail(RoomBookingEntity booking) {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>");
        html.append("<html lang='vi'>");
        html.append("<head><meta charset='UTF-8'></head>");
        html.append("<body style='font-family: Arial, sans-serif;'>");

        html.append("<h2>Nhắc nhở check-in</h2>");
        html.append("<p>Kính gửi ").append(booking.getGuestName()).append(",</p>");
        html.append("<p>Chúng tôi rất mong chờ được đón tiếp bạn!</p>");
        html.append("<p><strong>Thời gian check-in:</strong> ").append(formatDate(booking.getCheckInDate())).append(" từ 15:00</p>");
        html.append("<p><strong>Mã đặt phòng:</strong> ").append(booking.getBookingCode()).append("</p>");

        if (booking.getRemainingAmount().compareTo(BigDecimal.ZERO) > 0) {
            html.append("<p><strong>Số tiền cần thanh toán khi check-in:</strong> ");
            html.append(formatCurrency(booking.getRemainingAmount())).append("</p>");
        }

        html.append("<p>Vui lòng mang theo CMND/CCCD khi check-in.</p>");
        html.append("<p>Liên hệ: ").append(supportPhone).append("</p>");
        html.append("<p>Trân trọng,<br>").append(appName).append("</p>");

        html.append("</body></html>");

        return html.toString();
    }

    /**
     * Gửi email xác nhận hủy đặt phòng
     */
    public void sendCancellationConfirmation(RoomBookingEntity booking) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, appName);
            helper.setTo(booking.getGuestEmail());
            helper.setSubject("Xác nhận hủy đặt phòng #" + booking.getBookingCode());

            String htmlContent = buildCancellationEmail(booking);
            helper.setText(htmlContent, true);

            mailSender.send(message);

        } catch (Exception e) {
            throw new RuntimeException("Failed to send cancellation email", e);
        }
    }

    private String buildCancellationEmail(RoomBookingEntity booking) {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>");
        html.append("<html lang='vi'>");
        html.append("<head><meta charset='UTF-8'></head>");
        html.append("<body style='font-family: Arial, sans-serif;'>");

        html.append("<h2>Xác nhận hủy đặt phòng</h2>");
        html.append("<p>Kính gửi ").append(booking.getGuestName()).append(",</p>");
        html.append("<p>Đặt phòng <strong>").append(booking.getBookingCode()).append("</strong> đã được hủy thành công.</p>");

        html.append("<p><strong>Chi tiết:</strong></p>");
        html.append("<ul>");
        html.append("<li>Loại phòng: ").append(booking.getRoomType().getName()).append("</li>");
        html.append("<li>Ngày: ").append(formatDate(booking.getCheckInDate()))
                .append(" - ").append(formatDate(booking.getCheckOutDate())).append("</li>");
        html.append("</ul>");

        if (booking.getCancellationReason() != null) {
            html.append("<p><strong>Lý do hủy:</strong> ").append(booking.getCancellationReason()).append("</p>");
        }

        html.append("<p>Số tiền hoàn lại (nếu có) sẽ được xử lý trong vòng 5-7 ngày làm việc.</p>");
        html.append("<p>Rất tiếc vì sự bất tiện này. Hy vọng sẽ được phục vụ bạn trong tương lai!</p>");
        html.append("<p>Trân trọng,<br>").append(appName).append("</p>");

        html.append("</body></html>");

        return html.toString();
    }

    /**
     * Gửi email cảm ơn sau khi check-out
     */
    public void sendThankYouEmail(RoomBookingEntity booking) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, appName);
            helper.setTo(booking.getGuestEmail());
            helper.setSubject("Cảm ơn bạn đã lưu trú tại " + appName);

            String htmlContent = buildThankYouEmail(booking);
            helper.setText(htmlContent, true);

            mailSender.send(message);

        } catch (Exception e) {
            throw new RuntimeException("Failed to send thank you email", e);
        }
    }

    private String buildThankYouEmail(RoomBookingEntity booking) {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>");
        html.append("<html lang='vi'>");
        html.append("<head><meta charset='UTF-8'></head>");
        html.append("<body style='font-family: Arial, sans-serif; line-height: 1.6;'>");

        html.append("<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>");

        html.append("<h2 style='color: #c9a96e;'>Cảm ơn bạn đã lựa chọn ").append(appName).append("!</h2>");
        html.append("<p>Kính gửi <strong>").append(booking.getGuestName()).append("</strong>,</p>");
        html.append("<p>Chúng tôi rất vui khi được phục vụ bạn trong chuyến lưu trú vừa qua.</p>");

        html.append("<p>Hy vọng bạn đã có những trải nghiệm tuyệt vời tại <strong>")
                .append(booking.getBranch().getName()).append("</strong>.</p>");

        html.append("<div style='background: #f8f9fa; padding: 20px; border-radius: 10px; margin: 20px 0;'>");
        html.append("<h3>Đánh giá trải nghiệm của bạn</h3>");
        html.append("<p>Ý kiến của bạn rất quan trọng với chúng tôi. Vui lòng dành vài phút để đánh giá dịch vụ.</p>");
        html.append("<a href='#' style='display: inline-block; padding: 12px 30px; background: #c9a96e; color: white; text-decoration: none; border-radius: 5px;'>Đánh giá ngay</a>");
        html.append("</div>");

        html.append("<p>Giảm giá <strong>10%</strong> cho lần đặt phòng tiếp theo của bạn với mã: <strong>THANKS10</strong></p>");

        html.append("<p>Chúng tôi rất mong được đón tiếp bạn trở lại!</p>");
        html.append("<p>Trân trọng,<br><strong>").append(appName).append("</strong></p>");

        html.append("<hr style='border: none; border-top: 1px solid #eee; margin: 30px 0;'>");
        html.append("<p style='color: #999; font-size: 12px;'>Hotline: ").append(supportPhone).append("</p>");

        html.append("</div>");
        html.append("</body></html>");

        return html.toString();
    }
}