package com.sang.be_capnong.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sang.be_capnong.enums.BatchStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "checkout_batches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CheckoutBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    String payosOrderCode;
    String paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    BatchStatus status;

    BigDecimal totalAmount;
    BigDecimal batchDiscountAmount;

    @Column(columnDefinition = "TEXT")
    String checkoutUrl;

    @Column(columnDefinition = "TEXT")
    String qrCodeUrl;

    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    User buyer;

    @OneToMany(mappedBy = "checkoutBatch")
    @JsonIgnore
    List<Order> orders;
}
