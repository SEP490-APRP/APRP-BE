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
    String recipientName;
    String recipientPhone;
    String shippingAddress;
    Double shippingLatitude;
    Double shippingLongitude;
    String note;
    String deliveryZone;
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
