package com.sang.be_capnong.dto.response.referral;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sang.be_capnong.enums.ReferralTargetType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReferralLinkResponse {

    Long id;
    String referralCode;
    String shareUrl;
    ReferralTargetType targetType;
    Integer targetId;
    String targetName;
    String targetImageUrl;
    BigDecimal commissionRate;
    Integer clicksCount;
    Boolean isActive;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime createdAt;
}

