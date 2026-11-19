package com.sang.be_capnong.enums;

public enum FarmRegistrationStatus {
    PENDING,
    APPROVED,
    REJECTED,
    /** Chiến dịch bị huỷ (admin huỷ hoặc không đủ ngưỡng) nên lô đăng ký cũng huỷ theo */
    CANCELLED
}
