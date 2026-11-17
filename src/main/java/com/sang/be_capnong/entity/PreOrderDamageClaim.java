package com.sang.be_capnong.entity;

import com.sang.be_capnong.enums.DamageClaimStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Báo hàng hư (thay cho hoàn hàng): người mua tự cân phần hư, chụp ảnh cân + hàng làm bằng chứng.
 * Admin xem ảnh, chốt số kg và hoàn tiền phần hư vào ví người mua từ ví sàn. Không phải gửi hàng về.
 */
@Entity
@Table(name = "pre_order_damage_claims", indexes = {
        @Index(name = "idx_damage_claim_pre_order", columnList = "pre_order_id"),
        @Index(name = "idx_damage_claim_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PreOrderDamageClaim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "claim_id")
    Integer claimId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pre_order_id", nullable = false)
    PreOrder preOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "buyer_id", nullable = false)
    User buyer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    DamageClaimStatus status;

    /** Số kg hư người mua tự cân */
    @Column(nullable = false, precision = 10, scale = 2)
    BigDecimal claimedKg;

    /** Ảnh cân + ảnh hàng hư, ngăn cách bởi dấu phẩy */
    @Column(length = 2000, nullable = false)
    String photoUrls;

    @Column(length = 500)
    String reason;

    /** Số kg admin chấp nhận (có thể thấp hơn số người mua khai) */
    @Column(precision = 10, scale = 2)
    BigDecimal approvedKg;

    /** Tiền hoàn vào ví người mua = approvedKg x giá/kg */
    @Column(precision = 15, scale = 2)
    BigDecimal refundAmount;

    @Column(length = 500)
    String adminNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    User reviewedBy;

    LocalDateTime createdAt;
    LocalDateTime reviewedAt;
}
