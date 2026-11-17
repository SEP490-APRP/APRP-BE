package com.sang.be_capnong.entity;

import com.sang.be_capnong.enums.FarmRegistrationStatus;
import com.sang.be_capnong.enums.ShelfLifeTier;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Nhà vườn đăng ký lô hàng dư vào chiến dịch (GAP-04: chỉ khai số kg dư + ngày thu hoạch).
 * Việc cân, kiểm, chụp ảnh do Admin làm tại điểm tập kết (receivedWeightKg, qualityPassed, intakePhotoUrls).
 */
@Entity
// Một nhà vườn được đăng ký nhiều lô cho cùng chiến dịch (bỏ uk_campaign_farm, xem CampaignSchemaFixer)
@Table(name = "campaign_farm_registrations",
        indexes = @Index(name = "idx_cfr_campaign_shop", columnList = "campaign_id, shop_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CampaignFarmRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "registration_id")
    int registrationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    Campaign campaign;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shop_id", nullable = false)
    User shop;

    @Column(nullable = false, precision = 12, scale = 2)
    BigDecimal surplusVolumeKg;

    @Column(nullable = false)
    LocalDate harvestDate;

    @Column(length = 500)
    String note;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    FarmRegistrationStatus status;

    @Column(length = 500)
    String reviewNote;

    LocalDateTime reviewedAt;

    // ── Thông tin sản phẩm nhà vườn khai khi đăng ký ──

    /** Sản phẩm có sẵn trong gian hàng (tuỳ chọn); null nếu nhà vườn nhập tên mới */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    Product product;

    @Column(length = 200)
    String productName;

    /** Ảnh minh hoạ lô hàng do nhà vườn tải lên, ngăn cách bởi dấu phẩy */
    @Column(columnDefinition = "TEXT")
    String productImageUrls;

    /** Giá nhà vườn đề xuất (đ/kg) để thương lượng với CapNong */
    @Column(precision = 12, scale = 2)
    BigDecimal proposedPricePerKg;

    /** Giá chốt sau thương lượng (Admin nhập khi duyệt); null = dùng giá đề xuất */
    @Column(precision = 12, scale = 2)
    BigDecimal agreedPricePerKg;

    /** Nhà vườn đã tick cam kết giữ giá + số lượng + chất lượng như đã khai */
    Boolean priceCommitted;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 20)
    ShelfLifeTier shelfLifeTier;

    /** Số kg hệ thống phân bổ cho nhà vườn khi chiến dịch đủ ngưỡng */
    @Column(precision = 12, scale = 2)
    BigDecimal allocatedVolumeKg;

    /** Cân thực tế tại điểm tập kết */
    @Column(precision = 12, scale = 2)
    BigDecimal receivedWeightKg;

    /** Số kg hư hỏng / không đạt bị loại khi kiểm tại điểm tập kết */
    @Column(precision = 12, scale = 2)
    BigDecimal rejectedWeightKg;

    /** Số kg đạt chuẩn được ghi nhận vào chiến dịch = cân thực tế - hư hỏng */
    @Column(precision = 12, scale = 2)
    BigDecimal acceptedWeightKg;

    /** Kết quả kiểm tra chất lượng tại điểm tập kết */
    Boolean qualityPassed;

    /** Danh sách URL ảnh chụp tại điểm tập kết, ngăn cách bởi dấu phẩy */
    @Column(columnDefinition = "TEXT")
    String intakePhotoUrls;

    @Column(length = 500)
    String intakeNote;

    LocalDateTime intakeAt;

    /** Số tiền nhà vườn nhận (sau phí) khi chiến dịch hoàn tất */
    @Column(precision = 15, scale = 2)
    BigDecimal payoutAmount;

    LocalDateTime paidOutAt;

    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
