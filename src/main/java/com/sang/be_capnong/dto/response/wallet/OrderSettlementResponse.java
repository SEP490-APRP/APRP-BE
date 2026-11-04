package com.sang.be_capnong.dto.response.wallet;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Bảng chia tiền của 1 đơn hàng.
 * Luôn đúng: buyerPaid = shopReceive + shipperReceive + platformReceive.
 * Nếu đơn chưa COMPLETED thì đây là số dự kiến (settled = false, transactions rỗng).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderSettlementResponse {
    int orderId;
    String orderStatus;
    String paymentMethod;
    boolean settled;
    LocalDateTime settledAt;

    // ── Đầu vào ──
    BigDecimal goodsAmount;
    BigDecimal shippingFee;
    /** Voucher do Shop tạo: Shop tự chịu */
    BigDecimal shopVoucherDiscount;
    /** Voucher giảm tiền hàng do sàn tạo: sàn bù cho Shop */
    BigDecimal platformVoucherDiscount;
    /** Voucher freeship do sàn tạo: sàn bù cho Shipper */
    BigDecimal platformFreeshipDiscount;
    /** Tiền Buyer thực trả (= orders.total_amount) */
    BigDecimal buyerPaid;

    // ── Phí ──
    BigDecimal farmerFeeRate;
    /** Phí dịch vụ nông dân = farmerFeeRate x (tiền hàng - voucher shop) */
    BigDecimal farmerFee;
    BigDecimal shipperFeeRate;
    /** Phí dịch vụ shipper = shipperFeeRate x cước giao hàng */
    BigDecimal shipperFee;

    // ── Kết quả chia ──
    Party shop;
    BigDecimal shopReceive;
    Party shipper;
    BigDecimal shipperReceive;
    /** Sàn thực nhận = phí nông dân + phí shipper - tiền sàn bù voucher */
    BigDecimal platformReceive;

    // ── COD ──
    BigDecimal codAmount;
    LocalDateTime codCashCollectedAt;
    Boolean codSettled;
    LocalDateTime codSettledAt;

    // ── Hoàn tiền ──
    BigDecimal refundAmount;
    String refundReason;

    List<WalletTransactionResponse> transactions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Party {
        Integer userId;
        String name;
    }
}
