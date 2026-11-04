package com.sang.be_capnong.dto.response.wallet;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Số liệu tổng hợp cho màn Ví Sàn của Admin */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PlatformOverviewResponse {
    /** TODAY | 7D | 30D | ALL: áp dụng cho hoa hồng, tiền sàn bù và dòng tiền */
    String period;
    LocalDateTime from;

    // 1. Tiền đang giữ hộ đối tác (hiện tại, không theo kỳ)
    BigDecimal shopHeld;
    BigDecimal shipperHeld;
    int shopHeldOrders;
    int shipperHeldOrders;
    /** Số đơn khác nhau đang có tiền tạm giữ cho Shop hoặc Shipper */
    int heldOrders;

    // 2. Hoa hồng trong kỳ
    BigDecimal farmerFee;
    BigDecimal shipperFee;
    BigDecimal commissionTotal;

    // 3. Ví sàn
    BigDecimal platformBalance;
    BigDecimal platformHeld;
    /** Tiền sàn bù voucher giảm tiền hàng + freeship trong kỳ (số dương) */
    BigDecimal voucherSubsidy;
    BigDecimal freeshipSubsidy;

    // 4. COD shipper chưa nộp (hiện tại)
    BigDecimal codPendingAmount;
    int codPendingOrders;
    int codPendingShippers;
    LocalDateTime codOldestCollectedAt;

    // 5. Cần hoàn tiền cho khách (hiện tại)
    BigDecimal refundAmount;
    int refundOrders;

    // Ô phụ
    BigDecimal shopAvailable;
    BigDecimal shipperAvailable;
    /** Tổng tiền khách trả của đơn hoàn tất (chia tiền) trong kỳ */
    BigDecimal settledGmv;
    int settledOrders;

    List<Partner> partners;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Partner {
        int userId;
        int walletId;
        String name;
        /** SHOP | SHIPPER */
        String role;
        BigDecimal held;
        BigDecimal balance;
        int pendingOrders;
        BigDecimal codHolding;
    }
}
