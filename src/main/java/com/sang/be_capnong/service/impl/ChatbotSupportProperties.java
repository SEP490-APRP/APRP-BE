package com.sang.be_capnong.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Kênh hỗ trợ của sàn, đặt trong cấu hình (CAPNONG_SUPPORT_*). Để trống thì bot không hiện kênh đó,
 * tránh đưa cho khách số điện thoại hoặc link chưa được xác nhận.
 */
@Component
public class ChatbotSupportProperties {

    private final String hotline;
    private final String zaloUrl;
    private final String email;

    public ChatbotSupportProperties(
            @Value("${CAPNONG_SUPPORT_HOTLINE:}") String hotline,
            @Value("${CAPNONG_SUPPORT_ZALO_URL:}") String zaloUrl,
            @Value("${CAPNONG_SUPPORT_EMAIL:}") String email) {
        this.hotline = blankToNull(hotline);
        this.zaloUrl = blankToNull(zaloUrl);
        this.email = blankToNull(email);
    }

    public String getHotline() {
        return hotline;
    }

    public String getZaloUrl() {
        return zaloUrl;
    }

    public String getEmail() {
        return email;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
