package com.sang.be_capnong.enums;

/**
 * Vòng đời chiến dịch giải cứu (Review 2 - WF-01).
 * DRAFT -> ACTIVE -> THRESHOLD_MET -> FULFILLING -> COMPLETED; ngoại lệ: ACTIVE -> CANCELLED (thiếu ngưỡng lúc chốt).
 */
public enum CampaignStatus {
    /** Admin đang soạn; có thể đã đăng lời kêu gọi để nhà vườn đăng ký lô hàng */
    DRAFT,
    /** Đã mở bán cho người mua đặt trước + cọc */
    ACTIVE,
    /** Đến ngày chốt và đủ ngưỡng sản lượng; đã phân bổ sản lượng cho từng nhà vườn */
    THRESHOLD_MET,
    /** Đang gom hàng tại điểm tập kết, cân, đóng gói QR, chở tới hub */
    FULFILLING,
    COMPLETED,
    CANCELLED
}
