package com.sang.be_capnong.enums;

import java.math.BigDecimal;

/**
 * Thùng chuẩn của chiến dịch. Người mua chỉ được đặt thùng 5 / 10 / 15 kg (bội số của 5), xem {@link #BUYER_SIZES}.
 * 3 / 20 / 50 kg giữ lại để đọc đơn cũ.
 */
public enum CrateSize {
    KG_3(3),
    KG_5(5),
    KG_10(10),
    KG_15(15),
    KG_20(20),
    KG_50(50);

    /** Loại thùng người mua được chọn khi đặt trước */
    public static final java.util.Set<CrateSize> BUYER_SIZES = java.util.EnumSet.of(KG_5, KG_10, KG_15);

    private final int kg;

    CrateSize(int kg) {
        this.kg = kg;
    }

    public int getKg() {
        return kg;
    }

    public boolean buyerSelectable() {
        return BUYER_SIZES.contains(this);
    }

    public BigDecimal kgDecimal() {
        return BigDecimal.valueOf(kg);
    }
}
