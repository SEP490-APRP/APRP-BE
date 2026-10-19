package com.sang.be_capnong.service.impl;

import com.sang.be_capnong.entity.Order;
import com.sang.be_capnong.entity.User;
import com.sang.be_capnong.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class EmailServiceImpl implements EmailService {

    JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    @NonFinal
    String fromEmail;

    @Override
    public void sendOtpEmail(String toEmail, int otp) {
        log.info("Preparing to send verification OTP to: {}", toEmail);

        if (fromEmail == null || fromEmail.isBlank()) {
            log.warn("spring.mail.username is not configured in .env / application.yml.");
            log.info("========== [DEVELOPMENT OTP CODE] Email: {} | OTP: {} ==========", toEmail, otp);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("[APRP] Mã xác thực tài khoản (OTP)");

            String htmlContent = buildOtpHtmlContent(otp);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Verification OTP email sent successfully to: {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send OTP email to {}: {}", toEmail, e.getMessage());
            log.info("========== [FALLBACK OTP CODE] Email: {} | OTP: {} ==========", toEmail, otp);
        }
    }

    @Override
    public void sendForgotPasswordOtpEmail(String toEmail, int otp) {
        log.info("Preparing to send forgot-password OTP to: {}", toEmail);

        if (fromEmail == null || fromEmail.isBlank()) {
            log.warn("spring.mail.username is not configured in .env / application.yml.");
            log.info("========== [DEVELOPMENT FORGOT-PASSWORD OTP CODE] Email: {} | OTP: {} ==========", toEmail, otp);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("[APRP] Yêu cầu đặt lại mật khẩu (OTP)");

            String htmlContent = buildForgotPasswordOtpHtmlContent(otp);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Forgot-password OTP email sent successfully to: {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send forgot-password OTP email to {}: {}", toEmail, e.getMessage());
            log.info("========== [FALLBACK FORGOT-PASSWORD OTP CODE] Email: {} | OTP: {} ==========", toEmail, otp);
        }
    }

    private String buildForgotPasswordOtpHtmlContent(int otp) {
        return """
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 24px; border: 1px solid #e0e0e0; border-radius: 12px; background-color: #ffffff;">
                <div style="text-align: center; margin-bottom: 20px;">
                    <h2 style="color: #2e7d32; margin: 0; font-size: 24px;">NỀN TẢNG GIẢI CỨU NÔNG SẢN VIỆT NAM (APRP)</h2>
                    <p style="color: #666; font-size: 14px; margin-top: 6px;">Yêu Cầu Đặt Lại Mật Khẩu</p>
                </div>
                <hr style="border: none; border-top: 1px solid #eeeeee; margin: 20px 0;" />
                <p style="font-size: 16px; color: #333333;">Xin chào,</p>
                <p style="font-size: 15px; color: #555555; line-height: 1.6;">
                    Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản APRP của bạn. Vui lòng nhập mã OTP xác thực 6 chữ số dưới đây để hoàn tất việc đổi mật khẩu mới:
                </p>
                <div style="text-align: center; margin: 35px 0;">
                    <span style="display: inline-block; font-size: 36px; font-weight: bold; letter-spacing: 8px; color: #d32f2f; background: #ffebee; padding: 14px 28px; border-radius: 10px; border: 2px dashed #d32f2f;">
                        """ + otp + """
                    </span>
                </div>
                <p style="color: #d32f2f; font-size: 14px; font-weight: bold; text-align: center;">
                    ⚠️ Mã OTP có hiệu lực trong vòng 5 phút. Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email hoặc đổi mật khẩu để bảo vệ tài khoản.
                </p>
                <hr style="border: none; border-top: 1px solid #eeeeee; margin: 24px 0;" />
                <p style="font-size: 12px; color: #888888; text-align: center; margin: 0;">
                    Đây là email tự động từ hệ thống APRP. Vui lòng không trả lời email này.
                </p>
            </div>
        """;
    }

    @Override
    public void sendPaymentSuccessInvoiceEmail(User buyer, List<Order> orders, String orderCode, BigDecimal totalAmount) {
        if (buyer == null || buyer.getEmail() == null || buyer.getEmail().isBlank()) {
            log.warn("[InvoiceEmail] Buyer or buyer email is null. Skip sending invoice email for orderCode: {}", orderCode);
            return;
        }

        String toEmail = buyer.getEmail();
        log.info("[InvoiceEmail] Queuing payment invoice email to: {} for orderCode: {}", toEmail, orderCode);

        // Gửi email bất đồng bộ để không chặn luồng response của webhook/client
        CompletableFuture.runAsync(() -> {
            if (fromEmail == null || fromEmail.isBlank()) {
                log.warn("[InvoiceEmail] spring.mail.username is not configured. Logged invoice info for {}: orderCode={}, amount={}", toEmail, orderCode, totalAmount);
                return;
            }

            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                helper.setFrom(fromEmail);
                helper.setTo(toEmail);
                helper.setSubject("[APRP] Xác nhận thanh toán thành công - Mã GD #" + orderCode);

                String htmlContent = buildPaymentInvoiceHtmlContent(buyer, orders, orderCode, totalAmount);
                helper.setText(htmlContent, true);

                mailSender.send(message);
                log.info("[InvoiceEmail] Invoice email sent successfully to {} for orderCode: {}", toEmail, orderCode);

            } catch (Exception e) {
                log.error("[InvoiceEmail] Failed to send payment invoice email to {}: {}", toEmail, e.getMessage(), e);
            }
        });
    }

    private String buildOtpHtmlContent(int otp) {
        return """
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 24px; border: 1px solid #e0e0e0; border-radius: 12px; background-color: #ffffff;">
                <div style="text-align: center; margin-bottom: 20px;">
                    <h2 style="color: #2e7d32; margin: 0; font-size: 24px;">NỀN TẢNG GIẢI CỨU NÔNG SẢN VIỆT NAM (APRP)</h2>
                    <p style="color: #666; font-size: 14px; margin-top: 6px;">Hệ thống Kết nối & Tiêu thụ Nông sản</p>
                </div>
                <hr style="border: none; border-top: 1px solid #eeeeee; margin: 20px 0;" />
                <p style="font-size: 16px; color: #333333;">Xin chào,</p>
                <p style="font-size: 15px; color: #555555; line-height: 1.6;">
                    Cảm ơn bạn đã đăng ký tham gia mạng lưới APRP. Để hoàn tất kích hoạt tài khoản của bạn, vui lòng nhập mã xác thực OTP gồm 6 chữ số dưới đây:
                </p>
                <div style="text-align: center; margin: 35px 0;">
                    <span style="display: inline-block; font-size: 36px; font-weight: bold; letter-spacing: 8px; color: #2e7d32; background: #e8f5e9; padding: 14px 28px; border-radius: 10px; border: 2px dashed #2e7d32;">
                        """ + otp + """
                    </span>
                </div>
                <p style="color: #d32f2f; font-size: 14px; font-weight: bold; text-align: center;">
                    ⚠️ Mã OTP có hiệu lực trong vòng 5 phút. Vui lòng không chia sẻ mã này cho bất kỳ ai.
                </p>
                <hr style="border: none; border-top: 1px solid #eeeeee; margin: 24px 0;" />
                <p style="font-size: 12px; color: #888888; text-align: center; margin: 0;">
                    Đây là email tự động từ hệ thống APRP. Vui lòng không trả lời email này.
                </p>
            </div>
        """;
    }

    private String buildPaymentInvoiceHtmlContent(User buyer, List<Order> orders, String orderCode, BigDecimal totalAmount) {
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN"));
        String formattedTotal = totalAmount != null ? currencyFormat.format(totalAmount) : "0 đ";
        String payTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        String buyerName = buyer.getFullName() != null ? buyer.getFullName() : "Quý khách";

        StringBuilder ordersHtml = new StringBuilder();
        if (orders != null && !orders.isEmpty()) {
            for (Order order : orders) {
                String shopName = (order.getShop() != null && order.getShop().getShopName() != null)
                        ? order.getShop().getShopName()
                        : (order.getShop() != null ? order.getShop().getFullName() : "Cửa hàng nông sản");
                String orderTotal = order.getTotalAmount() != null ? currencyFormat.format(order.getTotalAmount()) : "0 đ";

                ordersHtml.append("""
                    <tr style="border-bottom: 1px solid #eeeeee;">
                        <td style="padding: 12px 8px; font-size: 14px; color: #333333;">
                            <strong>Đơn hàng #%d</strong><br/>
                            <span style="font-size: 12px; color: #666666;">Cửa hàng: %s</span>
                        </td>
                        <td style="padding: 12px 8px; font-size: 14px; color: #2e7d32; text-align: right; font-weight: bold;">
                            %s
                        </td>
                    </tr>
                """.formatted(order.getOrderId(), shopName, orderTotal));
            }
        }

        return """
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 24px; border: 1px solid #e0e0e0; border-radius: 12px; background-color: #ffffff;">
                <div style="text-align: center; margin-bottom: 20px;">
                    <h2 style="color: #2e7d32; margin: 0; font-size: 24px;">HÓA ĐƠN THANH TOÁN THÀNH CÔNG</h2>
                    <p style="color: #666; font-size: 14px; margin-top: 6px;">Nền Tảng Tiêu Thụ Nông Sản Việt Nam (APRP)</p>
                </div>
                <hr style="border: none; border-top: 1px solid #eeeeee; margin: 20px 0;" />
                <p style="font-size: 15px; color: #333333;">Xin chào <strong>%s</strong>,</p>
                <p style="font-size: 14px; color: #555555; line-height: 1.6;">
                    Giao dịch thanh toán trực tuyến của bạn qua cổng <strong>PayOS</strong> đã hoàn tất thành công. Các cửa hàng đã nhận được thông tin và sẽ tiến hành chuẩn bị nông sản để giao tới bạn sớm nhất!
                </p>
                <div style="background-color: #f9fbf9; border: 1px solid #c8e6c9; border-radius: 8px; padding: 16px; margin: 20px 0;">
                    <table style="width: 100%%; border-collapse: collapse;">
                        <tr>
                            <td style="padding: 4px 0; color: #666666; font-size: 13px;">Mã giao dịch PayOS:</td>
                            <td style="padding: 4px 0; font-weight: bold; color: #2e7d32; text-align: right; font-size: 13px;">#%s</td>
                        </tr>
                        <tr>
                            <td style="padding: 4px 0; color: #666666; font-size: 13px;">Thời gian thanh toán:</td>
                            <td style="padding: 4px 0; color: #333333; text-align: right; font-size: 13px;">%s</td>
                        </tr>
                        <tr>
                            <td style="padding: 4px 0; color: #666666; font-size: 13px;">Phương thức:</td>
                            <td style="padding: 4px 0; color: #333333; text-align: right; font-size: 13px;">PayOS QR / Ngân hàng</td>
                        </tr>
                        <tr>
                            <td style="padding: 4px 0; color: #666666; font-size: 13px;">Trạng thái:</td>
                            <td style="padding: 4px 0; font-weight: bold; color: #2e7d32; text-align: right; font-size: 13px;">ĐÃ THANH TOÁN (PAID)</td>
                        </tr>
                    </table>
                </div>

                <table style="width: 100%%; border-collapse: collapse; margin-top: 16px;">
                    <thead>
                        <tr style="background-color: #f1f8e9; border-bottom: 2px solid #c8e6c9;">
                            <th style="padding: 10px 8px; text-align: left; font-size: 13px; color: #2e7d32;">Chi tiết đơn hàng</th>
                            <th style="padding: 10px 8px; text-align: right; font-size: 13px; color: #2e7d32;">Thành tiền</th>
                        </tr>
                    </thead>
                    <tbody>
                        %s
                    </tbody>
                    <tfoot>
                        <tr>
                            <td style="padding: 16px 8px; font-size: 15px; font-weight: bold; color: #333333;">TỔNG TIỀN ĐÃ THANH TOÁN:</td>
                            <td style="padding: 16px 8px; font-size: 18px; font-weight: bold; color: #d32f2f; text-align: right;">%s</td>
                        </tr>
                    </tfoot>
                </table>

                <hr style="border: none; border-top: 1px solid #eeeeee; margin: 24px 0;" />
                <p style="font-size: 13px; color: #555555; text-align: center; line-height: 1.5;">
                    Cảm ơn bạn đã chung tay ủng hộ người nông dân và nông sản Việt Nam! 🌱<br/>
                    Mọi thắc mắc xin vui lòng liên hệ Bộ phận hỗ trợ APRP.
                </p>
                <p style="font-size: 11px; color: #999999; text-align: center; margin-top: 12px;">
                    Email này được tạo tự động bởi APRP. Vui lòng không phản hồi trực tiếp.
                </p>
            </div>
        """.formatted(buyerName, orderCode, payTime, ordersHtml.toString(), formattedTotal);
    }
}
