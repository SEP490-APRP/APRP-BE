package com.sang.be_capnong.dto.response.referral;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReferralStatsResponse {

    BigDecimal totalEarned;
    BigDecimal pendingCommission;
    long totalClicks;
    long totalOrders;
    BigDecimal defaultCommissionRate;
}

