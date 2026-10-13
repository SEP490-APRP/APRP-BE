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
    FOOD("FOOD", "Thực phẩm / Nông sản"),
    NATURAL_FOODS("NATURAL FOODS", "Nông sản tươi tự nhiên"),
    WHOLE_FOOD("WHOLE FOOD", "Nông sản nguyên bản"),
    LOCAL_FOOD("LOCAL FOOD", "Nông sản địa phương"),
    SUPERFOOD("SUPERFOOD", "Nông sản dinh dưỡng cao"),
    VEGETARIAN_FOOD("VEGETARIAN FOOD", "Thực phẩm thực vật"),
    INGREDIENT("INGREDIENT", "Nguyên liệu tươi"),
    STAPLE_FOOD("STAPLE FOOD", "Lương thực chính"),
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

    private final String label;
    private final String vietnameseName;

    private static final Map<String, AgriculturalProduceLabel> LABEL_MAP = new HashMap<>();

    static {
        for (AgriculturalProduceLabel item : values()) {
            LABEL_MAP.put(item.label.toUpperCase(), item);
        }
    }

    public static boolean isAgricultural(String labelName) {
        if (labelName == null || labelName.isBlank()) {
            return false;
        }
        return LABEL_MAP.containsKey(labelName.trim().toUpperCase());
    }

    public static Optional<AgriculturalProduceLabel> findByLabel(String labelName) {
        if (labelName == null || labelName.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(LABEL_MAP.get(labelName.trim().toUpperCase()));
    }


    public static String getVietnameseNameOrDefault(String labelName) {
        return findByLabel(labelName)
                .map(AgriculturalProduceLabel::getVietnameseName)
                .orElse(labelName);
    }

    /**
     * Xác định xem một nhãn có phải là loại nông sản cụ thể (không phải nhãn nhóm chung).
     */
    public boolean isSpecificProduce() {
        return switch (this) {
            case PLANT, VEGETABLE, FRUIT, PRODUCE, FOOD, NATURAL_FOODS, WHOLE_FOOD,
                 LOCAL_FOOD, SUPERFOOD, VEGETARIAN_FOOD, INGREDIENT, STAPLE_FOOD,
                 CROP, AGRICULTURE, FLORA, HARVEST, HERB, SEED, ROOT_VEGETABLE,
                 LEAF_VEGETABLE, CITRUS, BERRY, LEGUME, GOURD, SQUASH, GRAIN, TUBER, FUNGUS -> false;
            default -> true;
        };
    }

    /**
     * Trích xuất loại nông sản cụ thể từ tên sản phẩm tiếng Việt (nếu có).
     */
    public static Optional<AgriculturalProduceLabel> detectExpectedProduce(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        String lower = text.toLowerCase(Locale.ROOT);

        // Thứ tự ưu tiên từ dài đến ngắn để tránh nhận diện nhầm từ con
        if (lower.contains("dưa hấu")) return Optional.of(WATERMELON);
        if (lower.contains("thanh long")) return Optional.of(DRAGON_FRUIT);
        if (lower.contains("sầu riêng")) return Optional.of(DURIAN);
        if (lower.contains("khoai lang")) return Optional.of(SWEET_POTATO);
        if (lower.contains("khoai tây")) return Optional.of(POTATO);
        if (lower.contains("bắp cải")) return Optional.of(CABBAGE);
        if (lower.contains("bông cải") || lower.contains("súp lơ xanh")) return Optional.of(BROCCOLI);
        if (lower.contains("súp lơ")) return Optional.of(CAULIFLOWER);
        if (lower.contains("chôm chôm")) return Optional.of(RAMBUTAN);
        if (lower.contains("dâu tây")) return Optional.of(STRAWBERRY);
        if (lower.contains("dưa leo") || lower.contains("dưa chuột")) return Optional.of(CUCUMBER);
        if (lower.contains("cà chua")) return Optional.of(TOMATO);
        if (lower.contains("cà rốt")) return Optional.of(CARROT);
        if (lower.contains("bí đỏ")) return Optional.of(PUMPKIN);
        if (lower.contains("xà lách")) return Optional.of(LETTUCE);
        if (lower.contains("rau chân vịt") || lower.contains("cải bó xôi")) return Optional.of(SPINACH);
        if (lower.contains("bơ")) return Optional.of(AVOCADO);
        if (lower.contains("chuối")) return Optional.of(BANANA);
        if (lower.contains("xoài")) return Optional.of(MANGO);
        if (lower.contains("dứa") || lower.contains("thơm") || lower.contains("khóm")) return Optional.of(PINEAPPLE);
        if (lower.contains("đu đủ")) return Optional.of(PAPAYA);
        if (lower.contains("ổi")) return Optional.of(GUAVA);
        if (lower.contains("vải")) return Optional.of(LYCHEE);
        if (lower.contains("nhãn")) return Optional.of(LONGAN);
        if (lower.contains("mít")) return Optional.of(JACKFRUIT);
        if (lower.contains("bưởi")) return Optional.of(POMELO);
        if (lower.contains("chanh")) return Optional.of(LIME);
        if (lower.contains("táo")) return Optional.of(APPLE);
        if (lower.contains("cam")) return Optional.of(ORANGE);
        if (lower.contains("nho")) return Optional.of(GRAPE);
        if (lower.contains("mận")) return Optional.of(PLUM);
        if (lower.contains("đào")) return Optional.of(PEACH);
        if (lower.contains("hành tây") || lower.contains("hành lá") || lower.contains("hành củ")) return Optional.of(ONION);
        if (lower.contains("tỏi")) return Optional.of(GARLIC);
        if (lower.contains("ớt")) return Optional.of(CHILI);
        if (lower.contains("bắp") || lower.contains("ngô")) return Optional.of(CORN);
        if (lower.contains("nấm")) return Optional.of(MUSHROOM);
        if (lower.contains("đậu phộng") || lower.contains("lạc")) return Optional.of(PEANUT);

        return Optional.empty();
    }

    /**
     * Kiểm tra xem nhãn Google Vision trả về có phải là đồng loại / họ hàng với loại nông sản mong đợi không.
     */
    public boolean isFamilyCompatibleWith(AgriculturalProduceLabel detected) {
        if (this == detected) return true;
        if (!detected.isSpecificProduce()) return true; // Nhãn nhóm chung luôn tương thích

        // Các nhóm họ hàng gần gũi
        if ((this == WATERMELON && detected == CUCUMBER) || (this == CUCUMBER && detected == WATERMELON)) return true; // Cùng họ bầu bí Cucurbitaceae
        if ((this == CORN && detected == MAIZE) || (this == MAIZE && detected == CORN)) return true;
        if ((this == LIME && detected == LEMON) || (this == LEMON && detected == LIME)) return true;
        if ((this == POTATO && detected == SWEET_POTATO) || (this == SWEET_POTATO && detected == POTATO)) return true;

        return false;
    }
}

