package com.sang.be_capnong.enums;

public enum ReturnStatus {
    PENDING,            // Người mua gửi yêu cầu, chờ shop phản hồi
    SHOP_APPROVED,      // Shop đồng ý yêu cầu
    DISPUTED,           // Shop từ chối / có tranh chấp, chuyển lên Admin xử lý
    ADMIN_APPROVED,     // Admin duyệt yêu cầu
    APPROVED,           // Trạng thái duyệt chung (tương thích ngược)
    RETURN_SHIPPING,    // Chờ shipper nhận đơn / đang vận chuyển hoàn hàng
    RETURN_PICKED_UP,   // Shipper đã lấy hàng hoàn từ người mua
    RETURN_DELIVERED,   // Shipper đã giao hàng hoàn trả về cho shop
    RETURN_FAILED,      // Shipper giao nhận hàng hoàn thất bại
    COMPLETED,          // Hoàn tất (hàng đã trả về nếu cần, hoàn tiền hoàn tất)
    REJECTED,           // Từ chối hoàn trả
    CANCELLED           // Người mua tự hủy yêu cầu
}
