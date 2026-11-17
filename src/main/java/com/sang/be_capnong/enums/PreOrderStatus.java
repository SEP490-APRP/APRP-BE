package com.sang.be_capnong.enums;

/** PreOrder: PENDING -> DEPOSIT_PAID -> PROCESSING -> READY -> SHIPPING -> DELIVERED (+ CANCELLED / FAILED) */
public enum PreOrderStatus {
    /** Đã tạo, chờ thanh toán cọc PayOS */
    PENDING,
    DEPOSIT_PAID,
    /** Đã đóng gói + dán tem QR tại điểm tập kết */
    PROCESSING,
    /** Đã tới hub, chờ shipper (READY_FOR_PICKUP) */
    READY,
    /** Shipper đã nhận hàng khỏi hub và đang giao tới người mua */
    SHIPPING,
    DELIVERED,
    /** Huỷ: quá hạn cọc, người mua huỷ khi chưa cọc, hoặc chiến dịch bị huỷ */
    CANCELLED,
    /** Giao thất bại, hàng quay về hub */
    FAILED
}
