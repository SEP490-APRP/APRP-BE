package com.sang.be_capnong.enums;

/**
 * Trạng thái hoa hồng tiếp thị liên kết:
 * - PENDING: Đang chờ đơn hàng hoàn tất đối soát (sau khi đơn DELIVERED 24h hoặc COMPLETED)
 * - COMPLETED: Đã quyết toán cộng tiền thành công vào ví TYPE_BUYER của người giới thiệu
 * - CANCELLED: Đơn hàng bị huỷ hoặc thanh toán PayOS hết hạn/thất bại
 */
public enum CommissionStatus {
    PENDING,
    COMPLETED,
    CANCELLED
}

