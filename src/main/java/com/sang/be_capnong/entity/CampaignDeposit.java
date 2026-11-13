package com.sang.be_capnong.entity;

import com.sang.be_capnong.enums.CampaignDepositStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Tiền cọc PayOS của một PreOrder (Review 2 gọi là EscrowWallet).
 * Tiền nằm trong frozenBalance của ví Sàn tới khi giải ngân cho nhà vườn hoặc hoàn lại người mua.
 */
@Entity
@Table(name = "campaign_deposits")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CampaignDeposit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "deposit_id")
    int depositId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pre_order_id", nullable = false, unique = true)
    PreOrder preOrder;

    @Column(nullable = false, precision = 15, scale = 2)
    BigDecimal amount;

    @Column(unique = true, length = 30)
    String payosOrderCode;

    @Column(columnDefinition = "TEXT")
    String checkoutUrl;

    @Column(columnDefinition = "TEXT")
    String qrCodeUrl;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    CampaignDepositStatus status;

    LocalDateTime heldAt;
    LocalDateTime releasedAt;
    LocalDateTime refundedAt;
    /** Phần cọc trả bằng ví người mua; phần còn lại (amount - walletAmount) qua PayOS */
    BigDecimal walletAmount;
    /** Đã hoàn phần ví khi đơn huỷ lúc cọc chưa xong */
    LocalDateTime walletRefundedAt;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
