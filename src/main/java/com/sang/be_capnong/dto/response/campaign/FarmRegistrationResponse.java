package com.sang.be_capnong.dto.response.campaign;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FarmRegistrationResponse {
    int registrationId;
    int campaignId;
    String campaignTitle;
    String campaignStatus;
    LocalDateTime campaignCutoffAt;
    String collectionSiteName;
    String collectionSiteAddress;
    LocalDate deliveryDate;
    int shopId;
    String shopName;
    String shopPhone;
    BigDecimal surplusVolumeKg;
    LocalDate harvestDate;
    String note;
    Integer productId;
    String productName;
    List<String> productImageUrls;
    BigDecimal proposedPricePerKg;
    BigDecimal agreedPricePerKg;
    /** Giá áp dụng khi chi trả: giá chốt, nếu chưa có thì giá đề xuất, cuối cùng là giá giải cứu */
    BigDecimal effectivePricePerKg;
    BigDecimal campaignPricePerKg;
    Boolean priceCommitted;
    String shelfLifeTier;
    String status;
    String reviewNote;
    LocalDateTime reviewedAt;
    BigDecimal allocatedVolumeKg;
    BigDecimal receivedWeightKg;
    BigDecimal rejectedWeightKg;
    /** Kg đạt chuẩn ghi nhận vào chiến dịch */
    BigDecimal acceptedWeightKg;
    /** Kg được tính tiền = min(đạt chuẩn, phân bổ) */
    BigDecimal countedWeightKg;
    /** Kg thiếu so với phân bổ (0 nếu đủ) */
    BigDecimal shortfallKg;
    Boolean qualityPassed;
    List<String> intakePhotoUrls;
    String intakeNote;
    LocalDateTime intakeAt;
    BigDecimal payoutAmount;
    LocalDateTime paidOutAt;
    LocalDateTime createdAt;
}
