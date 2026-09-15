package com.sang.be_capnong.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sang.be_capnong.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    int paymentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    Order order;

    LocalDateTime paymentDate;
    BigDecimal amount;
    String paymentGateway;

    @Enumerated(EnumType.STRING)
    PaymentStatus status;

    String image;
    String payosOrderCode;

    @Column(columnDefinition = "TEXT")
    String checkoutUrl;

    @Column(columnDefinition = "TEXT")
    String qrCodeUrl;

    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    @OneToMany(mappedBy = "payment")
    @JsonIgnore
    List<Transaction> transactions;
}
