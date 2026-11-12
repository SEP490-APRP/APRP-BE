package com.sang.be_capnong.entity;

import com.sang.be_capnong.enums.AttributionStatus;
import com.sang.be_capnong.enums.ReferralTargetType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "referral_attributions",
        indexes = {
                @Index(name = "idx_attr_buyer_target", columnList = "buyer_id, target_type, target_id, status"),
                @Index(name = "idx_attr_expires", columnList = "expires_at")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReferralAttribution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "buyer_id", nullable = false)
    User buyer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "referral_link_id", nullable = false)
    ReferralLink referralLink;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "referrer_id", nullable = false)
    User referrer;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 20)
    ReferralTargetType targetType;

    @Column(name = "target_id", nullable = false)
    Integer targetId;

    @Column(name = "attributed_at", nullable = false)
    LocalDateTime attributedAt;

    @Column(name = "expires_at", nullable = false)
    LocalDateTime expiresAt;

    @Column(name = "converted_order_count", nullable = false)
    @Builder.Default
    Integer convertedOrderCount = 0;

    @Column(name = "max_conversions", nullable = false)
    @Builder.Default
    Integer maxConversions = 3;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    AttributionStatus status = AttributionStatus.ACTIVE;
}

