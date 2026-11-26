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

    /** Hub (điểm trung chuyển gần người mua) nơi xe CapNong đưa thùng tới, shipper lấy hàng tại đây */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hub_id")
    DeliveryHub hub;

    /** Khu giao / hub được gán khi đóng gói */
    @Column(length = 100)
    String deliveryZone;

    // ── Nhận hàng: DELIVERY (giao tận nơi) | PICKUP (tự đến điểm tập kết, 0đ) ──
    @Column(length = 16)
    String fulfillmentMethod;
    /** Phường/xã giao hàng, dùng để giảm phí gom đơn */
    @Column(length = 150)
    String deliveryWard;
    /** Khoảng cách điểm tập kết -> người mua (km) */
    Double shippingDistanceKm;
    /** Phí gốc theo khoảng cách (chưa giảm) */
    BigDecimal shippingBaseFee;
    /** Phí giao sau khi giảm theo kg (thu khi nhận hàng, nằm trong remainingAmount) */
    BigDecimal shippingFee;
    /** Giảm thêm do gom đơn cùng phường (trừ vào remainingAmount) */
    BigDecimal shippingDiscountAmount;
    /** Số lần đưa lại ra hub sau khi giao thất bại */
    Integer retryCount;

    /** Tiền thu hộ (phần còn lại) shipper đang ký quỹ trong ví cho lượt giao hiện tại */
    @Column(precision = 15, scale = 2)
    BigDecimal codReservedAmount;
    /** Đơn tự đến lấy: lần nhắc khách gần nhất và số lần đã nhắc */
    LocalDateTime pickupRemindedAt;
    Integer pickupRemindCount;
    /** Cước gộp giao chặng cuối được chia từ phí giao người mua trả (chốt cùng kế hoạch xe ra hub) */
    @Column(precision = 15, scale = 2)
    BigDecimal hubDeliveryPay;

    /** Tổng cước đã trả cho shipper (sau phí dịch vụ), cộng dồn qua các lượt giao */
    @Column(precision = 15, scale = 2)
    BigDecimal shipperIncomeAmount;

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
    /** Số thứ tự các thùng đã đóng khi đơn chưa đóng gói xong, vd "1,3" */
    @Column(length = 1000)
    String packedCrates;
    LocalDateTime readyAt;
    LocalDateTime pickedUpAt;
    /** Hub xác nhận đã nhận đủ đơn từ xe tải: chưa có thì shipper chưa thấy đơn */
    LocalDateTime hubReceivedAt;
    String hubReceivedBy;
    /** Hub báo thiếu / hư khi nhận */
    Integer hubIssueCrates;
    @Column(length = 500)
    String hubIssueNote;
    LocalDateTime deliveredAt;
    LocalDateTime cancelledAt;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    @OneToOne(mappedBy = "preOrder", fetch = FetchType.LAZY)
    CampaignDeposit deposit;

    /** Lô hàng của nhà vườn được dùng để đóng thùng cho đơn này (gán lúc đóng gói) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_registration_id")
    CampaignFarmRegistration sourceRegistration;
}
