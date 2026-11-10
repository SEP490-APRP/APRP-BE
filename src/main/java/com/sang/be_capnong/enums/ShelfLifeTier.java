package com.sang.be_capnong.enums;

/** Thời gian bảo quản lô hàng sau thu hoạch (nhà vườn tự khai khi đăng ký chiến dịch) */
public enum ShelfLifeTier {
    /** 1-2 ngày: hàng dễ dập/héo, cần giao ngay */
    DAYS_1_2(2),
    /** 3-4 ngày */
    DAYS_3_4(4),
    /** 5-6 ngày */
    DAYS_5_6(6);

    private final int maxDays;

    ShelfLifeTier(int maxDays) {
        this.maxDays = maxDays;
    }

    /** Số ngày tối đa hàng còn tươi kể từ ngày thu hoạch */
    public int getMaxDays() {
        return maxDays;
    }
}
