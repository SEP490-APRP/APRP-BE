package com.sang.be_capnong.service.impl;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Nhận biết câu chọn món trong danh sách bot vừa đưa ra ("2", "số 2", "lấy cái đầu tiên").
 * Chuyển từ luồng SELECT_ITEM_OPTION của dự án cũ. Chỉ khớp cả câu để không nhầm với câu mua hàng như "mua 2 kg".
 */
final class ChatbotSelectionParser {

    private static final Pattern NUMBER = Pattern.compile(
            "^(?:(?:tôi |mình |em )?(?:lấy|chọn|mua|thêm)\\s+)?(?:(?:cái|món|loại|số|sản phẩm)\\s+)?(?:(?:thứ|số)\\s+)?(\\d{1,2})(?:\\s+(?:nhé|nha|đi|ạ))?$");
    private static final Pattern WORD = Pattern.compile(
            "^(?:(?:tôi |mình |em )?(?:lấy|chọn|mua|thêm)\\s+)?(?:(?:cái|món|loại|sản phẩm)\\s+)?(đầu tiên|thứ nhất|số một|thứ hai|số hai|thứ ba|số ba|thứ tư|số bốn|thứ năm|số năm|cuối cùng)(?:\\s+(?:nhé|nha|đi|ạ))?$");
    private static final List<String> ORDINALS = List.of("thứ nhất", "thứ hai", "thứ ba", "thứ tư", "thứ năm");
    private static final List<String> NUMBERED = List.of("số một", "số hai", "số ba", "số bốn", "số năm");

    private ChatbotSelectionParser() {
    }

    /** @return vị trí bắt đầu từ 1, -1 nghĩa là món cuối, null nếu không phải câu chọn món. */
    static Integer parse(String message) {
        if (message == null) return null;
        String text = message.trim().toLowerCase(Locale.ROOT).replaceAll("[.!?,]+$", "").trim();

        Matcher number = NUMBER.matcher(text);
        if (number.matches()) {
            int value = Integer.parseInt(number.group(1));
            return value > 0 ? value : null;
        }

        Matcher word = WORD.matcher(text);
        if (!word.matches()) return null;
        String position = word.group(1);
        if (position.equals("đầu tiên")) return 1;
        if (position.equals("cuối cùng")) return -1;
        int index = ORDINALS.indexOf(position);
        return (index >= 0 ? index : NUMBERED.indexOf(position)) + 1;
    }
}
