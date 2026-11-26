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
    /** Nông sản cần giải cứu (bắt buộc) */
    String productName;
    BigDecimal targetVolumeKg;
    BigDecimal minThresholdKg;
    BigDecimal rescuePricePerKg;
    /** Tỷ lệ cọc 0-1, vd 0.3 = 30% */
    BigDecimal depositPercent;
    LocalDateTime cutoffAt;
    /** Ngày bắt đầu mở điểm tập kết tại điểm tập kết, sau ngày chốt đơn (cutoffAt) */
    LocalDate startDate;
    /** Ngày giờ dự kiến đăng bài, trước hạn chốt đơn */
    LocalDateTime publishAt;
    /** Hạn chốt góp hàng của nhà vườn: sau ngày đăng bài, trước hạn chốt đơn đặt trước */
    LocalDateTime farmCutoffAt;
    /** Ngày kết thúc mở điểm tập kết, không trước ngày bắt đầu mở điểm tập kết (cũng là ngày cần xe ra hub) */
    LocalDate endDate;
    /** Ngày kết thúc chiến dịch (tài xế giao xong), không trước ngày kết thúc mở điểm tập kết */
    LocalDate finishDate;
    LocalDate deliveryDate;
    String collectionSiteName;
    String collectionSiteAddress;
    Double collectionSiteLatitude;
    Double collectionSiteLongitude;
    Boolean pickupEnabled;
}
