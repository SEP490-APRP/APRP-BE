package com.sang.be_capnong.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Getter
@RequiredArgsConstructor
public enum CookedFoodLabel {

    // === MÓN ĂN & NẤU NƯỚNG THÀNH PHẨM ===
    DISH("DISH", "Món ăn"),
    CUISINE("CUISINE", "Ẩm thực"),
    SOUP("SOUP", "Món canh / Súp"),
    RECIPE("RECIPE", "Công thức món ăn"),
    MEAL("MEAL", "Bữa ăn"),
    FOOD("FOOD", "Thực phẩm / Thức ăn"),
    SALAD("SALAD", "Salad trộn"),
    STEW("STEW", "Món hầm / Món kho"),
    HOT_POT("HOT POT", "Món lẩu"),
    COOKING("COOKING", "Nấu nướng / Chế biến"),
    INGREDIENT("INGREDIENT", "Nguyên liệu chế biến"),
    COMFORT_FOOD("COMFORT FOOD", "Món ăn truyền thống"),
    VEGETARIAN_FOOD("VEGETARIAN FOOD", "Món ăn chay"),
    SIDE_DISH("SIDE DISH", "Món ăn kèm"),
    ASIAN_FOOD("ASIAN FOOD", "Món ăn châu Á"),
    VIETNAMESE_CUISINE("VIETNAMESE CUISINE", "Ẩm thực Việt Nam"),
    DELICACY("DELICACY", "Món ăn đặc sản"),
    LUNCH("LUNCH", "Bữa trưa"),
    DINNER("DINNER", "Bữa tối"),
    FRIED_FOOD("FRIED FOOD", "Món chiên / rán"),
    NOODLE("NOODLE", "Mì / Bún / Phở"),
    BROTH("BROTH", "Nước dùng"),
    CURRY("CURRY", "Món cà ri");

    private final String label;
    private final String vietnameseName;

    private static final Map<String, CookedFoodLabel> LABEL_MAP = new HashMap<>();

    static {
        for (CookedFoodLabel item : values()) {
            LABEL_MAP.put(item.label.toUpperCase(), item);
        }
    }

    public static boolean isCookedFood(String labelName) {
        if (labelName == null || labelName.isBlank()) {
            return false;
        }
        return LABEL_MAP.containsKey(labelName.trim().toUpperCase());
    }

    public static Optional<CookedFoodLabel> findByLabel(String labelName) {
        if (labelName == null || labelName.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(LABEL_MAP.get(labelName.trim().toUpperCase()));
    }

    public static String getVietnameseNameOrDefault(String labelName) {
        return findByLabel(labelName)
                .map(CookedFoodLabel::getVietnameseName)
                .orElse(labelName);
    }
}
