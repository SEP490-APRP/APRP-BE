package com.sang.be_capnong.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.*;

@Getter
@RequiredArgsConstructor
public enum AgriculturalProduceLabel {

    // === NHÓM DANH MỤC CHUNG & MÙA VỤ ===
    PLANT("PLANT", "Cây trồng / Thực vật"),
    VEGETABLE("VEGETABLE", "Rau củ"),
    FRUIT("FRUIT", "Trái cây / Hoa quả"),
    PRODUCE("PRODUCE", "Nông sản tươi"),
    CROP("CROP", "Mùa vụ nông nghiệp"),
    AGRICULTURE("AGRICULTURE", "Nông nghiệp"),
    FLORA("FLORA", "Thực vật"),
    HARVEST("HARVEST", "Nông sản thu hoạch"),
    HERB("HERB", "Rau thơm / Thảo mộc"),
    SEED("SEED", "Hạt giống"),
    ROOT_VEGETABLE("ROOT VEGETABLE", "Củ / Rau ăn củ"),
    LEAF_VEGETABLE("LEAF VEGETABLE", "Rau ăn lá"),
    CITRUS("CITRUS", "Trái cây có múi"),
    BERRY("BERRY", "Quả mọng"),
    LEGUME("LEGUME", "Đậu / Cây họ đậu"),
    GOURD("GOURD", "Bầu / Bí"),
    SQUASH("SQUASH", "Quả bí"),
    GRAIN("GRAIN", "Ngũ cốc"),
    TUBER("TUBER", "Củ / Khoai"),
    FUNGUS("FUNGUS", "Họ nấm"),
    MUSHROOM("MUSHROOM", "Nấm ăn"),

    // === CÁC LOẠI RAU CỦ & GIA VỊ ===
    CABBAGE("CABBAGE", "Bắp cải"),
    CARROT("CARROT", "Cà rốt"),
    POTATO("POTATO", "Khoai tây"),
    SWEET_POTATO("SWEET POTATO", "Khoai lang"),
    ONION("ONION", "Hành tây"),
    GARLIC("GARLIC", "Tỏi"),
    TOMATO("TOMATO", "Cà chua"),
    CUCUMBER("CUCUMBER", "Dưa leo"),
    LETTUCE("LETTUCE", "Xà lách"),
    SPINACH("SPINACH", "Rau chân vịt / Cải bó xôi"),
    BROCCOLI("BROCCOLI", "Bông cải xanh"),
    CAULIFLOWER("CAULIFLOWER", "Súp lơ trắng"),
    PUMPKIN("PUMPKIN", "Bí đỏ"),
    PEPPER("PEPPER", "Ớt ngọt / Tiêu"),
    CHILI("CHILI", "Ớt cay"),
    CORN("CORN", "Bắp / Ngô"),
    MAIZE("MAIZE", "Bắp ngô"),
    RICE("RICE", "Lúa / Gạo"),
    WHEAT("WHEAT", "Lúa mì"),
    PEAS("PEAS", "Đậu Hà Lan"),
    BEANS("BEANS", "Đậu que / Đậu cô ve"),
    PEANUT("PEANUT", "Đậu phộng / Lạc"),

    // === CÁC LOẠI TRÁI CÂY / HOA QUẢ ===
    BANANA("BANANA", "Chuối"),
    APPLE("APPLE", "Táo"),
    ORANGE("ORANGE", "Cam"),
    MANGO("MANGO", "Xoài"),
    WATERMELON("WATERMELON", "Dưa hấu"),
    PINEAPPLE("PINEAPPLE", "Dứa / Thơm"),
    DRAGON_FRUIT("DRAGON FRUIT", "Thanh long"),
    DURIAN("DURIAN", "Sầu riêng"),
    AVOCADO("AVOCADO", "Bơ"),
    PAPAYA("PAPAYA", "Đu đủ"),
    GUAVA("GUAVA", "Ổi"),
    LYCHEE("LYCHEE", "Vải thiều"),
    LONGAN("LONGAN", "Nhãn"),
    RAMBUTAN("RAMBUTAN", "Chôm chôm"),
    JACKFRUIT("JACKFRUIT", "Mít"),
    POMELO("POMELO", "Bưởi"),
    LIME("LIME", "Chanh xanh"),
    LEMON("LEMON", "Chanh vàng"),
    GRAPE("GRAPE", "Nho"),
    STRAWBERRY("STRAWBERRY", "Dâu tây"),
    PEACH("PEACH", "Đào"),
    PLUM("PLUM", "Mận");

    private final String awsLabel;
    private final String vietnameseName;

    private static final Map<String, AgriculturalProduceLabel> AWS_LABEL_MAP = new HashMap<>();

    static {
        for (AgriculturalProduceLabel label : values()) {
            AWS_LABEL_MAP.put(label.awsLabel.toUpperCase(), label);
        }
    }

    public static boolean isAgricultural(String labelName) {
        if (labelName == null || labelName.isBlank()) {
            return false;
        }
        return AWS_LABEL_MAP.containsKey(labelName.trim().toUpperCase());
    }

    public static Optional<AgriculturalProduceLabel> findByAwsLabel(String labelName) {
        if (labelName == null || labelName.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(AWS_LABEL_MAP.get(labelName.trim().toUpperCase()));
    }

    public static String getVietnameseNameOrDefault(String labelName) {
        return findByAwsLabel(labelName)
                .map(AgriculturalProduceLabel::getVietnameseName)
                .orElse(labelName);
    }
}
