package com.sang.be_capnong.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Getter
@RequiredArgsConstructor
public enum PackagingLabel {

    // === HỘP ĐỰNG & BAO BÌ QUÀ TẶNG / TÚI MÙ ===
    BOX("BOX", "Hộp đựng"),
    PACKAGING("PACKAGING", "Bao bì đóng gói"),
    CARDBOARD("CARDBOARD", "Bìa carton"),
    PACKAGE("PACKAGE", "Gói kiện hàng"),
    GIFT("GIFT", "Hộp quà biếu tặng"),
    BASKET("BASKET", "Giỏ hàng / Lẵng quả"),
    HAMPER("HAMPER", "Giỏ quà tết"),
    PAPER("PAPER", "Túi giấy / Giấy gói"),
    BAG("BAG", "Túi đựng"),
    CONTAINER("CONTAINER", "Hộp chứa / Khay đựng"),
    CRATE("CRATE", "Thùng gỗ / Sọt nhựa"),
    WRAPPING("WRAPPING", "Màng bọc / Giấy bọc"),
    PRESENT("PRESENT", "Quà biếu tặng"),
    PARCEL("PARCEL", "Bưu kiện đóng gói"),
    CARDBOARD_BOX("CARDBOARD BOX", "Thùng carton"),
    CARTON("CARTON", "Hộp carton"),
    GIFT_BASKET("GIFT BASKET", "Giỏ quà tặng");

    private final String label;
    private final String vietnameseName;

    private static final Map<String, PackagingLabel> LABEL_MAP = new HashMap<>();

    static {
        for (PackagingLabel item : values()) {
            LABEL_MAP.put(item.label.toUpperCase(), item);
        }
    }

    public static boolean isPackaging(String labelName) {
        if (labelName == null || labelName.isBlank()) {
            return false;
        }
        return LABEL_MAP.containsKey(labelName.trim().toUpperCase());
    }

    public static Optional<PackagingLabel> findByLabel(String labelName) {
        if (labelName == null || labelName.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(LABEL_MAP.get(labelName.trim().toUpperCase()));
    }

    public static String getVietnameseNameOrDefault(String labelName) {
        return findByLabel(labelName)
                .map(PackagingLabel::getVietnameseName)
                .orElse(labelName);
    }
}
