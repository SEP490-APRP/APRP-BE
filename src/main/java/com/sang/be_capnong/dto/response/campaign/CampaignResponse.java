package com.sang.be_capnong.dto.response.campaign;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CampaignResponse {
    int campaignId;
    String code;
    String title;
    String description;
    Integer categoryId;
    String categoryName;
    String season;
    String coverImageUrl;
    BigDecimal targetVolumeKg;
    BigDecimal minThresholdKg;
    BigDecimal rescuePricePerKg;
    BigDecimal depositPercent;
    LocalDateTime cutoffAt;
    LocalDate deliveryDate;
    String collectionSiteName;
    String collectionSiteAddress;
    Double collectionSiteLatitude;
    Double collectionSiteLongitude;
    String status;
    /** Kg người mua đã cọc thành công */
    BigDecimal bookedVolumeKg;
    /** Kg còn nhận đặt = min(mục tiêu, tổng kg dư nhà vườn đã duyệt) - đã cọc - đang chờ cọc */
    BigDecimal remainingVolumeKg;
    /** % tiến độ so với mục tiêu (0-100) */
    BigDecimal progressPercent;
    /** Tổng kg dư của các nhà vườn đã được duyệt */
    BigDecimal approvedSurplusKg;
    long approvedFarmCount;
    long pendingFarmCount;

    // ── Cung - cầu khi gom hàng (THRESHOLD_MET trở đi) ──
    /** Kg người mua đã cọc và còn hiệu lực */
    BigDecimal soldVolumeKg;
    /** Kg đạt chuẩn đã nhận tại điểm tập kết (tính tối đa bằng phần phân bổ của từng nhà vườn) */
    BigDecimal acceptedVolumeKg;
    /** Kg đã đóng gói / giao */
    BigDecimal packedVolumeKg;
    /** Thiếu = đã bán - đạt chuẩn (0 nếu đủ) */
    BigDecimal shortfallVolumeKg;
    /** Số nhà vườn được phân bổ nhưng chưa cân nhận hàng */
    long intakePendingCount;
    LocalDateTime callPostedAt;
    LocalDateTime publishedAt;
    LocalDateTime evaluatedAt;
    LocalDateTime completedAt;
    LocalDateTime cancelledAt;
    String cancelReason;
    LocalDateTime createdAt;
}
