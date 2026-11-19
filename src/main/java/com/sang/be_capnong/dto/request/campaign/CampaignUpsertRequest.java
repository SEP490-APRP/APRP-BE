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
    /** Ngày bắt đầu chiến dịch, không trước ngày chốt đơn (cutoffAt) */
    LocalDate startDate;
    /** Ngày giờ dự kiến đăng bài, trước hạn chốt đơn */
    LocalDateTime publishAt;
    /** Hạn chốt góp hàng của nhà vườn: sau ngày đăng bài, trước hạn chốt đơn đặt trước */
    LocalDateTime farmCutoffAt;
    /** Ngày kết thúc chiến dịch, không trước ngày bắt đầu */
    LocalDate endDate;
    LocalDate deliveryDate;
    String collectionSiteName;
    String collectionSiteAddress;
    Double collectionSiteLatitude;
    Double collectionSiteLongitude;
    java.math.BigDecimal shipHalfKg;
    java.math.BigDecimal shipFreeKg;
    Double shipFreeRadiusKm;
    Integer groupDiscountMinOrders;
    Boolean pickupEnabled;
}
