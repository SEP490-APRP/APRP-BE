package com.sang.be_capnong.dto.response.campaign;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DamageClaimResponse {
    Integer claimId;
    Integer preOrderId;
    String preOrderCode;
    Integer campaignId;
    String campaignTitle;
    String buyerName;
    String buyerPhone;
    BigDecimal orderKg;
    BigDecimal pricePerKg;
    String status;
    BigDecimal claimedKg;
    List<String> photoUrls;
    String reason;
    BigDecimal approvedKg;
    BigDecimal refundAmount;
    String adminNote;
    String sourceShopName;
    LocalDateTime deliveredAt;
    LocalDateTime createdAt;
    LocalDateTime reviewedAt;
}
