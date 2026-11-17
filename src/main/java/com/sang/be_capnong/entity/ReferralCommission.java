package com.sang.be_capnong.entity;

import com.sang.be_capnong.enums.CommissionStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "referral_commissions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_ref_comm_order_detail", columnNames = {"order_detail_id"})
        },
        indexes = {
                @Index(name = "idx_ref_comm_order", columnList = "order_id"),
                @Index(name = "idx_ref_comm_referrer", columnList = "referrer_id"),
                @Index(name = "idx_ref_comm_status", columnList = "status")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReferralCommission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    Order order;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_detail_id", nullable = false, unique = true)
    OrderDetail orderDetail;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "referral_link_id", nullable = false)
    ReferralLink referralLink;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referral_attribution_id")
    ReferralAttribution referralAttribution;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "referrer_id", nullable = false)
    User referrer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "buyer_id", nullable = false)
    User buyer;

    /** Tỷ lệ hoa hồng snapshot tại thời điểm đặt hàng (%) */
    @Column(name = "commission_rate", nullable = false, precision = 5, scale = 2)
    BigDecimal commissionRate;

    /** Giá trị cơ sở tính hoa hồng sau khi trừ voucher shop */
    @Column(name = "base_amount", nullable = false, precision = 15, scale = 2)
    BigDecimal baseAmount;

    /** Số tiền hoa hồng thực tế sau khi áp dụng Safety Cap (VNĐ) */
    @Column(name = "commission_amount", nullable = false, precision = 15, scale = 2)
    BigDecimal commissionAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    CommissionStatus status = CommissionStatus.PENDING;

    /** Thời điểm tiền hoa hồng được cộng thành công vào ví */
    @Column(name = "settled_at")
    LocalDateTime settledAt;

    @Column(name = "created_at", nullable = false)
    LocalDateTime createdAt;

    @Column(name = "updated_at")
    LocalDateTime updatedAt;
}

