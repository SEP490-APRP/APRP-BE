package com.sang.be_capnong.enums;

/** Trạng thái lời mời / thành viên của đơn đề xuất chiến dịch */
public enum ProposalMemberStatus {
    /** Đã mời, chờ shop trả lời (hết hạn sau 72 giờ) */
    PENDING,
    ACCEPTED,
    DECLINED,
    /** Quá 72 giờ, hoặc đơn đã được duyệt / từ chối / rút khi còn chờ */
    EXPIRED,
    /** Shop khởi xướng gỡ lời mời khi đơn còn nháp */
    REMOVED
}
