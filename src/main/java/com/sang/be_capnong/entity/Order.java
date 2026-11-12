package com.sang.be_capnong.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.sang.be_capnong.enums.CodPaymentOption;
import com.sang.be_capnong.enums.DeliveryFailReason;
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
    @Column(length = 32)
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
    LocalDateTime pickedUpAt;

    // Thời điểm Shipper giao thành công (DELIVERED) - dùng cho tự động hoàn tất đơn & thời hạn 24h hoàn hàng
    LocalDateTime deliveredAt;

    // Thời điểm đơn chuyển COMPLETED
    LocalDateTime completedAt;

    // Thời điểm đã chia tiền vào ví (Shop / Shipper / Sàn). NULL = chưa quyết toán
    LocalDateTime settledAt;

    BigDecimal rescuedWeight;

    // ── Ký quỹ COD: số tiền đã giữ chỗ trong ví Shipper khi nhận đơn. NULL = đơn nhận theo luồng cũ (ghi nợ) ──
    BigDecimal codReservedAmount;

    // ── Điều phối: đợt quét shipper gần nhất (1 = bán kính nhỏ nhất) ──
    /** Thời điểm Shop báo READY (mốc để cảnh báo chưa có tài xế) */
    LocalDateTime readyAt;
    Integer dispatchWave;
    LocalDateTime lastDispatchedAt;
    /** Đã báo Shop/Admin "chưa có tài xế nhận" */
    LocalDateTime dispatchAlertedAt;

    // ── Bằng chứng giao hàng ──
    /** Danh sách URL ảnh, ngăn cách bởi dấu xuống dòng */
    @Column(columnDefinition = "TEXT")
    String deliveryProofUrls;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    DeliveryFailReason failReason;

    @Column(length = 500)
    String failNote;

    Integer failCallAttempts;

    @Column(columnDefinition = "TEXT")
    String failProofUrls;

    LocalDateTime failedAt;

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
