package com.sang.be_capnong.entity;

import com.sang.be_capnong.enums.CampaignStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Chiến dịch giải cứu nhiều nhà vườn do Admin tạo (Review 2 - F-01 / WF-01 / GAP-01).
 * Admin đặt sản lượng mục tiêu, ngưỡng tối thiểu, giá giải cứu, ngày chốt và điểm tập kết.
 */
@Entity
@Table(name = "campaigns", indexes = {
        @Index(name = "idx_campaign_status_cutoff", columnList = "status, cutoff_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Campaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "campaign_id")
    int campaignId;

    @Column(nullable = false, unique = true, length = 30)
    String code;

    @Column(nullable = false, length = 200)
    String title;

    @Column(columnDefinition = "TEXT")
    String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    Category category;

    /** Tên mùa vụ, vd "Vụ vải thiều 2026" */
    @Column(length = 100)
    String season;

    @Column(columnDefinition = "TEXT")
    String coverImageUrl;

    /** Sản lượng mục tiêu (kg) - nhận đặt trước tối đa tới mức này */
    @Column(nullable = false, precision = 12, scale = 2)
    BigDecimal targetVolumeKg;

    /** Ngưỡng tối thiểu (kg) để chiến dịch chạy tiếp lúc chốt; thiếu thì huỷ + hoàn cọc */
    @Column(nullable = false, precision = 12, scale = 2)
    BigDecimal minThresholdKg;

    /** Giá giải cứu (VNĐ/kg) do Admin đặt */
    @Column(nullable = false, precision = 15, scale = 2)
    BigDecimal rescuePricePerKg;

    /** Tỷ lệ cọc, vd 0.30 = 30% */
    @Column(nullable = false, precision = 5, scale = 4)
    BigDecimal depositPercent;

    /** Hạn chốt nhận đặt trước */
    @Column(name = "cutoff_at", nullable = false)
    LocalDateTime cutoffAt;

    /** Ngày bắt đầu chiến dịch, sau hạn chốt đơn đặt trước (cutoffAt) khi đã đủ người đặt */
    @Column(name = "start_date")
    LocalDate startDate;

    /** Ngày giờ đăng bài kêu gọi nhà vườn góp hàng (tới giờ tự gửi lời kêu gọi) */
    @Column(name = "publish_at")
    LocalDateTime publishAt;

    /** Hạn chốt góp hàng của nhà vườn: sau ngày đăng bài, trước hạn chốt đơn đặt trước */
    @Column(name = "farm_cutoff_at")
    LocalDateTime farmCutoffAt;

    /** Đã kiểm tra số kg nhà vườn góp lúc chốt góp hàng (đủ ngưỡng thì giữ, thiếu thì đã tự huỷ) */
    @Column(name = "farm_cutoff_checked_at")
    LocalDateTime farmCutoffCheckedAt;

    /** Lần tự đăng theo publishAt thất bại (vd chưa có nhà vườn được duyệt); admin sửa chiến dịch thì xoá để thử lại */
    @Column(name = "auto_publish_failed_at")
    LocalDateTime autoPublishFailedAt;

    /** Ngày kết thúc chiến dịch, không trước ngày bắt đầu */
    @Column(name = "end_date")
    LocalDate endDate;

    /** Ngày dự kiến gom hàng tại điểm tập kết / giao */
    LocalDate deliveryDate;

    @Column(length = 200)
    String collectionSiteName;

    @Column(length = 500)
    String collectionSiteAddress;

    Double collectionSiteLatitude;
    Double collectionSiteLongitude;

    // ── Phí giao hàng (null = mặc định trong CampaignShippingCalculator) ──
    /** Từ bấy nhiêu kg trở lên giảm 50% phí giao (mặc định 10) */
    BigDecimal shipHalfKg;
    /** Từ bấy nhiêu kg trở lên miễn phí giao trong bán kính shipFreeRadiusKm (mặc định 20) */
    BigDecimal shipFreeKg;
    /** Bán kính miễn phí giao (km, mặc định 10) */
    Double shipFreeRadiusKm;
    /** Phường có từ bấy nhiêu đơn trở lên được giảm thêm 50% phí giao (mặc định 5) */
    Integer groupDiscountMinOrders;
    /** Cho phép tự đến điểm tập kết lấy hàng (mặc định có) */
    Boolean pickupEnabled;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    CampaignStatus status;

    /** Tổng kg người mua đã cọc thành công (PreOrder DEPOSIT_PAID trở đi) */
    @Builder.Default
    @Column(nullable = false, precision = 12, scale = 2)
    BigDecimal bookedVolumeKg = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    User createdBy;

    /** Thời điểm Admin đăng lời kêu gọi nhà vườn tham gia */
    LocalDateTime callPostedAt;
    LocalDateTime publishedAt;
    LocalDateTime evaluatedAt;
    LocalDateTime completedAt;
    LocalDateTime cancelledAt;

    /** Kêu gọi bổ sung: mở khi mọi nhà vườn đã giao đủ phần được chia mà hàng đạt chuẩn vẫn thiếu so với số đã bán */
    LocalDateTime topUpCallAt;
    LocalDateTime topUpClosedAt;
    /** Số kg cần bổ sung tại thời điểm mở kêu gọi */
    @Column(precision = 12, scale = 2)
    BigDecimal topUpKg;

    @Column(length = 500)
    String cancelReason;

    /** Banner trang chủ do Admin tuỳ chỉnh: AUTO (dựng từ thông tin chiến dịch) | IMAGE (ảnh tải lên). null = AUTO */
    @Column(length = 10)
    String bannerMode;
    /** ORANGE | GREEN | RED */
    @Column(length = 20)
    String bannerTheme;
    @Column(length = 200)
    String bannerHeadline;
    @Column(length = 300)
    String bannerSubline;
    @Column(length = 60)
    String bannerBadge;
    @Column(length = 60)
    String bannerCtaText;
    @Column(length = 500)
    String bannerImageUrl;
    /** null = có hiện ở trang chủ */
    Boolean bannerOnHome;
    LocalDateTime bannerUpdatedAt;

    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
