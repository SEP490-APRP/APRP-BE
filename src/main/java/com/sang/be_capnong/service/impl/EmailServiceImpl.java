package com.sang.be_capnong.service.impl;

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
}
