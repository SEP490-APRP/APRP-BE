package com.sang.be_capnong.dto.response.campaign;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PreOrderResponse {
    int preOrderId;
    String code;
    int campaignId;
    String campaignTitle;
    String campaignStatus;
    int buyerId;
    String buyerName;
    String crateSize;
    int crateKg;
    int crateQuantity;
    BigDecimal totalKg;
    BigDecimal unitPrice;
    BigDecimal totalAmount;
    BigDecimal depositAmount;
    BigDecimal remainingAmount;
    /** Phần cọc đã trả bằng ví */
    BigDecimal depositWalletAmount;
    /** Phần cọc trả qua PayOS */
    BigDecimal depositPayosAmount;
    String recipientName;
    String recipientPhone;
    String shippingAddress;
    Double shippingLatitude;
    Double shippingLongitude;
    String note;
    String deliveryZone;
    String fulfillmentMethod;
    String deliveryWard;
    Double shippingDistanceKm;
    BigDecimal shippingBaseFee;
    BigDecimal shippingFee;
    /** Đơn tự đến lấy: lần nhắc khách gần nhất và số lần đã nhắc */
    LocalDateTime pickupRemindedAt;
    Integer pickupRemindCount;
    /** Shipper: tiền thu hộ đang ký quỹ trong ví cho lượt giao hiện tại */
    BigDecimal codReservedAmount;
    /** Shipper: tổng cước đã nhận cho đơn này (sau phí dịch vụ) */
    BigDecimal shipperIncomeAmount;
    BigDecimal shippingDiscountAmount;
    String collectionSiteName;
    String collectionSiteAddress;
    /** Độ gấp khi giao: URGENT (Gấp) | HIGH (Ưu tiên) | NORMAL (Thường) */
    String priorityLevel;
    String priorityReason;
    /** Hạn nên giao tới khách để nông sản còn tươi */
    LocalDateTime deliverBy;
    String shelfLifeTier;
    Integer retryCount;
    /** Báo hàng hư: hạn báo (48h sau khi nhận), yêu cầu mới nhất, tổng tiền đã được hoàn */
    java.time.LocalDateTime damageClaimDeadline;
    String damageClaimStatus;
    BigDecimal damageClaimKg;
    BigDecimal damageRefundTotal;
    /** Lô hàng nhà vườn dùng cho đơn (theo phân loại thời gian bảo quản shop đã khai) */
    Integer sourceRegistrationId;
    String sourceShopName;
    String sourceProductName;
    java.time.LocalDate sourceHarvestDate;
    String qrCode;
    String status;
    Integer shipperId;
    String shipperName;
    String podImageUrl;
    String failReason;
    String cancelReason;
    Integer originalCrateQuantity;
    BigDecimal shortfallRefundAmount;
    // Tiền cọc
    String depositStatus;
    String payosOrderCode;
    String checkoutUrl;
    String qrCodeUrl;
    LocalDateTime depositPaidAt;
    LocalDateTime packedAt;
    LocalDateTime readyAt;
    LocalDateTime pickedUpAt;
    LocalDateTime deliveredAt;
    LocalDateTime cancelledAt;
    LocalDateTime createdAt;
}
