package com.sang.be_capnong.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Getter
@RequiredArgsConstructor
public enum NonAgriculturalItem {

    // === THỨC ĂN NHANH & MÓN ĂN CHẾ BIẾN SẴN ===
    FAST_FOOD("FAST FOOD", "Thức ăn nhanh"),
    BURGER("BURGER", "Bánh kẹp thịt / Burger"),
    HAMBURGER("HAMBURGER", "Bánh kẹp thịt / Hamburger"),
    PIZZA("PIZZA", "Bánh Pizza"),
    HOT_DOG("HOT DOG", "Bánh mì kẹp xúc xích / Hot dog"),
    SANDWICH("SANDWICH", "Bánh mì kẹp / Sandwich"),
    FRENCH_FRIES("FRENCH FRIES", "Khoai tây chiên"),

    // === THỊT & HẢI SẢN CHẾ BIẾN / SỐNG ===
    MEAT("MEAT", "Thịt gia súc"),
    PORK("PORK", "Thịt lợn / Thịt heo"),
    BEEF("BEEF", "Thịt bò"),
    POULTRY("POULTRY", "Thịt gia cầm"),
    STEAK("STEAK", "Bít tết"),
    SEAFOOD("SEAFOOD", "Hải sản"),
    FISH("FISH", "Cá / Thủy hải sản"),

    // === ĐỒ UỐNG PHA SẴN & ĐỒ UỐNG CÓ CỒN ===
    BEVERAGE("BEVERAGE", "Đồ uống / Nước giải khát"),
    ALCOHOL("ALCOHOL", "Đồ uống có cồn"),
    BEER("BEER", "Bia"),
    WINE("WINE", "Rượu vang"),
    COCKTAIL("COCKTAIL", "Cocktail"),
    COFFEE("COFFEE", "Cà phê pha sẵn");

    private final String label;
    private final String vietnameseName;

    private static final Map<String, NonAgriculturalItem> LABEL_MAP = new HashMap<>();

    static {
        for (NonAgriculturalItem item : values()) {
            LABEL_MAP.put(item.label.toUpperCase(), item);
        }
    }

    public boolean isAlcoholic() {
        return this == ALCOHOL || this == BEER || this == WINE || this == COCKTAIL;
    }

    public static boolean isAlcoholLabel(String labelName) {
        return findByLabel(labelName).map(NonAgriculturalItem::isAlcoholic).orElse(false);
    }

    public static boolean isNonAgricultural(String labelName) {
        if (labelName == null || labelName.isBlank()) {
            return false;
        }
        return LABEL_MAP.containsKey(labelName.trim().toUpperCase());
    }

    public static Optional<NonAgriculturalItem> findByLabel(String labelName) {
        if (labelName == null || labelName.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(LABEL_MAP.get(labelName.trim().toUpperCase()));
    }


    public static String getVietnameseNameOrDefault(String labelName) {
        return findByLabel(labelName)
                .map(NonAgriculturalItem::getVietnameseName)
                .orElse(labelName);
    }
}
