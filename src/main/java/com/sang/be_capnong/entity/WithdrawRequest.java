package com.sang.be_capnong.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sang.be_capnong.enums.PayoutStatus;
import com.sang.be_capnong.enums.WithdrawStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "withdraw_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WithdrawRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id")
    int requestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    Wallet wallet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    User admin;

    String reason;
    String adminNote;

    LocalDateTime processedAt;
    LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    WithdrawStatus status;

    BigDecimal receiveAmount;
    BigDecimal amount;
    BigDecimal fee;

    String bankName;
    String bankAccountNumber;
    String bankAccountHolder;

    @Enumerated(EnumType.STRING)
    PayoutStatus payoutStatus;

    String payoutProvider;
    String payoutOrderCode;
    String payoutTransactionId;

    @Column(columnDefinition = "TEXT")
    String payoutCheckoutUrl;

    @Column(columnDefinition = "TEXT")
    String payoutQrUrl;

    String payoutMessage;
    LocalDateTime payoutAt;

    @OneToMany(mappedBy = "withdrawRequest")
    @JsonIgnore
    List<Image> images;
}
