package com.example.htql_nhahang_khachsan.service;

import com.example.htql_nhahang_khachsan.entity.TableBookingEntity;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TableBookingEmailService {

    private final JavaMailSender mailSender;

    @Value("${app.name}")
    private String appName;

    @Value("${app.email}")
    private String fromEmail;

    @Value("${app.support.phone}")
    private String supportPhone;

    @Value("${app.support.email}")
    private String supportEmail;

    public void sendBookingConfirmation(TableBookingEntity booking) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, appName);
            helper.setTo(booking.getCustomerEmail());
            helper.setSubject("Xác nhận đặt bàn #" + booking.getBookingCode());

            String htmlContent = buildBookingConfirmationEmail(booking);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Không thể gửi email xác nhận", e);
        }
    }

    public void sendBookingConfirmed(TableBookingEntity booking) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, appName);
            helper.setTo(booking.getCustomerEmail());
            helper.setSubject("Đặt bàn #" + booking.getBookingCode() + " đã được xác nhận");

            String htmlContent = buildBookingConfirmedEmail(booking);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Không thể gửi email xác nhận duyệt", e);
        }
    }

    public void sendBookingCancelled(TableBookingEntity booking, String reason) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, appName);
            helper.setTo(booking.getCustomerEmail());
            helper.setSubject("Đặt bàn #" + booking.getBookingCode() + " đã bị hủy");

            String htmlContent = buildBookingCancelledEmail(booking, reason);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Không thể gửi email hủy đặt bàn", e);
        }
    }

    private String buildBookingConfirmationEmail(TableBookingEntity booking) {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>");
        html.append("<html lang='vi'>");
        html.append("<head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<style>");
        html.append("body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; }");
        html.append(".header { background: linear-gradient(135deg, #c9a96e, #d4b079); color: white; padding: 30px; text-align: center; }");
        html.append(".content { padding: 30px; background: #f8f9fa; }");
        html.append(".booking-card { background: white; border-radius: 10px; padding: 20px; margin-bottom: 20px; box-shadow: 0 2px 5px rgba(0,0,0,0.1); }");
        html.append(".info-row { display: flex; justify-content: space-between; padding: 10px 0; border-bottom: 1px solid #eee; }");
        html.append(".info-row:last-child { border-bottom: none; }");
        html.append(".highlight { background: #fff3cd; padding: 15px; border-radius: 5px; margin: 20px 0; }");
        html.append(".footer { background: #2c3e50; color: white; padding: 20px; text-align: center; }");
        html.append(".badge { display: inline-block; padding: 5px 10px; background: #ffc107; color: #000; border-radius: 5px; font-weight: bold; }");
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");

        // Header
        html.append("<div class='header'>");
        html.append("<h1>").append(appName).append("</h1>");
        html.append("<h2>Xác nhận đặt bàn</h2>");
        html.append("</div>");

        // Content
        html.append("<div class='content'>");

        html.append("<p>Kính gửi <strong>").append(booking.getCustomerName()).append("</strong>,</p>");
        html.append("<p>Cảm ơn bạn đã chọn ").append(appName).append(". Chúng tôi đã nhận được yêu cầu đặt bàn của bạn!</p>");

        // Booking Code
        html.append("<div class='highlight'>");
        html.append("<h3 style='margin: 0; color: #c9a96e;'>Mã đặt bàn: ").append(booking.getBookingCode()).append("</h3>");
        html.append("<p style='margin: 5px 0 0 0;'><span class='badge'>CHỜ XÁC NHẬN</span></p>");
        html.append("<p style='margin: 5px 0 0 0;'>Chúng tôi sẽ xác nhận đặt bàn của bạn trong thời gian sớm nhất</p>");
        html.append("</div>");

        // Booking Details
        html.append("<div class='booking-card'>");
        html.append("<h3>Thông tin đặt bàn</h3>");

        html.append("<div class='info-row'>");
        html.append("<span><strong>Chi nhánh:</strong></span>");
        html.append("<span>").append(booking.getBranch().getName()).append("</span>");
        html.append("</div>");

        html.append("<div class='info-row'>");
        html.append("<span><strong>Ngày:</strong></span>");
        html.append("<span>").append(formatDate(booking.getBookingDate())).append("</span>");
        html.append("</div>");

        html.append("<div class='info-row'>");
        html.append("<span><strong>Giờ:</strong></span>");
        html.append("<span>").append(formatTime(booking.getBookingTime())).append("</span>");
        html.append("</div>");

        html.append("<div class='info-row'>");
        html.append("<span><strong>Số lượng khách:</strong></span>");
        html.append("<span>").append(booking.getPartySize()).append(" người</span>");
        html.append("</div>");

        if (!booking.getTables().isEmpty()) {
            html.append("<div class='info-row'>");
            html.append("<span><strong>Bàn:</strong></span>");
            html.append("<span>").append(booking.getTables().stream()
                            .map(t -> "Bàn " + t.getTableNumber())
                            .collect(Collectors.joining(", ")))
                    .append("</span>");
            html.append("</div>");
        }

        html.append("<div class='info-row'>");
        html.append("<span><strong>Liên hệ:</strong></span>");
        html.append("<span>").append(booking.getContactPhone()).append("</span>");
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
        html.append("<li>Vui lòng đến đúng giờ đã đặt</li>");
        html.append("<li>Nếu muốn hủy, vui lòng thông báo trước ít nhất 2 giờ</li>");
        html.append("<li>Bàn chỉ được giữ trong 15 phút kể từ giờ đặt</li>");
        html.append("</ul>");
        html.append("</div>");

        // Contact Info
        html.append("<div class='booking-card'>");
        html.append("<h3>Cần hỗ trợ?</h3>");
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
        html.append("</div>");

        html.append("</body>");
        html.append("</html>");

        return html.toString();
    }

    private String buildBookingConfirmedEmail(TableBookingEntity booking) {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>");
        html.append("<html lang='vi'>");
        html.append("<head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<style>");
        html.append("body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; }");
        html.append(".header { background: linear-gradient(135deg, #28a745, #20c997); color: white; padding: 30px; text-align: center; }");
        html.append(".content { padding: 30px; background: #f8f9fa; }");
        html.append(".booking-card { background: white; border-radius: 10px; padding: 20px; margin-bottom: 20px; box-shadow: 0 2px 5px rgba(0,0,0,0.1); }");
        html.append(".info-row { display: flex; justify-content: space-between; padding: 10px 0; border-bottom: 1px solid #eee; }");
        html.append(".success { background: #d4edda; padding: 15px; border-radius: 5px; margin: 20px 0; border-left: 4px solid #28a745; }");
        html.append(".footer { background: #2c3e50; color: white; padding: 20px; text-align: center; }");
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");

        html.append("<div class='header'>");
        html.append("<h1>✓ Đặt bàn đã được xác nhận!</h1>");
        html.append("</div>");

        html.append("<div class='content'>");

        html.append("<p>Kính gửi <strong>").append(booking.getCustomerName()).append("</strong>,</p>");

        html.append("<div class='success'>");
        html.append("<h3 style='margin: 0; color: #28a745;'>Đặt bàn #").append(booking.getBookingCode()).append(" đã được xác nhận!</h3>");
        html.append("<p style='margin: 5px 0 0 0;'>Chúng tôi rất mong chờ được đón tiếp bạn.</p>");
        html.append("</div>");

        html.append("<div class='booking-card'>");
        html.append("<h3>Thông tin đặt bàn</h3>");

        html.append("<div class='info-row'>");
        html.append("<span><strong>Chi nhánh:</strong></span>");
        html.append("<span>").append(booking.getBranch().getName()).append("</span>");
        html.append("</div>");

        html.append("<div class='info-row'>");
        html.append("<span><strong>Ngày:</strong></span>");
        html.append("<span>").append(formatDate(booking.getBookingDate())).append("</span>");
        html.append("</div>");

        html.append("<div class='info-row'>");
        html.append("<span><strong>Giờ:</strong></span>");
        html.append("<span>").append(formatTime(booking.getBookingTime())).append("</span>");
        html.append("</div>");

        html.append("<div class='info-row'>");
        html.append("<span><strong>Số khách:</strong></span>");
        html.append("<span>").append(booking.getPartySize()).append(" người</span>");
        html.append("</div>");

        if (!booking.getTables().isEmpty()) {
            html.append("<div class='info-row'>");
            html.append("<span><strong>Bàn đã được sắp xếp:</strong></span>");
            html.append("<span>").append(booking.getTables().stream()
                            .map(t -> "Bàn " + t.getTableNumber())
                            .collect(Collectors.joining(", ")))
                    .append("</span>");
            html.append("</div>");
        }

        html.append("</div>");

        html.append("<div class='booking-card'>");
        html.append("<h3>Lưu ý:</h3>");
        html.append("<ul>");
        html.append("<li>Vui lòng đến đúng giờ</li>");
        html.append("<li>Mang theo mã đặt bàn: <strong>").append(booking.getBookingCode()).append("</strong></li>");
        html.append("<li>Bàn chỉ được giữ 15 phút</li>");
        html.append("</ul>");
        html.append("</div>");

        html.append("<div class='booking-card'>");
        html.append("<h3>Liên hệ</h3>");
        html.append("<p>📞 ").append(supportPhone).append("</p>");
        html.append("<p>📍 ").append(booking.getBranch().getAddress()).append("</p>");
        html.append("</div>");

        html.append("<p>Trân trọng,<br><strong>").append(appName).append("</strong></p>");

        html.append("</div>");

        html.append("<div class='footer'>");
        html.append("<p>© 2024 ").append(appName).append("</p>");
        html.append("</div>");

        html.append("</body>");
        html.append("</html>");

        return html.toString();
    }

    private String buildBookingCancelledEmail(TableBookingEntity booking, String reason) {
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>");
        html.append("<html lang='vi'>");
        html.append("<head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<style>");
        html.append("body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; }");
        html.append(".header { background: linear-gradient(135deg, #dc3545, #c82333); color: white; padding: 30px; text-align: center; }");
        html.append(".content { padding: 30px; background: #f8f9fa; }");
        html.append(".booking-card { background: white; border-radius: 10px; padding: 20px; margin-bottom: 20px; box-shadow: 0 2px 5px rgba(0,0,0,0.1); }");
        html.append(".warning { background: #f8d7da; padding: 15px; border-radius: 5px; margin: 20px 0; border-left: 4px solid #dc3545; }");
        html.append(".footer { background: #2c3e50; color: white; padding: 20px; text-align: center; }");
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");

        html.append("<div class='header'>");
        html.append("<h1>Đặt bàn đã bị hủy</h1>");
        html.append("</div>");

        html.append("<div class='content'>");

        html.append("<p>Kính gửi <strong>").append(booking.getCustomerName()).append("</strong>,</p>");

        html.append("<div class='warning'>");
        html.append("<h3 style='margin: 0; color: #dc3545;'>Đặt bàn #").append(booking.getBookingCode()).append(" đã bị hủy</h3>");
        html.append("</div>");

        html.append("<div class='booking-card'>");
        html.append("<h3>Thông tin đặt bàn</h3>");
        html.append("<p><strong>Ngày:</strong> ").append(formatDate(booking.getBookingDate())).append("</p>");
        html.append("<p><strong>Giờ:</strong> ").append(formatTime(booking.getBookingTime())).append("</p>");
        html.append("<p><strong>Số khách:</strong> ").append(booking.getPartySize()).append(" người</p>");
        html.append("</div>");

        if (reason != null && !reason.isEmpty()) {
            html.append("<div class='booking-card'>");
            html.append("<h3>Lý do hủy</h3>");
            html.append("<p>").append(reason).append("</p>");
            html.append("</div>");
        }

        html.append("<div class='booking-card'>");
        html.append("<p>Rất tiếc vì sự bất tiện này. Hy vọng được phục vụ bạn trong tương lai!</p>");
        html.append("<p>Nếu có thắc mắc, vui lòng liên hệ: ").append(supportPhone).append("</p>");
        html.append("</div>");

        html.append("<p>Trân trọng,<br><strong>").append(appName).append("</strong></p>");

        html.append("</div>");

        html.append("<div class='footer'>");
        html.append("<p>© 2024 ").append(appName).append("</p>");
        html.append("</div>");

        html.append("</body>");
        html.append("</html>");

        return html.toString();
    }

    private String formatDate(LocalDate date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return date.format(formatter);
    }

    private String formatTime(LocalTime time) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return time.format(formatter);
    }
}