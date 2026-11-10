package com.sang.be_capnong.enums;

import java.math.BigDecimal;

/** Thùng chuẩn của chiến dịch (GAP-03): chỉ bán 10 / 20 / 50 kg, không bán lẻ */
public enum CrateSize {
    KG_10(10),
    KG_20(20),
    KG_50(50);

    private final int kg;

    CrateSize(int kg) {
        this.kg = kg;
    }

    public int getKg() {
        return kg;
    }

    public BigDecimal kgDecimal() {
        return BigDecimal.valueOf(kg);
    }
}
