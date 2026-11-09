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
    PLUM("PLUM", "Mận"),

    // === NÔNG SẢN PHỔ BIẾN BỔ SUNG ===
    BITTER_MELON("BITTER MELON", "Khổ qua"),
    WATER_SPINACH("WATER SPINACH", "Rau muống"),
    CANTALOUPE("CANTALOUPE", "Dưa lưới"),
    EGGPLANT("EGGPLANT", "Cà tím"),
    BELL_PEPPER("BELL PEPPER", "Ớt chuông"),
    ASPARAGUS("ASPARAGUS", "Măng tây"),
    CUSTARD_APPLE("CUSTARD APPLE", "Mãng cầu"),
    SAPODILLA("SAPODILLA", "Hồng xiêm"),
    PASSION_FRUIT("PASSION FRUIT", "Chanh dây");

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
     * Trả về từ khóa cần loại trừ khi tìm kiếm để tránh hiện tượng trùng lặp từ khóa con
     * (Ví dụ: Tìm Bắp / Ngô thì loại trừ "bắp cải" để không lẫn lộn với Cabbage).
     */
    public String getExcludeKeyword() {
        if (this == CORN || this == MAIZE) {
            return "bắp cải";
        }
        return null;
    }

    /**
     * Trích xuất loại nông sản cụ thể từ tên sản phẩm tiếng Việt hoặc tiếng Anh.
     */
    public static Optional<AgriculturalProduceLabel> detectExpectedProduce(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        String lower = text.toLowerCase(Locale.ROOT);

        // Thứ tự ưu tiên từ dài đến ngắn để tránh nhận diện nhầm từ con (Hỗ trợ cả tiếng Việt và tiếng Anh)
        if (lower.contains("dưa hấu") || lower.contains("watermelon") || lower.contains("citrullus")) return Optional.of(WATERMELON);
        if (lower.contains("thanh long") || lower.contains("dragon fruit") || lower.contains("pitaya")) return Optional.of(DRAGON_FRUIT);
        if (lower.contains("sầu riêng") || lower.contains("durian")) return Optional.of(DURIAN);
        if (lower.contains("khoai lang") || lower.contains("sweet potato")) return Optional.of(SWEET_POTATO);
        if (lower.contains("khoai tây") || lower.contains("potato")) return Optional.of(POTATO);
        if (lower.contains("bắp cải") || lower.contains("cabbage")) return Optional.of(CABBAGE);
        if (lower.contains("bông cải") || lower.contains("súp lơ xanh") || lower.contains("broccoli")) return Optional.of(BROCCOLI);
        if (lower.contains("súp lơ") || lower.contains("cauliflower")) return Optional.of(CAULIFLOWER);
        if (lower.contains("chôm chôm") || lower.contains("rambutan")) return Optional.of(RAMBUTAN);
        if (lower.contains("dâu tây") || lower.contains("strawberry")) return Optional.of(STRAWBERRY);
        if (lower.contains("dưa leo") || lower.contains("dưa chuột") || lower.contains("cucumber")) return Optional.of(CUCUMBER);
        if (lower.contains("dưa lưới") || lower.contains("cantaloupe") || lower.contains("muskmelon")) return Optional.of(CANTALOUPE);
        if (lower.contains("khổ qua") || lower.contains("mướp đắng") || lower.contains("bitter melon") || lower.contains("bitter gourd")) return Optional.of(BITTER_MELON);
        if (lower.contains("cà chua") || lower.contains("tomato")) return Optional.of(TOMATO);
        if (lower.contains("cà rốt") || lower.contains("carrot")) return Optional.of(CARROT);
        if (lower.contains("cà tím") || lower.contains("eggplant") || lower.contains("aubergine")) return Optional.of(EGGPLANT);
        if (lower.contains("ớt chuông") || lower.contains("bell pepper") || lower.contains("sweet pepper")) return Optional.of(BELL_PEPPER);
        if (lower.contains("măng tây") || lower.contains("asparagus")) return Optional.of(ASPARAGUS);
        if (lower.contains("rau muống") || lower.contains("water spinach") || lower.contains("morning glory")) return Optional.of(WATER_SPINACH);
        if (lower.contains("chanh dây") || lower.contains("chanh leo") || lower.contains("passion fruit")) return Optional.of(PASSION_FRUIT);
        if (lower.contains("mãng cầu") || lower.contains("trái na") || lower.contains("quả na") || lower.contains("custard apple") || lower.contains("sugar apple")) return Optional.of(CUSTARD_APPLE);
        if (lower.contains("hồng xiêm") || lower.contains("sa pô chê") || lower.contains("sapodilla")) return Optional.of(SAPODILLA);
        if (lower.contains("bí đỏ") || lower.contains("pumpkin") || lower.contains("squash")) return Optional.of(PUMPKIN);
        if (lower.contains("xà lách") || lower.contains("lettuce") || lower.contains("salad greens")) return Optional.of(LETTUCE);
        if (lower.contains("rau chân vịt") || lower.contains("cải bó xôi") || lower.contains("spinach")) return Optional.of(SPINACH);
        if (lower.contains("bơ sáp") || lower.contains("quả bơ") || lower.contains("trái bơ") || lower.contains("avocado")) return Optional.of(AVOCADO);
        if (lower.contains("chuối") || lower.contains("banana")) return Optional.of(BANANA);
        if (lower.contains("xoài") || lower.contains("mango")) return Optional.of(MANGO);
        if (lower.contains("dứa") || lower.contains("thơm") || lower.contains("khóm") || lower.contains("pineapple")) return Optional.of(PINEAPPLE);
        if (lower.contains("đu đủ") || lower.contains("papaya")) return Optional.of(PAPAYA);
        if (lower.contains("ổi") || lower.contains("guava")) return Optional.of(GUAVA);
        if (lower.contains("vải") || lower.contains("lychee")) return Optional.of(LYCHEE);
        if (lower.contains("nhãn") || lower.contains("longan")) return Optional.of(LONGAN);
        if (lower.contains("mít") || lower.contains("jackfruit")) return Optional.of(JACKFRUIT);
        if (lower.contains("bưởi") || lower.contains("pomelo") || lower.contains("grapefruit")) return Optional.of(POMELO);
        if (lower.contains("chanh") || lower.contains("lime") || lower.contains("lemon")) return Optional.of(LIME);
        if (lower.contains("táo") || lower.contains("apple")) return Optional.of(APPLE);
        if (lower.contains("cam") || lower.contains("orange")) return Optional.of(ORANGE);
        if (lower.contains("nho") || lower.contains("grape")) return Optional.of(GRAPE);
        if (lower.contains("mận") || lower.contains("plum")) return Optional.of(PLUM);
        if (lower.contains("đào") || lower.contains("peach")) return Optional.of(PEACH);
        if (lower.contains("hành tây") || lower.contains("onion")) return Optional.of(ONION);
        if (lower.contains("tỏi") || lower.contains("garlic")) return Optional.of(GARLIC);
        if (lower.contains("ớt") || lower.contains("chili") || lower.contains("pepper")) return Optional.of(CHILI);
        if (lower.contains("bắp") || lower.contains("ngô") || lower.contains("corn") || lower.contains("maize")) return Optional.of(CORN);
        if (lower.contains("nấm") || lower.contains("mushroom")) return Optional.of(MUSHROOM);
        if (lower.contains("đậu phộng") || lower.contains("lạc") || lower.contains("peanut")) return Optional.of(PEANUT);

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

