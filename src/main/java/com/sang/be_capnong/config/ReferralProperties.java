package com.sang.be_capnong.config;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
@ConfigurationProperties(prefix = "capnong.referral")
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReferralProperties {

    /** Base URL của link chia sẻ referral - Mặc định http://localhost:5173/ref/ cho local dev */
    String baseUrl = "http://localhost:5173/ref/";

    /** Tỷ lệ hoa hồng mặc định (%) - Mặc định 2.0% cho MVP */
    BigDecimal defaultCommissionRate = new BigDecimal("2.0");

    /** Số đơn hàng chuyển đổi tối đa cho mỗi attribution trong cửa sổ ghi nhận */
    int maxConversionsPerAttribution = 3;

    /** Cửa sổ ghi nhận attribution (ngày) - Mặc định 7 ngày */
    int windowDays = 7;

    /** Giới hạn số click ghi nhận tối đa từ 1 địa chỉ IP trong 1 phút */
    int clickRateLimitPerIp = 10;
}

