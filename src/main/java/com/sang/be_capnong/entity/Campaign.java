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

    /** Ngày dự kiến gom hàng tại điểm tập kết / giao */
    LocalDate deliveryDate;

    @Column(length = 200)
    String collectionSiteName;

    @Column(length = 500)
    String collectionSiteAddress;

    Double collectionSiteLatitude;
    Double collectionSiteLongitude;

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

    @Column(length = 500)
    String cancelReason;

    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
