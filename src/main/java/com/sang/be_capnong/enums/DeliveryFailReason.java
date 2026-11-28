package com.sang.be_capnong.enums;

/** Lý do Shipper báo giao hàng thất bại */
public enum DeliveryFailReason {
    /** Gọi khách nhiều lần không nghe máy (bắt buộc ghi số cuộc gọi, tối thiểu 3) */
    KHONG_LIEN_LAC_DUOC,
    /** Địa chỉ nhận hàng sai hoặc không tìm thấy */
    SAI_DIA_CHI,
    /** Khách từ chối nhận hàng */
    KHACH_TU_CHOI,
    /** Nhà vườn đóng cửa hoặc không có người bàn giao hàng */
    SHOP_DONG_CUA,
    /** Nhà vườn chưa đóng gói xong hoặc thiếu hàng */
    SHOP_CHUA_SAN_SANG,
    /** Lý do khác (bắt buộc ghi chú) */
    KHAC
}
