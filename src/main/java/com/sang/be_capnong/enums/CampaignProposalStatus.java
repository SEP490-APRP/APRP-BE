package com.sang.be_capnong.enums;

/**
 * Đơn shop đề xuất mở chiến dịch giải cứu.
 * DRAFT -> SUBMITTED -> APPROVED (đã tạo chiến dịch) | REJECTED; SUBMITTED -> NEEDS_CHANGES -> SUBMITTED; shop rút: WITHDRAWN.
 */
public enum CampaignProposalStatus {
    /** Shop đang soạn và mời shop lân cận */
    DRAFT,
    /** Đã gửi, chờ admin duyệt */
    SUBMITTED,
    /** Admin trả về để shop sửa rồi gửi lại */
    NEEDS_CHANGES,
    /** Admin duyệt: đã tạo chiến dịch (có thể gộp nhiều đơn vào 1 chiến dịch) */
    APPROVED,
    REJECTED,
    /** Shop khởi xướng rút đơn */
    WITHDRAWN;

    public boolean open() {
        return this == DRAFT || this == SUBMITTED || this == NEEDS_CHANGES;
    }

    public boolean editable() {
        return this == DRAFT || this == NEEDS_CHANGES;
    }
}
