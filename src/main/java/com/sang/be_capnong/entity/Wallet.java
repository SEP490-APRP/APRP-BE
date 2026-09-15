package com.sang.be_capnong.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sang.be_capnong.enums.WalletStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "wallets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wallet_id")
    int walletId;

    @Enumerated(EnumType.STRING)
    WalletStatus status;

    String type;

    @Builder.Default
    BigDecimal totalBalance = BigDecimal.ZERO;

    @Builder.Default
    BigDecimal frozenBalance = BigDecimal.ZERO;

    @Builder.Default
    BigDecimal totalRevenueAllTime = BigDecimal.ZERO;

    @Builder.Default
    BigDecimal totalWithdrawn = BigDecimal.ZERO;

    @Builder.Default
    BigDecimal commissionWallet = BigDecimal.ZERO;

    LocalDateTime createAt;
    LocalDateTime updateAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @OneToMany(mappedBy = "wallet")
    @JsonIgnore
    List<WithdrawRequest> withdrawRequests;
}
