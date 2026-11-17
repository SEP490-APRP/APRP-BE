package com.sang.be_capnong.entity;

import com.sang.be_capnong.enums.ReferralTargetType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "referral_links",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_ref_code", columnNames = {"referral_code"}),
                @UniqueConstraint(name = "uk_referrer_target", columnNames = {"referrer_id", "target_type", "target_id"})
        },
        indexes = {
                @Index(name = "idx_ref_code", columnList = "referral_code"),
                @Index(name = "idx_ref_referrer", columnList = "referrer_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReferralLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "referral_code", nullable = false, length = 32)
    String referralCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "referrer_id", nullable = false)
    User referrer;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    ReferralTargetType targetType;

    @Column(name = "target_id", nullable = false)
    Integer targetId;

    /** Tỷ lệ hoa hồng ghi đè (%) nếu có, NULL = dùng mặc định cấu hình hệ thống (2.0%) */
    @Column(name = "commission_rate", precision = 5, scale = 2)
    BigDecimal commissionRate;

    @Column(name = "clicks_count", nullable = false)
    @Builder.Default
    Integer clicksCount = 0;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    Boolean isActive = true;

    @Column(name = "created_at", nullable = false)
    LocalDateTime createdAt;

    @Column(name = "updated_at")
    LocalDateTime updatedAt;
}

