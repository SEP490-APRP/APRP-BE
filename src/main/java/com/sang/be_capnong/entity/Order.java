package com.sang.be_capnong.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sang.be_capnong.enums.CodPaymentOption;
import com.sang.be_capnong.enums.CodPaymentStatus;
import com.sang.be_capnong.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    int orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    User buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    User shop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipper_id")
    User shipper;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checkout_batch_id")
    CheckoutBatch checkoutBatch;

    BigDecimal shippingFee;

    @Column(length = 50)
    String recipientName;

    @Column(length = 20)
    String recipientPhone;

    @Column(length = 255)
    String shippingAddress;

    LocalDateTime estimatedDeliveryDate;

    @Column(length = 500)
    String note;

    @Enumerated(EnumType.STRING)
    OrderStatus status;

    LocalDateTime createdAt;
    BigDecimal totalAmount;
    String paymentMethod;

    BigDecimal shippingDiscountAmount;
    BigDecimal batchDiscountAmount;

    Double shippingLatitude;
    Double shippingLongitude;
    Double shopLatitude;
    Double shopLongitude;

    BigDecimal refundAmount;
    String refundReason;

    BigDecimal rewardPoints;
    BigDecimal codAmount;
    Boolean codSettled;
    LocalDateTime codSettledAt;
    LocalDateTime codCashConfirmedAt;

    @Enumerated(EnumType.STRING)
    CodPaymentOption codPaymentOption;

    @Enumerated(EnumType.STRING)
    CodPaymentStatus codPaymentStatus;

    BigDecimal codPrepaidAmount;
    LocalDateTime codRefundedAt;
    LocalDateTime shipperPaidAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    List<OrderDetail> orderDetails;

    @OneToMany(mappedBy = "order")
    @JsonIgnore
    List<Payment> payments;

    @OneToMany(mappedBy = "order")
    @JsonIgnore
    List<ShipperLocation> shipperLocations;

    @OneToMany(mappedBy = "order")
    @JsonIgnore
    List<VoucherUsage> voucherUsages;
}
