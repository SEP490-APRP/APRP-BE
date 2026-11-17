package com.sang.be_capnong.enums;

/** Deposit: PENDING -> HELD_IN_ESCROW -> RELEASED_TO_FARMER (ngoại lệ: -> REFUNDED, hoặc CANCELLED nếu chưa trả) */
public enum CampaignDepositStatus {
    PENDING,
    HELD_IN_ESCROW,
    RELEASED_TO_FARMER,
    REFUNDED,
    /** Link cọc chưa thanh toán đã bị huỷ (không có tiền để hoàn) */
    CANCELLED
}
