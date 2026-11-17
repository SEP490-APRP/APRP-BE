package com.sang.be_capnong.entity;

import com.sang.be_capnong.enums.WalletTopupStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Lệnh nạp tiền vào ví qua PayOS */
@Entity
@Table(name = "wallet_topups")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WalletTopup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    Wallet wallet;

    BigDecimal amount;

    @Column(name = "payos_order_code", unique = true, length = 32)
    String payosOrderCode;

    @Column(length = 500)
    String checkoutUrl;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    WalletTopupStatus status;

    LocalDateTime createdAt;
    LocalDateTime paidAt;
}
