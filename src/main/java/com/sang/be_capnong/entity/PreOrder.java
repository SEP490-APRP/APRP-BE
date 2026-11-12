package com.sang.be_capnong.entity;

import com.sang.be_capnong.enums.CrateSize;
import com.sang.be_capnong.enums.PreOrderStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Đơn đặt trước thùng chuẩn của người mua trong một chiến dịch (Review 2 gọi là RFQContract).
 */
@Entity
@Table(name = "pre_orders", indexes = {
        @Index(name = "idx_pre_order_campaign_status", columnList = "campaign_id, status"),
        @Index(name = "idx_pre_order_buyer", columnList = "buyer_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PreOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pre_order_id")
    int preOrderId;

    @Column(nullable = false, unique = true, length = 30)
    String code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    Campaign campaign;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "buyer_id", nullable = false)
    User buyer;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    CrateSize crateSize;

    @Column(nullable = false)
    int crateQuantity;

    @Column(nullable = false, precision = 12, scale = 2)
    BigDecimal totalKg;

    /** Giá/kg chốt tại thời điểm đặt */
    @Column(nullable = false, precision = 15, scale = 2)
    BigDecimal unitPrice;

    @Column(nullable = false, precision = 15, scale = 2)
    BigDecimal totalAmount;

    @Column(nullable = false, precision = 15, scale = 2)
    BigDecimal depositAmount;

    /** Phần còn lại người mua trả khi nhận hàng */
    @Column(nullable = false, precision = 15, scale = 2)
    BigDecimal remainingAmount;

    @Column(length = 100)
    String recipientName;

    @Column(length = 20)
    String recipientPhone;

    @Column(length = 500)
    String shippingAddress;

    Double shippingLatitude;
    Double shippingLongitude;

    @Column(length = 500)
    String note;

    /** Khu giao / hub được gán khi đóng gói */
    @Column(length = 100)
    String deliveryZone;

    /** Nội dung tem QR dán trên thùng */
    @Column(length = 100)
    String qrCode;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    PreOrderStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipper_id")
    User shipper;

    @Column(columnDefinition = "TEXT")
    String podImageUrl;

    @Column(length = 500)
    String failReason;

    @Column(length = 500)
    String cancelReason;

    /** Số thùng ban đầu khi đặt, chỉ có khi đơn bị giảm thùng do thiếu hàng */
    Integer originalCrateQuantity;

    /** Tiền cọc đã hoàn vào ví do thiếu hàng (giảm thùng hoặc huỷ) */
    @Column(precision = 15, scale = 2)
    BigDecimal shortfallRefundAmount;

    LocalDateTime depositPaidAt;
    LocalDateTime packedAt;
    LocalDateTime readyAt;
    LocalDateTime pickedUpAt;
    LocalDateTime deliveredAt;
    LocalDateTime cancelledAt;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    @OneToOne(mappedBy = "preOrder", fetch = FetchType.LAZY)
    CampaignDeposit deposit;
}
