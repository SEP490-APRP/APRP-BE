package com.sang.be_capnong.dto.response.referral;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sang.be_capnong.enums.CommissionStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReferralCommissionResponse {

    Long id;
    Integer orderId;
    Integer orderDetailId;
    String productName;
    String productImageUrl;
    BigDecimal commissionRate;
    BigDecimal baseAmount;
    BigDecimal commissionAmount;
    CommissionStatus status;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime settledAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime createdAt;
}

