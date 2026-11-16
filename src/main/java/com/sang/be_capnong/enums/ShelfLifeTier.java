package com.sang.be_capnong.enums;

/**
 * Mức bảo quản lô hàng (nhà vườn tự khai khi đăng ký chiến dịch).
 * Số ngày tính từ NGÀY CHỐT chiến dịch: vd chốt ngày 20 thì mức 1 còn tươi tới ngày 22–23,
 * mức 2 tới 23–24, mức 3 tới 25–26.
 */
public enum ShelfLifeTier {
    /** Mức 1: 2–3 ngày sau ngày chốt, hàng dễ dập/héo, giao gấp */
    DAYS_2_3(2, 3),
    /** Mức 2: 3–4 ngày sau ngày chốt */
    DAYS_3_4(3, 4),
    /** Mức 3: 5–6 ngày sau ngày chốt */
    DAYS_5_6(5, 6);

    private final int minDays;
    private final int maxDays;

    ShelfLifeTier(int minDays, int maxDays) {
        this.minDays = minDays;
        this.maxDays = maxDays;
    }

    /** Số ngày chắc chắn còn tươi kể từ ngày chốt */
    public int getMinDays() {
        return minDays;
    }

    /** Số ngày tối đa còn tươi kể từ ngày chốt */
    public int getMaxDays() {
        return maxDays;
    }
}
