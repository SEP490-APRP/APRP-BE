package com.sang.be_capnong.dto.response.referral;

import com.sang.be_capnong.enums.ReferralTargetType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReferralResolveResponse {

    String referralCode;
    ReferralTargetType targetType;
    Integer targetId;
    String targetName;
    String targetUrl;
    String imageUrl;
    BigDecimal price;
    String shopName;
    String referrerName;
}

