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
    /** Banner trang chủ (Admin tuỳ chỉnh). bannerMode null = AUTO */
    String bannerMode;
    String bannerTheme;
    String bannerHeadline;
    String bannerSubline;
    String bannerBadge;
    String bannerCtaText;
    String bannerImageUrl;
    Boolean bannerOnHome;
    java.time.LocalDateTime bannerUpdatedAt;
    /** Thứ tự trên banner đầu trang chủ; null = không gắn */
    Integer homeSlideOrder;
    /** Nông sản cần giải cứu */
    String productName;
    /** Tổng hàng cần giải cứu = lớn hơn giữa mục tiêu và tổng kg lô đã duyệt */
    BigDecimal targetVolumeKg;
    /** Mục tiêu admin đặt khi tạo chiến dịch */
    BigDecimal plannedTargetKg;
    BigDecimal minThresholdKg;
    BigDecimal rescuePricePerKg;
    BigDecimal depositPercent;
    LocalDateTime cutoffAt;
    LocalDate startDate;
    LocalDate endDate;
    LocalDate finishDate;
    LocalDateTime publishAt;
    LocalDateTime farmCutoffAt;
    LocalDate deliveryDate;
    String collectionSiteName;
    String collectionSiteAddress;
    Double collectionSiteLatitude;
    Double collectionSiteLongitude;
    /** Quy tắc phí giao (đã áp mặc định) */
    BigDecimal shipHalfKg;
    BigDecimal shipFreeKg;
    BigDecimal shipMidFee;
    BigDecimal shipSmallFee;
    Boolean pickupEnabled;
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
    /** Số lô (đơn đăng ký) đã duyệt: một nhà vườn có thể góp nhiều lô */
    long approvedLotCount;
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
    /** Báo cáo tổng kết công khai, chỉ có khi chiến dịch hoàn tất (số gộp, không có thông tin cá nhân) */
    BigDecimal deliveredKg;
    Long deliveredOrderCount;
    /** Đơn đã cọc (không tính đơn huỷ / chưa cọc) */
    Long totalOrderCount;
    Long buyerCount;
    /** Đang kêu gọi nhà vườn bổ sung hàng cho phần thiếu */
    boolean topUpOpen;
    /** Hàng tại điểm tập kết: hư hỏng, bổ sung, phần bị bớt; null khi chưa cân lô nào */
    SupplySummaryResponse supply;
    LocalDateTime topUpCallAt;
    LocalDateTime topUpClosedAt;
    BigDecimal topUpKg;
    /** Kg còn cần bổ sung = thiếu - phần đã chia cho lô bổ sung chưa giao */
    BigDecimal topUpRemainingKg;
    LocalDateTime callPostedAt;
    LocalDateTime shipperCalledAt;
    Integer shipperCallTrips;
    LocalDateTime publishedAt;
    LocalDateTime evaluatedAt;
    LocalDateTime completedAt;
    LocalDateTime cancelledAt;
    String cancelReason;
    LocalDateTime createdAt;
}
