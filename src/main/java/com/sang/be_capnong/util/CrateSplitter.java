package com.sang.be_capnong.util;

import com.sang.be_capnong.enums.CrateSize;

import java.util.Optional;

/**
 * Chia số kg người mua đặt thành thùng chuẩn: dùng thùng lớn nhất (15, 10, 5 kg) mà số kg chia hết,
 * để ít thùng nhất khi mỗi đơn chỉ một cỡ thùng. Số kg phải là bội số của 5.
 */
public final class CrateSplitter {

    private static final CrateSize[] SIZES = {CrateSize.KG_15, CrateSize.KG_10, CrateSize.KG_5};

    public record Split(CrateSize size, int quantity) {
    }

    private CrateSplitter() {
    }

    public static Optional<Split> split(int totalKg) {
        if (totalKg < CrateSize.KG_5.getKg() || totalKg % CrateSize.KG_5.getKg() != 0) return Optional.empty();
        for (CrateSize size : SIZES) {
            if (totalKg % size.getKg() == 0) return Optional.of(new Split(size, totalKg / size.getKg()));
        }
        return Optional.empty();
    }
}
