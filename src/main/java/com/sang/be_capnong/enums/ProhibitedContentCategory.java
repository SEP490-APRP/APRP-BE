package com.sang.be_capnong.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

@Getter
@RequiredArgsConstructor
public enum ProhibitedContentCategory {

    NARCOTICS(
            "Chất gây nghiện & Ma túy",
            List.of("ma túy", "ma tuy", "cần sa", "can sa", "thuốc phiện", "thuoc phien",
                    "heroin", "cỏ mỹ", "co my", "ketamin", "ma túy đá", "ma tuy da")
    ),

    WEAPONS_EXPLOSIVES(
            "Vũ khí & Chất nổ",
            List.of("súng", "sung", "đạn", "dan", "bom", "lựu đạn", "luu dan",
                    "thuốc nổ", "thuoc no", "pháo nổ", "phao no", "dao bấm", "dao bam", "vũ khí", "vu khi")
    ),

    ALCOHOL_TOBACCO(
            "Rượu bia & Thuốc lá",
            List.of("rượu mạnh", "ruou manh", "vodka", "whisky", "thuốc lá", "thuoc la",
                    "thuốc lào", "thuoc lao", "vape", "xì gà", "xi ga")
    ),

    WILDLIFE_FORBIDDEN_MEAT(
            "Động vật hoang dã & Thịt cấm",
            List.of("thịt chó", "thit cho", "thịt mèo", "thit meo", "cao hổ", "cao ho",
                    "ngà voi", "nga voi", "sừng tê giác", "sung te giac")
    ),

    PROMPT_INJECTION(
            "Can thiệp & Tấn công Prompt AI",
            List.of("ignore previous instructions", "bỏ qua các chỉ dẫn", "bo qua cac chi dan",
                    "bỏ qua hướng dẫn", "bo qua huong dan", "system prompt", "jailbreak")
    );

    private final String description;
    private final List<String> keywords;

    /**
     * Quét chuỗi văn bản xem có chứa từ cấm nào thuộc danh mục này không.
     *
     * @param text Văn bản cần kiểm tra
     * @return Optional chứa Danh mục vi phạm nếu tìm thấy, ngược lại trả về Optional.empty()
     */
    public static Optional<ProhibitedContentCategory> findViolation(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        String normalized = text.toLowerCase();
        for (ProhibitedContentCategory category : values()) {
            for (String kw : category.getKeywords()) {
                if (normalized.contains(kw)) {
                    return Optional.of(category);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Kiểm tra nhanh xem văn bản có chứa bất kỳ từ cấm nào không.
     */
    public static boolean containsProhibitedContent(String text) {
        return findViolation(text).isPresent();
    }
}
