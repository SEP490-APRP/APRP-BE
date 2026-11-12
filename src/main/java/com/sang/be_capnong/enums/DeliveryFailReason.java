package com.sang.be_capnong.enums;

/** Lý do Shipper báo giao hàng thất bại */
public enum DeliveryFailReason {
    /** Gọi khách nhiều lần không nghe máy (bắt buộc ghi số cuộc gọi, tối thiểu 3) */
    KHONG_LIEN_LAC_DUOC,
    /** Địa chỉ nhận hàng sai hoặc không tìm thấy */
    SAI_DIA_CHI,
    /** Khách từ chối nhận hàng */
    KHACH_TU_CHOI,
    /** Lý do khác (bắt buộc ghi chú) */
    KHAC
}
