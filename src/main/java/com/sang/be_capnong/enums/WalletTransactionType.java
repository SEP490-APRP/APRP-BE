package com.sang.be_capnong.enums;

/**
 * Loại bút toán ghi vào ví khi quyết toán đơn hàng.
 * Số tiền trong WalletTransaction luôn có dấu: dương = cộng ví, âm = trừ ví.
 */
public enum WalletTransactionType {
    /** Shop nhận tiền hàng sau khi trừ voucher shop và phí dịch vụ nông dân */
    SHOP_INCOME,
    /** Shipper nhận cước giao hàng sau khi trừ phí dịch vụ shipper */
    SHIPPER_INCOME,
    /** Sàn thu phí dịch vụ nông dân (% tiền hàng) */
    PLATFORM_FARMER_FEE,
    /** Sàn thu phí dịch vụ shipper (% cước giao hàng) */
    PLATFORM_SHIPPER_FEE,
    /** Sàn bù tiền voucher giảm tiền hàng do sàn tài trợ (âm) */
    PLATFORM_VOUCHER_SUBSIDY,
    /** Sàn bù tiền voucher freeship do sàn tài trợ (âm) */
    PLATFORM_FREESHIP_SUBSIDY,
    /** Shipper đang giữ tiền mặt COD của khách (âm = nợ sàn) */
    COD_CASH_COLLECTED,
    /** Shipper đã nộp tiền mặt COD về sàn (dương, xoá nợ) */
    COD_CASH_REMITTED,

    // ── Tiền tạm giữ (cộng/trừ frozenBalance, không đổi số dư rút được) ──
    /** Shop: tiền hàng tạm giữ ngay khi tiền vào sàn (PayOS đã trả / COD đã thu) */
    SHOP_INCOME_HOLD,
    /** Shipper: cước giao tạm giữ ngay khi nhận đơn */
    SHIPPER_INCOME_HOLD,
    /** Sàn: phần sàn được hưởng, tạm giữ ngay khi tiền vào sàn */
    PLATFORM_INCOME_HOLD,
    /** Đơn COMPLETED: gỡ tiền tạm giữ (âm), tiền thật được cộng bằng các bút toán *_INCOME / PLATFORM_* */
    HOLD_RELEASED,
    /** Đơn huỷ / giao thất bại: gỡ tiền tạm giữ (âm) */
    HOLD_CANCELLED,

    // ── Hoàn hàng / Hoàn tiền ──
    /** Hoàn tiền về ví cho người mua (dương = cộng ví Buyer) */
    BUYER_REFUND,
    /** Trừ tiền tạm giữ hoặc trừ tiền hàng của Shop do hoàn trả (âm) */
    SHOP_REFUND_DEDUCT,
    /** Trừ tiền tạm giữ hoặc điều chỉnh phí của Sàn do hoàn tiền (âm) */
    PLATFORM_REFUND_DEDUCT,
    /** Shop thanh toán cước vận chuyển hoàn trả hàng từ ví (âm) */
    SHOP_RETURN_SHIPPING_DEDUCT,
    /** Hoàn lại cước vận chuyển hoàn trả hàng cho shop do yêu cầu bị huỷ/từ chối (dương) */
    SHOP_RETURN_SHIPPING_REFUND,

    // ── Ký quỹ COD (cộng/trừ wallets.cod_reserved, không đổi số dư) ──
    /** Shipper nhận đơn COD: giữ chỗ số tiền COD trong ví (âm = số dư khả dụng giảm) */
    COD_RESERVED,
    /** Đơn COD giao xong / thất bại / huỷ: gỡ giữ chỗ (dương = số dư khả dụng tăng lại) */
    COD_RESERVE_RELEASED,

    // ── Nạp ví ──
    /** Nạp tiền vào ví qua PayOS (dương) */
    TOP_UP,

    // ── Rút tiền ──
    /** Tạo lệnh rút: trừ số dư ngay (âm) để không rút trùng */
    WITHDRAW_REQUESTED,
    /** Admin từ chối lệnh rút: trả lại số dư (dương) */
    WITHDRAW_REFUNDED
}
