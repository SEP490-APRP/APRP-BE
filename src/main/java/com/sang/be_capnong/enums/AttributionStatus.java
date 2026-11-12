package com.sang.be_capnong.enums;

/**
 * Trạng thái ghi nhận tiếp thị liên kết (Referral Attribution):
 * - ACTIVE: Đang có hiệu lực trong cửa sổ 7 ngày và chưa chạm trần đơn chuyển đổi (convertedOrderCount < maxConversions)
 * - EXHAUSTED: Đã đạt trần tối đa số đơn chuyển đổi (ví dụ: 3 đơn)
 * - EXPIRED: Đã hết hạn thời gian (sau 7 ngày kể từ lúc nhấp link)
 */
public enum AttributionStatus {
    ACTIVE,
    EXHAUSTED,
    EXPIRED
}

