package com.sang.be_capnong.entity;

import com.sang.be_capnong.enums.WalletTransactionType;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Sổ cái biến động ví. Mỗi (ví, đơn, loại) chỉ ghi đúng 1 lần nhờ unique constraint,
 * nên quyết toán chạy lại (retry, scheduler + buyer bấm cùng lúc) không cộng tiền 2 lần.
 */
@Entity
@Table(name = "wallet_transactions",
        uniqueConstraints = @UniqueConstraint(name = "uk_wallet_tx_wallet_order_type",
                columnNames = {"wallet_id", "order_id", "type"}),
        indexes = {
                @Index(name = "idx_wallet_tx_wallet_created", columnList = "wallet_id, created_at"),
                @Index(name = "idx_wallet_tx_order", columnList = "order_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id", nullable = false)
    Wallet wallet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    Order order;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "type", nullable = false, length = 40)
    WalletTransactionType type;

    /** Có dấu: dương = cộng, âm = trừ. Loại *_HOLD / HOLD_* tác động lên frozenBalance */
    @Column(nullable = false, precision = 15, scale = 2)
    BigDecimal amount;

    /** Số dư rút được sau bút toán */
    @Column(nullable = false, precision = 15, scale = 2)
    BigDecimal balanceAfter;

    /** Số tiền tạm giữ sau bút toán (null với dữ liệu cũ) */
    @Column(precision = 15, scale = 2)
    BigDecimal frozenAfter;

    @Column(length = 500)
    String description;

    @Column(name = "created_at", nullable = false)
    LocalDateTime createdAt;
}
