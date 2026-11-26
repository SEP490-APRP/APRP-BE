package com.sang.be_capnong.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.text.Normalizer;
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
    MELON("MELON", "Dưa lưới / Dưa gang"),
    MUSKMELON("MUSKMELON", "Dưa lưới"),
    CANTALOUPE("CANTALOUPE", "Dưa lưới ruột cam"),
    HONEYDEW("HONEYDEW", "Dưa lê / Dưa bở"),
    GALIA_MELON("GALIA MELON", "Dưa lưới Galia"),
    CUCUMIS("CUCUMIS", "Chi Dưa (Cucumis)"),
    WINTER_MELON("WINTER MELON", "Bí đao / Bí xanh"),
    BITTER_MELON("BITTER MELON", "Khổ qua / Mướp đắng"),
    BITTER_GOURD("BITTER GOURD", "Khổ qua / Mướp đắng"),
    EGGPLANT("EGGPLANT", "Cà tím"),
    AUBERGINE("AUBERGINE", "Cà tím"),
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
    PASSION_FRUIT("PASSION FRUIT", "Chanh dây / Chanh leo"),

    // === NÔNG SẢN PHỔ BIẾN BỔ SUNG ===
    WATER_SPINACH("WATER SPINACH", "Rau muống"),
    BELL_PEPPER("BELL PEPPER", "Ớt chuông"),
    ASPARAGUS("ASPARAGUS", "Măng tây"),
    CUSTARD_APPLE("CUSTARD APPLE", "Mãng cầu"),
    SAPODILLA("SAPODILLA", "Hồng xiêm"),
    MANGOSTEEN("MANGOSTEEN", "Măng cụt"),
    COCONUT("COCONUT", "Dừa"),
    GINGER("GINGER", "Gừng"),
    BEETROOT("BEETROOT", "Củ dền"),
    ZUCCHINI("ZUCCHINI", "Bí ngòi"),
    BOK_CHOY("BOK CHOY", "Cải thìa"),
    BASIL("BASIL", "Húng quế"),
    LEMONGRASS("LEMONGRASS", "Sả"),
    MINT("MINT", "Bạc hà"),
    CORIANDER("CORIANDER", "Ngò rí"),
    PERILLA("PERILLA", "Tía tô"),
    SHISO("SHISO", "Tía tô"),
    NONI("NONI", "Quả nhàu"),
    MORINDA("MORINDA", "Quả nhàu"),
    PEAR("PEAR", "Lê"),
    STAR_FRUIT("STAR FRUIT", "Khế"),
    CARAMBOLA("CARAMBOLA", "Khế"),
    PERSIMMON("PERSIMMON", "Quả hồng"),
    POMEGRANATE("POMEGRANATE", "Lựu"),
    KIWI("KIWI", "Kiwi"),
    BLUEBERRY("BLUEBERRY", "Việt quất"),
    CHERRY("CHERRY", "Cherry / Anh đào"),
    RADISH("RADISH", "Củ cải"),
    DAIKON("DAIKON", "Củ cải trắng"),
    TARO("TARO", "Khoai môn"),
    CASSAVA("CASSAVA", "Khoai mì / Sắn"),
    YAM("YAM", "Khoai mỡ / Củ từ"),
    JICAMA("JICAMA", "Củ đậu / Củ sắn nước"),
    LOTUS_ROOT("LOTUS ROOT", "Củ sen / Ngó sen"),
    GALANGAL("GALANGAL", "Củ riềng"),
    OKRA("OKRA", "Đậu bắp"),
    CHAYOTE("CHAYOTE", "Su su"),
    KOHLRABI("KOHLRABI", "Su hào"),
    LOOFAH("LOOFAH", "Mướp"),
    CALABASH("CALABASH", "Bầu"),
    CELERY("CELERY", "Cần tây"),
    TURMERIC("TURMERIC", "Nghệ"),
    PITAYA("PITAYA", "Thanh long"),
    WAX_APPLE("WAX APPLE", "Mận / Quả roi"),
    ROSE_APPLE("ROSE APPLE", "Mận / Quả roi"),
    GRAPEFRUIT("GRAPEFRUIT", "Bưởi"),
    TANGERINE("TANGERINE", "Quýt"),
    MANDARIN("MANDARIN ORANGE", "Quýt"),
    CLEMENTINE("CLEMENTINE", "Quýt"),
    KUMQUAT("KUMQUAT", "Tắc / Quất"),
    CALAMANSI("CALAMANSI", "Tắc / Quất"),
    SOURSOP("SOURSOP", "Mãng cầu xiêm"),
    SUGAR_APPLE("SUGAR APPLE", "Na / Mãng cầu ta"),
    JUJUBE("JUJUBE", "Táo ta / Táo xanh"),
    TAMARIND("TAMARIND", "Me"),
    AMBARELLA("AMBARELLA", "Cóc"),
    LANGSAT("LANGSAT", "Bòn bon"),
    SHALLOT("SHALLOT", "Hành tím"),
    SCALLION("SCALLION", "Hành lá"),
    SPRING_ONION("SPRING ONION", "Hành lá"),
    LEEK("LEEK", "Hành boa rô / Tỏi tây"),
    CHIVES("CHIVES", "Hẹ"),
    DILL("DILL", "Thì là"),
    CILANTRO("CILANTRO", "Ngò rí / Rau mùi"),
    CULANTRO("CULANTRO", "Ngò gai / Mùi tàu"),
    VIETNAMESE_CORIANDER("VIETNAMESE CORIANDER", "Rau răm"),
    FISH_MINT("HOUTTUYNIA", "Rau diếp cá"),
    CENTELLA("CENTELLA", "Rau má"),
    PENNYWORT("PENNYWORT", "Rau má"),
    WATERCRESS("WATERCRESS", "Xà lách xoong / Cải xoong"),
    NAPA_CABBAGE("NAPA CABBAGE", "Cải thảo"),
    CHINESE_CABBAGE("CHINESE CABBAGE", "Cải thảo"),
    MUSTARD_GREENS("MUSTARD GREENS", "Cải bẹ xanh / Cải xanh"),
    CHOY_SUM("CHOY SUM", "Cải ngọt"),
    KALE("KALE", "Cải xoăn Kale"),
    AMARANTH("AMARANTH", "Rau dền"),
    MALABAR_SPINACH("MALABAR SPINACH", "Rau mồng tơi"),
    JUTE_MALLOW("JUTE MALLOW", "Rau đay"),
    BAMBOO_SHOOT("BAMBOO SHOOT", "Măng tươi"),
    ARTICHOKE("ARTICHOKE", "Atiso"),
    SOYBEAN("SOYBEAN", "Đậu nành"),
    MUNG_BEAN("MUNG BEAN", "Đậu xanh"),
    BEAN_SPROUT("BEAN SPROUT", "Giá đỗ"),
    OYSTER_MUSHROOM("OYSTER MUSHROOM", "Nấm bào ngư"),
    KING_OYSTER_MUSHROOM("KING OYSTER MUSHROOM", "Nấm đùi gà"),
    ENOKI("ENOKI MUSHROOM", "Nấm kim châm"),
    SHIITAKE("SHIITAKE", "Nấm hương / Nấm đông cô"),
    WOOD_EAR("WOOD EAR", "Nấm mèo / Mộc nhĩ");

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
        if (this == COCONUT) {
            return "dưa";
        }
        return null;
    }

    /**
     * Từ khóa tìm kiếm chuẩn hóa (ngắn gọn, không chứa tiền tố "Quả/Trái/Củ" hoặc dấu "/"
     * để thanh tìm kiếm Frontend và truy vấn DB khớp được mọi cách đặt tên của nhà vườn).
     */
    public String getSearchKeyword() {
        return switch (this) {
            case NONI, MORINDA -> "Nhàu";
            case MUSHROOM, FUNGUS, OYSTER_MUSHROOM, KING_OYSTER_MUSHROOM, ENOKI, SHIITAKE, WOOD_EAR -> "Nấm";
            case SPINACH -> "Cải bó xôi";
            case BROCCOLI -> "Bông cải";
            case CAULIFLOWER -> "Súp lơ";
            case PEPPER, CHILI -> "Ớt";
            case CORN, MAIZE -> "Bắp";
            case RICE -> "Gạo";
            case BEANS -> "Đậu que";
            case PEANUT -> "Đậu phộng";
            case ONION -> "Hành";
            case SHALLOT -> "Hành tím";
            case SCALLION, SPRING_ONION -> "Hành lá";
            case LEEK -> "Hành boa rô";
            case MELON, MUSKMELON, CANTALOUPE, GALIA_MELON, CUCUMIS -> "Dưa lưới";
            case HONEYDEW -> "Dưa lê";
            case WINTER_MELON -> "Bí đao";
            case BITTER_MELON, BITTER_GOURD -> "Khổ qua";
            case PINEAPPLE -> "Dứa";
            case DRAGON_FRUIT, PITAYA -> "Thanh long";
            case LYCHEE -> "Vải";
            case LIME, LEMON -> "Chanh";
            case KUMQUAT, CALAMANSI -> "Tắc";
            case TANGERINE, MANDARIN, CLEMENTINE -> "Quýt";
            case POMELO, GRAPEFRUIT -> "Bưởi";
            case PASSION_FRUIT -> "Chanh dây";
            case CUSTARD_APPLE, SUGAR_APPLE, SOURSOP -> "Mãng cầu";
            case WAX_APPLE, ROSE_APPLE, PLUM -> "Mận";
            case APPLE, JUJUBE -> "Táo";
            case STAR_FRUIT, CARAMBOLA -> "Khế";
            case BASIL -> "Húng";
            case CORIANDER, CILANTRO -> "Ngò";
            case CULANTRO -> "Ngò gai";
            case PERILLA, SHISO -> "Tía tô";
            case CENTELLA, PENNYWORT -> "Rau má";
            case WATERCRESS -> "Xà lách xoong";
            case NAPA_CABBAGE, CHINESE_CABBAGE -> "Cải thảo";
            case MUSTARD_GREENS -> "Cải xanh";
            case CHOY_SUM -> "Cải ngọt";
            case KALE -> "Cải xoăn";
            case PERSIMMON -> "Hồng";
            case CASSAVA -> "Khoai mì";
            case YAM -> "Khoai mỡ";
            case JICAMA -> "Củ đậu";
            case LOTUS_ROOT -> "Củ sen";
            case GALANGAL -> "Riềng";
            case RADISH, DAIKON -> "Củ cải";
            case BAMBOO_SHOOT -> "Măng";
            case CHERRY -> "Cherry";
            default -> stripClassifierPrefix(this.vietnameseName);
        };
    }

    /**
     * Danh sách toàn bộ từ đồng nghĩa vùng miền & biến thể không dấu để tìm kiếm trong CSDL không bị sót.
     */
    public List<String> getSearchAliases() {
        Set<String> aliases = new LinkedHashSet<>();
        String primary = getSearchKeyword();
        if (primary != null && !primary.isBlank()) {
            aliases.add(primary.toLowerCase(Locale.ROOT));
        }
        switch (this) {
            case NONI, MORINDA -> aliases.addAll(List.of("nhàu", "nhau", "quả nhàu", "trái nhàu", "noni"));
            case MUSHROOM, FUNGUS, OYSTER_MUSHROOM, KING_OYSTER_MUSHROOM, ENOKI, SHIITAKE, WOOD_EAR ->
                    aliases.addAll(List.of("nấm", "nam", "nấm đùi gà", "nấm bào ngư", "nấm kim châm", "nấm hương", "nấm đông cô", "mộc nhĩ"));
            case SPINACH -> aliases.addAll(List.of("cải bó xôi", "rau chân vịt", "bó xôi", "chân vịt"));
            case BROCCOLI, CAULIFLOWER -> aliases.addAll(List.of("bông cải", "súp lơ", "bong cai", "sup lo"));
            case PEPPER, CHILI -> aliases.addAll(List.of("ớt", "tiêu", "ớt hiểm", "ớt chỉ thiên", "ớt sừng"));
            case BELL_PEPPER -> aliases.addAll(List.of("ớt chuông", "ớt ngọt", "ot chuong"));
            case CORN, MAIZE -> aliases.addAll(List.of("bắp", "ngô", "bap", "ngo", "bắp mỹ", "bắp nếp"));
            case BEANS -> aliases.addAll(List.of("đậu que", "đậu cô ve", "cô ve", "dau que"));
            case PEANUT -> aliases.addAll(List.of("đậu phộng", "lạc", "dau phong", "củ lạc"));
            case MELON, MUSKMELON, CANTALOUPE, GALIA_MELON, CUCUMIS ->
                    aliases.addAll(List.of("dưa lưới", "dưa gang", "dưa vàng", "dưa hoàng kim", "dưa hami", "dua luoi", "dua gang"));
            case HONEYDEW -> aliases.addAll(List.of("dưa lê", "dưa bở", "dưa lưới", "dua le"));
            case WINTER_MELON -> aliases.addAll(List.of("bí đao", "bí xanh", "bi dao", "bi xanh"));
            case BITTER_MELON, BITTER_GOURD -> aliases.addAll(List.of("khổ qua", "mướp đắng", "kho qua", "muop dang"));
            case CUCUMBER -> aliases.addAll(List.of("dưa leo", "dưa chuột", "dua leo", "dua chuot"));
            case PINEAPPLE -> aliases.addAll(List.of("dứa", "thơm", "khóm", "dua", "thom", "khom"));
            case DRAGON_FRUIT, PITAYA -> aliases.addAll(List.of("thanh long", "thanh long ruột đỏ", "thanh long ruột trắng"));
            case LYCHEE -> aliases.addAll(List.of("vải", "vải thiều", "vai thieu"));
            case LIME, LEMON -> aliases.addAll(List.of("chanh", "chanh xanh", "chanh vàng", "chanh không hạt", "chanh giấy"));
            case KUMQUAT, CALAMANSI -> aliases.addAll(List.of("tắc", "quất", "trái tắc", "quả quất", "chanh hạnh"));
            case TANGERINE, MANDARIN, CLEMENTINE -> aliases.addAll(List.of("quýt", "quýt đường", "quýt hồng", "cam quýt", "quyt"));
            case POMELO, GRAPEFRUIT -> aliases.addAll(List.of("bưởi", "bưởi da xanh", "bưởi năm roi", "buoi"));
            case PASSION_FRUIT -> aliases.addAll(List.of("chanh dây", "chanh leo", "chanh day", "chanh leo"));
            case CUSTARD_APPLE, SUGAR_APPLE, SOURSOP ->
                    aliases.addAll(List.of("mãng cầu", "mãng cầu xiêm", "mãng cầu ta", "na", "trái na", "quả na", "mang cau"));
            case WAX_APPLE, ROSE_APPLE, PLUM ->
                    aliases.addAll(List.of("mận", "quả roi", "trái roi", "mận an phước", "mận hậu", "mận hồng", "man"));
            case APPLE, JUJUBE -> aliases.addAll(List.of("táo", "táo xanh", "táo ta", "táo đỏ", "tao"));
            case STAR_FRUIT, CARAMBOLA -> aliases.addAll(List.of("khế", "quả khế", "trái khế", "khế ngọt", "khế chua", "khe"));
            case SAPODILLA -> aliases.addAll(List.of("hồng xiêm", "sa pô chê", "sapoche", "hong xiem"));
            case BASIL -> aliases.addAll(List.of("húng quế", "húng", "rau húng", "húng chó", "húng chanh", "hung que"));
            case MINT -> aliases.addAll(List.of("bạc hà", "húng lủi", "rau húng lủi", "bac ha", "hung lui"));
            case CORIANDER, CILANTRO -> aliases.addAll(List.of("ngò rí", "ngò", "rau mùi", "ngo ri", "rau mui"));
            case CULANTRO -> aliases.addAll(List.of("ngò gai", "mùi tàu", "ngo gai", "mui tau"));
            case VIETNAMESE_CORIANDER -> aliases.addAll(List.of("rau răm", "răm", "rau ram"));
            case FISH_MINT -> aliases.addAll(List.of("diếp cá", "rau diếp cá", "diep ca"));
            case CENTELLA, PENNYWORT -> aliases.addAll(List.of("rau má", "rau ma"));
            case WATERCRESS -> aliases.addAll(List.of("xà lách xoong", "cải xoong", "xa lach xoong", "cai xoong"));
            case NAPA_CABBAGE, CHINESE_CABBAGE -> aliases.addAll(List.of("cải thảo", "bắp cải thảo", "cai thao"));
            case MUSTARD_GREENS -> aliases.addAll(List.of("cải xanh", "cải bẹ xanh", "cải ngọt", "cai xanh", "cai be xanh"));
            case CHOY_SUM -> aliases.addAll(List.of("cải ngọt", "cải xanh", "cai ngot"));
            case BOK_CHOY -> aliases.addAll(List.of("cải thìa", "cải chíp", "cai thia", "cai chip"));
            case KALE -> aliases.addAll(List.of("cải xoăn", "cải kale", "kale", "cai xoan"));
            case AMARANTH -> aliases.addAll(List.of("rau dền", "dền đỏ", "dền cơm", "rau den"));
            case MALABAR_SPINACH -> aliases.addAll(List.of("mồng tơi", "rau mồng tơi", "mùng tơi", "mong toi", "mung toi"));
            case JUTE_MALLOW -> aliases.addAll(List.of("rau đay", "đay", "rau day"));
            case ONION -> aliases.addAll(List.of("hành tây", "hành", "hành củ", "hanh tay"));
            case SHALLOT -> aliases.addAll(List.of("hành tím", "hành hương", "hành củ", "hanh tim"));
            case SCALLION, SPRING_ONION -> aliases.addAll(List.of("hành lá", "hành hoa", "hanh la"));
            case LEEK -> aliases.addAll(List.of("hành boa rô", "boa rô", "tỏi tây", "hanh boa ro", "toi tay"));
            case CHIVES -> aliases.addAll(List.of("hẹ", "lá hẹ", "rau hẹ", "la he"));
            case DILL -> aliases.addAll(List.of("thì là", "rau thì là", "thi la"));
            case CASSAVA -> aliases.addAll(List.of("khoai mì", "sắn", "củ sắn", "khoai mi"));
            case YAM -> aliases.addAll(List.of("khoai mỡ", "khoai từ", "củ từ", "khoai mo", "cu tu"));
            case JICAMA -> aliases.addAll(List.of("củ đậu", "củ sắn nước", "sắn nước", "cu dau", "cu san nuoc"));
            case LOTUS_ROOT -> aliases.addAll(List.of("củ sen", "ngó sen", "hạt sen", "cu sen", "ngo sen"));
            case GALANGAL -> aliases.addAll(List.of("củ riềng", "riềng", "cu rieng", "rieng"));
            case BEETROOT -> aliases.addAll(List.of("củ dền", "dền", "cu den"));
            case RADISH, DAIKON -> aliases.addAll(List.of("củ cải", "củ cải trắng", "cải trắng", "cu cai"));
            case BAMBOO_SHOOT -> aliases.addAll(List.of("măng", "măng tươi", "măng tre", "măng trúc", "mang tuoi"));
            case CHERRY -> aliases.addAll(List.of("cherry", "anh đào", "quả anh đào", "anh dao"));
            case BEAN_SPROUT -> aliases.addAll(List.of("giá đỗ", "giá sống", "gia do"));
            default -> {
                String stripped = stripClassifierPrefix(this.vietnameseName).toLowerCase(Locale.ROOT);
                aliases.add(stripped);
                String unaccented = removeDiacritics(stripped);
                if (!unaccented.isBlank() && unaccented.length() >= 3) {
                    aliases.add(unaccented);
                }
            }
        }
        // Bổ sung biến thể không dấu cho các từ khóa >= 3 ký tự (ví dụ "nhàu" -> "nhau", "dưa lưới" -> "dua luoi")
        for (String a : new ArrayList<>(aliases)) {
            String unaccented = removeDiacritics(a);
            if (!unaccented.isBlank() && unaccented.length() >= 3) {
                aliases.add(unaccented);
            }
        }
        return new ArrayList<>(aliases);
    }

    public static String stripClassifierPrefix(String name) {
        if (name == null) return "";
        String trimmed = name.trim();
        if (trimmed.contains("/")) {
            trimmed = trimmed.split("/")[0].trim();
        }
        String lower = trimmed.toLowerCase(Locale.ROOT);
        for (String prefix : List.of("quả ", "trái ", "cây ")) {
            if (lower.startsWith(prefix) && trimmed.length() > prefix.length()) {
                return trimmed.substring(prefix.length()).trim();
            }
        }
        return trimmed;
    }

    public static String removeDiacritics(String input) {
        if (input == null) return "";
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        return normalized
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    /**
     * Trích xuất loại nông sản cụ thể từ tên sản phẩm tiếng Việt hoặc tiếng Anh.
     */
    public static Optional<AgriculturalProduceLabel> detectExpectedProduce(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        String lower = text.toLowerCase(Locale.ROOT);
        String ascii = " " + removeDiacritics(lower).replaceAll("[^a-z0-9]+", " ").trim() + " ";

        // Thứ tự ưu tiên từ dài đến ngắn để tránh nhận diện nhầm từ con (Hỗ trợ cả tiếng Việt có dấu, không dấu và tiếng Anh)
        if (lower.contains("quả nhàu") || lower.contains("trái nhàu") || lower.contains("cây nhàu") || lower.contains("nhàu") || ascii.contains(" qua nhau ") || ascii.contains(" trai nhau ") || ascii.contains(" nhau ") || lower.contains("noni") || lower.contains("morinda")) return Optional.of(NONI);
        if (lower.contains("dưa hấu") || ascii.contains(" dua hau ") || lower.contains("watermelon") || lower.contains("citrullus")) return Optional.of(WATERMELON);
        if (lower.contains("dưa lưới") || lower.contains("dưa vàng") || lower.contains("dưa gang") || lower.contains("dưa hoàng kim") || lower.contains("dưa hami") || ascii.contains(" dua luoi ") || ascii.contains(" dua gang ") || lower.contains("cantaloupe") || lower.contains("muskmelon")) return Optional.of(MUSKMELON);
        if (lower.contains("dưa lê") || lower.contains("dưa bở") || ascii.contains(" dua le ") || ascii.contains(" dua bo ") || lower.contains("honeydew")) return Optional.of(HONEYDEW);
        if (lower.contains("bí đao") || lower.contains("bí xanh") || ascii.contains(" bi dao ") || ascii.contains(" bi xanh ") || lower.contains("winter melon")) return Optional.of(WINTER_MELON);
        if (lower.contains("thanh long") || ascii.contains(" thanh long ") || lower.contains("dragon fruit") || lower.contains("pitaya")) return Optional.of(DRAGON_FRUIT);
        if (lower.contains("sầu riêng") || ascii.contains(" sau rieng ") || lower.contains("durian")) return Optional.of(DURIAN);
        if (lower.contains("khoai lang") || ascii.contains(" khoai lang ") || lower.contains("sweet potato")) return Optional.of(SWEET_POTATO);
        if (lower.contains("khoai tây") || ascii.contains(" khoai tay ") || lower.contains("potato")) return Optional.of(POTATO);
        if (lower.contains("khoai môn") || ascii.contains(" khoai mon ") || lower.contains("taro")) return Optional.of(TARO);
        if (lower.contains("khoai mỡ") || lower.contains("củ từ") || ascii.contains(" khoai mo ") || lower.contains("yam")) return Optional.of(YAM);
        if (lower.contains("củ đậu") || lower.contains("củ sắn nước") || ascii.contains(" cu dau ") || lower.contains("jicama")) return Optional.of(JICAMA);
        if (lower.contains("khoai mì") || lower.contains("củ sắn") || ascii.contains(" khoai mi ") || lower.contains("cassava")) return Optional.of(CASSAVA);
        if (lower.contains("cải thảo") || ascii.contains(" cai thao ") || lower.contains("napa cabbage") || lower.contains("chinese cabbage")) return Optional.of(NAPA_CABBAGE);
        if (lower.contains("bắp cải") || ascii.contains(" bap cai ") || lower.contains("cabbage")) return Optional.of(CABBAGE);
        if (lower.contains("bông cải") || lower.contains("súp lơ xanh") || ascii.contains(" bong cai ") || lower.contains("broccoli")) return Optional.of(BROCCOLI);
        if (lower.contains("súp lơ") || ascii.contains(" sup lo ") || lower.contains("cauliflower")) return Optional.of(CAULIFLOWER);
        if (lower.contains("măng cụt") || ascii.contains(" mang cut ") || lower.contains("mangosteen")) return Optional.of(MANGOSTEEN);
        if (lower.contains("chôm chôm") || ascii.contains(" chom chom ") || lower.contains("rambutan")) return Optional.of(RAMBUTAN);
        if (lower.contains("dâu tây") || ascii.contains(" dau tay ") || lower.contains("strawberry")) return Optional.of(STRAWBERRY);
        if (lower.contains("việt quất") || ascii.contains(" viet quat ") || lower.contains("blueberry")) return Optional.of(BLUEBERRY);
        if (lower.contains("cherry") || lower.contains("anh đào") || ascii.contains(" anh dao ")) return Optional.of(CHERRY);
        if (lower.contains("dưa leo") || lower.contains("dưa chuột") || ascii.contains(" dua leo ") || ascii.contains(" dua chuot ") || lower.contains("cucumber")) return Optional.of(CUCUMBER);
        if (lower.contains("khổ qua") || lower.contains("mướp đắng") || ascii.contains(" kho qua ") || ascii.contains(" muop dang ") || lower.contains("bitter melon") || lower.contains("bitter gourd")) return Optional.of(BITTER_MELON);
        if (lower.contains("cà chua") || ascii.contains(" ca chua ") || lower.contains("tomato")) return Optional.of(TOMATO);
        if (lower.contains("cà rốt") || ascii.contains(" ca rot ") || lower.contains("carrot")) return Optional.of(CARROT);
        if (lower.contains("cà tím") || ascii.contains(" ca tim ") || lower.contains("eggplant") || lower.contains("aubergine")) return Optional.of(EGGPLANT);
        if (lower.contains("ớt chuông") || ascii.contains(" ot chuong ") || lower.contains("bell pepper") || lower.contains("sweet pepper")) return Optional.of(BELL_PEPPER);
        if (lower.contains("măng tây") || ascii.contains(" mang tay ") || lower.contains("asparagus")) return Optional.of(ASPARAGUS);
        if (lower.contains("măng tươi") || lower.contains("măng tre") || lower.contains("măng trúc") || lower.contains("bamboo shoot")) return Optional.of(BAMBOO_SHOOT);
        if (lower.contains("rau muống") || ascii.contains(" rau muong ") || lower.contains("water spinach") || lower.contains("morning glory")) return Optional.of(WATER_SPINACH);
        if (lower.contains("rau mồng tơi") || lower.contains("mồng tơi") || lower.contains("mùng tơi") || ascii.contains(" mong toi ") || lower.contains("malabar spinach")) return Optional.of(MALABAR_SPINACH);
        if (lower.contains("rau dền") || ascii.contains(" rau den ") || lower.contains("amaranth")) return Optional.of(AMARANTH);
        if (lower.contains("rau đay") || ascii.contains(" rau day ") || lower.contains("jute mallow")) return Optional.of(JUTE_MALLOW);
        if (lower.contains("rau má") || ascii.contains(" rau ma ") || lower.contains("centella") || lower.contains("pennywort")) return Optional.of(CENTELLA);
        if (lower.contains("diếp cá") || ascii.contains(" diep ca ") || lower.contains("houttuynia") || lower.contains("fish mint")) return Optional.of(FISH_MINT);
        if (lower.contains("rau răm") || ascii.contains(" rau ram ") || lower.contains("vietnamese coriander")) return Optional.of(VIETNAMESE_CORIANDER);
        if (lower.contains("xà lách xoong") || lower.contains("cải xoong") || ascii.contains(" xa lach xoong ") || ascii.contains(" cai xoong ") || lower.contains("watercress")) return Optional.of(WATERCRESS);
        if (lower.contains("chanh dây") || lower.contains("chanh leo") || ascii.contains(" chanh day ") || ascii.contains(" chanh leo ") || lower.contains("passion fruit")) return Optional.of(PASSION_FRUIT);
        if (lower.contains("mãng cầu xiêm") || lower.contains("soursop")) return Optional.of(SOURSOP);
        if (lower.contains("mãng cầu") || lower.contains("trái na") || lower.contains("quả na") || ascii.contains(" mang cau ") || lower.contains("custard apple") || lower.contains("sugar apple")) return Optional.of(CUSTARD_APPLE);
        if (lower.contains("hồng xiêm") || lower.contains("sa pô chê") || lower.contains("sapoche") || ascii.contains(" hong xiem ") || lower.contains("sapodilla")) return Optional.of(SAPODILLA);
        if (lower.contains("dừa xiêm") || lower.contains("trái dừa") || lower.contains("quả dừa") || lower.contains("coconut")) return Optional.of(COCONUT);
        if (lower.contains("củ gừng") || lower.contains("gừng") || lower.contains("ginger")) return Optional.of(GINGER);
        if (lower.contains("củ riềng") || lower.contains("riềng") || ascii.contains(" cu rieng ") || lower.contains("galangal")) return Optional.of(GALANGAL);
        if (lower.contains("củ nghệ") || lower.contains("nghệ") || lower.contains("turmeric")) return Optional.of(TURMERIC);
        if (lower.contains("củ sen") || lower.contains("ngó sen") || ascii.contains(" cu sen ") || ascii.contains(" ngo sen ") || lower.contains("lotus root")) return Optional.of(LOTUS_ROOT);
        if (lower.contains("củ dền") || ascii.contains(" cu den ") || lower.contains("beetroot") || lower.contains("beet")) return Optional.of(BEETROOT);
        if (lower.contains("củ cải") || ascii.contains(" cu cai ") || lower.contains("radish") || lower.contains("daikon")) return Optional.of(RADISH);
        if (lower.contains("đậu bắp") || ascii.contains(" dau bap ") || lower.contains("okra")) return Optional.of(OKRA);
        if (lower.contains("su su") || lower.contains("chayote")) return Optional.of(CHAYOTE);
        if (lower.contains("su hào") || ascii.contains(" su hao ") || lower.contains("kohlrabi")) return Optional.of(KOHLRABI);
        if (lower.contains("cần tây") || ascii.contains(" can tay ") || lower.contains("celery")) return Optional.of(CELERY);
        if (lower.contains("bí ngòi") || ascii.contains(" bi ngoi ") || lower.contains("zucchini") || lower.contains("courgette")) return Optional.of(ZUCCHINI);
        if (lower.contains("cải thìa") || lower.contains("cải chíp") || ascii.contains(" cai thia ") || lower.contains("bok choy") || lower.contains("pak choi")) return Optional.of(BOK_CHOY);
        if (lower.contains("cải bẹ xanh") || lower.contains("cải xanh") || ascii.contains(" cai xanh ") || lower.contains("mustard greens")) return Optional.of(MUSTARD_GREENS);
        if (lower.contains("cải ngọt") || ascii.contains(" cai ngot ") || lower.contains("choy sum")) return Optional.of(CHOY_SUM);
        if (lower.contains("cải xoăn") || lower.contains("cải kale") || lower.contains("kale")) return Optional.of(KALE);
        if (lower.contains("bí đỏ") || ascii.contains(" bi do ") || lower.contains("pumpkin") || lower.contains("squash")) return Optional.of(PUMPKIN);
        if (lower.contains("xà lách") || ascii.contains(" xa lach ") || lower.contains("lettuce") || lower.contains("salad greens")) return Optional.of(LETTUCE);
        if (lower.contains("rau chân vịt") || lower.contains("cải bó xôi") || ascii.contains(" cai bo xoi ") || lower.contains("spinach")) return Optional.of(SPINACH);
        if (lower.contains("bơ sáp") || lower.contains("quả bơ") || lower.contains("trái bơ") || lower.contains("bơ") || ascii.contains(" bo sap ") || ascii.contains(" qua bo ") || ascii.contains(" trai bo ") || lower.contains("avocado")) return Optional.of(AVOCADO);
        if (lower.contains("chuối") || ascii.contains(" chuoi ") || lower.contains("banana")) return Optional.of(BANANA);
        if (lower.contains("xoài") || ascii.contains(" xoai ") || lower.contains("mango")) return Optional.of(MANGO);
        if (lower.contains("dứa") || lower.contains("thơm") || lower.contains("khóm") || lower.contains("pineapple")) return Optional.of(PINEAPPLE);
        if (lower.contains("đu đủ") || ascii.contains(" du du ") || lower.contains("papaya")) return Optional.of(PAPAYA);
        if (lower.contains("ổi") || ascii.contains(" qua oi ") || ascii.contains(" trai oi ") || lower.contains("guava")) return Optional.of(GUAVA);
        if (lower.contains("vải") || ascii.contains(" vai thieu ") || lower.contains("lychee")) return Optional.of(LYCHEE);
        if (lower.contains("nhãn") || lower.contains("longan")) return Optional.of(LONGAN);
        if (lower.contains("mít") || lower.contains("jackfruit")) return Optional.of(JACKFRUIT);
        if (lower.contains("bưởi") || ascii.contains(" buoi ") || lower.contains("pomelo") || lower.contains("grapefruit")) return Optional.of(POMELO);
        if (lower.contains("quýt") || ascii.contains(" quyt ") || lower.contains("tangerine") || lower.contains("mandarin") || lower.contains("clementine")) return Optional.of(TANGERINE);
        if (lower.contains("trái tắc") || lower.contains("quả tắc") || lower.contains("quả quất") || lower.contains("kumquat") || lower.contains("calamansi")) return Optional.of(KUMQUAT);
        if (lower.contains("quả lê") || lower.contains("trái lê") || lower.contains("pear")) return Optional.of(PEAR);
        if (lower.contains("quả khế") || lower.contains("trái khế") || lower.contains("khế") || lower.contains("star fruit") || lower.contains("carambola")) return Optional.of(STAR_FRUIT);
        if (lower.contains("quả hồng") || lower.contains("trái hồng") || lower.contains("hồng giòn") || lower.contains("persimmon")) return Optional.of(PERSIMMON);
        if (lower.contains("quả lựu") || lower.contains("trái lựu") || lower.contains("lựu") || lower.contains("pomegranate")) return Optional.of(POMEGRANATE);
        if (lower.contains("kiwi")) return Optional.of(KIWI);
        if (lower.contains("mướp hương") || lower.contains("quả mướp") || lower.contains("loofah") || lower.contains("luffa")) return Optional.of(LOOFAH);
        if (lower.contains("quả bầu") || lower.contains("trái bầu") || lower.contains("calabash") || lower.contains("bottle gourd")) return Optional.of(CALABASH);
        if (lower.contains("húng quế") || lower.contains("húng chanh") || lower.contains("húng tây") || lower.contains("húng chó") || lower.contains("rau húng") || ascii.contains(" hung que ") || lower.contains("lemon basil") || lower.contains("lime basil") || lower.contains("thai basil") || lower.contains("sweet basil") || lower.contains("basil") || lower.contains("ocimum")) return Optional.of(BASIL);
        if (lower.contains("cây sả") || lower.contains("củ sả") || lower.contains("lemongrass") || lower.contains("sả")) return Optional.of(LEMONGRASS);
        if (lower.contains("bạc hà") || lower.contains("húng lủi") || ascii.contains(" bac ha ") || lower.contains("peppermint") || lower.contains("spearmint") || lower.contains("mint")) return Optional.of(MINT);
        if (lower.contains("ngò gai") || lower.contains("mùi tàu") || ascii.contains(" ngo gai ") || ascii.contains(" mui tau ") || lower.contains("culantro")) return Optional.of(CULANTRO);
        if (lower.contains("ngò rí") || lower.contains("rau mùi") || lower.contains("rau ngò") || ascii.contains(" ngo ri ") || ascii.contains(" rau mui ") || lower.contains("coriander") || lower.contains("cilantro")) return Optional.of(CORIANDER);
        if (lower.contains("tía tô") || ascii.contains(" tia to ") || lower.contains("perilla") || lower.contains("shiso")) return Optional.of(PERILLA);
        if (lower.contains("thì là") || ascii.contains(" thi la ") || lower.contains("dill")) return Optional.of(DILL);
        if (lower.contains("lá hẹ") || lower.contains("rau hẹ") || lower.contains("chives")) return Optional.of(CHIVES);
        if (lower.contains("chanh vàng") || lower.contains("lemon")) return Optional.of(LEMON);
        if (lower.contains("chanh xanh") || lower.contains("chanh") || ascii.contains(" chanh ") || lower.contains("lime")) return Optional.of(LIME);
        if (lower.contains("táo ta") || lower.contains("táo xanh") || lower.contains("jujube")) return Optional.of(JUJUBE);
        if (lower.contains("táo") || ascii.contains(" qua tao ") || lower.contains("apple")) return Optional.of(APPLE);
        if (lower.contains("cam") || lower.contains("orange")) return Optional.of(ORANGE);
        if (lower.contains("nho") || lower.contains("grape")) return Optional.of(GRAPE);
        if (lower.contains("quả roi") || lower.contains("trái roi") || lower.contains("mận an phước") || lower.contains("wax apple") || lower.contains("rose apple")) return Optional.of(WAX_APPLE);
        if (lower.contains("mận") || lower.contains("plum")) return Optional.of(PLUM);
        if (lower.contains("đào") || lower.contains("peach")) return Optional.of(PEACH);
        if (lower.contains("hành tím") || lower.contains("hành hương") || ascii.contains(" hanh tim ") || lower.contains("shallot")) return Optional.of(SHALLOT);
        if (lower.contains("hành lá") || lower.contains("hành hoa") || ascii.contains(" hanh la ") || lower.contains("scallion") || lower.contains("spring onion")) return Optional.of(SCALLION);
        if (lower.contains("hành boa rô") || lower.contains("tỏi tây") || ascii.contains(" hanh boa ro ") || lower.contains("leek")) return Optional.of(LEEK);
        if (lower.contains("hành tây") || lower.contains("hành củ") || ascii.contains(" hanh tay ") || lower.contains("onion")) return Optional.of(ONION);
        if (lower.contains("tỏi") || ascii.contains(" cu toi ") || lower.contains("garlic")) return Optional.of(GARLIC);
        if (lower.contains("ớt") || lower.contains("chili") || lower.contains("pepper")) return Optional.of(CHILI);
        if (lower.contains("bắp") || lower.contains("ngô") || lower.contains("corn") || lower.contains("maize")) return Optional.of(CORN);
        if (lower.contains("nấm") || lower.contains("mushroom")) return Optional.of(MUSHROOM);
        if (lower.contains("giá đỗ") || ascii.contains(" gia do ") || lower.contains("bean sprout")) return Optional.of(BEAN_SPROUT);
        if (lower.contains("đậu hà lan") || ascii.contains(" dau ha lan ") || lower.contains("peas")) return Optional.of(PEAS);
        if (lower.contains("đậu que") || lower.contains("đậu cô ve") || ascii.contains(" dau que ") || ascii.contains(" co ve ") || lower.contains("green bean")) return Optional.of(BEANS);
        if (lower.contains("đậu phộng") || lower.contains("lạc") || ascii.contains(" dau phong ") || lower.contains("peanut")) return Optional.of(PEANUT);

        return Optional.empty();
    }

    /**
     * Kiểm tra xem nhãn Google Vision trả về có phải là đồng loại / họ hàng với loại nông sản mong đợi không.
     */
    public boolean isFamilyCompatibleWith(AgriculturalProduceLabel detected) {
        if (this == detected) return true;
        if (!detected.isSpecificProduce()) return true; // Nhãn nhóm chung luôn tương thích

        // Nếu cùng chia sẻ từ khóa tìm kiếm chuẩn hóa (ví dụ NONI & MORINDA -> "Nhàu", DRAGON_FRUIT & PITAYA -> "Thanh long")
        if (Objects.equals(this.getSearchKeyword(), detected.getSearchKeyword())) {
            return true;
        }

        // Các nhóm họ hàng gần gũi
        // Nhóm dưa lưới / dưa gang / dưa lê (Cucumis melo & các phân loài)
        boolean isThisMelon = (this == MELON || this == MUSKMELON || this == CANTALOUPE || this == HONEYDEW || this == GALIA_MELON || this == CUCUMIS);
        boolean isDetectedMelon = (detected == MELON || detected == MUSKMELON || detected == CANTALOUPE || detected == HONEYDEW || detected == GALIA_MELON || detected == CUCUMIS);
        if (isThisMelon && isDetectedMelon) return true;

        // Nhóm trái cây có múi (Cam / Quýt / Chanh / Tắc / Bưởi)
        Set<AgriculturalProduceLabel> citrusGroup = Set.of(ORANGE, TANGERINE, MANDARIN, CLEMENTINE, LIME, LEMON, KUMQUAT, CALAMANSI, POMELO, GRAPEFRUIT);
        if (citrusGroup.contains(this) && citrusGroup.contains(detected)) return true;

        // Nhóm cải xanh / cải ngọt / cải thìa / cải thảo / bắp cải / cải xoăn
        Set<AgriculturalProduceLabel> brassicaGroup = Set.of(CABBAGE, NAPA_CABBAGE, CHINESE_CABBAGE, BOK_CHOY, MUSTARD_GREENS, CHOY_SUM, KALE, BROCCOLI, CAULIFLOWER);
        if (brassicaGroup.contains(this) && brassicaGroup.contains(detected)) return true;

        // Nhóm hành / tỏi / hẹ / boa rô (Allium)
        Set<AgriculturalProduceLabel> alliumGroup = Set.of(ONION, SHALLOT, SCALLION, SPRING_ONION, LEEK, CHIVES, GARLIC);
        if (alliumGroup.contains(this) && alliumGroup.contains(detected)) return true;

        // Nhóm củ khoai (Khoai tây / Khoai lang / Khoai môn / Khoai mì / Khoai mỡ / Củ đậu)
        Set<AgriculturalProduceLabel> tuberGroup = Set.of(POTATO, SWEET_POTATO, TARO, CASSAVA, YAM, JICAMA);
        if (tuberGroup.contains(this) && tuberGroup.contains(detected)) return true;

        // Nhóm gừng / riềng / nghệ
        Set<AgriculturalProduceLabel> rhizomeGroup = Set.of(GINGER, GALANGAL, TURMERIC);
        if (rhizomeGroup.contains(this) && rhizomeGroup.contains(detected)) return true;

        if ((this == BITTER_MELON && detected == BITTER_GOURD) || (this == BITTER_GOURD && detected == BITTER_MELON)) return true;
        if ((this == EGGPLANT && detected == AUBERGINE) || (this == AUBERGINE && detected == EGGPLANT)) return true;
        if ((this == CORN && detected == MAIZE) || (this == MAIZE && detected == CORN)) return true;

        return false;
    }
}

