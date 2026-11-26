package com.sang.be_capnong.enums;

/** Chuyến xe chở hàng từ điểm tập kết ra hub: OPEN -> ACCEPTED -> IN_TRANSIT -> DELIVERED (+ CANCELLED) */
public enum HubTripStatus {
    /** Chưa có tài xế nhận */
    OPEN,
    /** Đã có tài xế nhận (hoặc admin chỉ định) */
    ACCEPTED,
    /** Tài xế đã lấy hàng tại điểm tập kết, đang chở ra hub */
    IN_TRANSIT,
    /** Đã giao hàng tới hub */
    DELIVERED,
    /** Kế hoạch đổi nên chuyến không còn */
    CANCELLED
}
