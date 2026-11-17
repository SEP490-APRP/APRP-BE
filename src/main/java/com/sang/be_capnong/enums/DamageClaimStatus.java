package com.sang.be_capnong.enums;

/** Trạng thái yêu cầu báo hàng hư của đơn đặt trước chiến dịch */
public enum DamageClaimStatus {
    /** Người mua đã gửi cân + ảnh, chờ admin xem */
    PENDING,
    /** Admin duyệt, đã hoàn tiền phần hư vào ví người mua (sàn chi trả) */
    APPROVED,
    /** Admin từ chối (ảnh không rõ, không đúng đơn...) */
    REJECTED
}
