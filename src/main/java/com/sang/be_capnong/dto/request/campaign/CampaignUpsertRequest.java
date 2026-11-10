package com.sang.be_capnong.dto.request.campaign;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Admin tạo / sửa chiến dịch (chỉ khi DRAFT) */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CampaignUpsertRequest {
    String title;
    String description;
    Integer categoryId;
    String season;
    String coverImageUrl;
    BigDecimal targetVolumeKg;
    BigDecimal minThresholdKg;
    BigDecimal rescuePricePerKg;
    /** Tỷ lệ cọc 0-1, vd 0.3 = 30% */
    BigDecimal depositPercent;
    LocalDateTime cutoffAt;
    LocalDate deliveryDate;
    String collectionSiteName;
    String collectionSiteAddress;
    Double collectionSiteLatitude;
    Double collectionSiteLongitude;
}
