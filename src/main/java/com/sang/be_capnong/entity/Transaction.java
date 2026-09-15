package com.sang.be_capnong.entity;

import com.sang.be_capnong.enums.TransactionStatus;
import com.sang.be_capnong.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    int transactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    Payment payment;

    String paymentGateway;
    BigDecimal amount;
    BigDecimal balanceAfter;

    @Enumerated(EnumType.STRING)
    TransactionStatus status;

    @Column(length = 500)
    String content;

    @Enumerated(EnumType.STRING)
    TransactionType transactionType;

    String referenceId;
}
