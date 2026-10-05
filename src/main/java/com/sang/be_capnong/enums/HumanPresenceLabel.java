package com.sang.be_capnong.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Getter
@RequiredArgsConstructor
public enum HumanPresenceLabel {

    // === NHẬN DIỆN CON NGƯỜI & ĐỐI TƯỢNG ===
    PERSON("PERSON", "Người"),
    HUMAN("HUMAN", "Con người"),
    PEOPLE("PEOPLE", "Nhiều người"),
    ADULT("ADULT", "Người lớn"),
    MAN("MAN", "Đàn ông"),
    WOMAN("WOMAN", "Phụ nữ"),
    FEMALE("FEMALE", "Nữ giới"),
    MALE("MALE", "Nam giới"),

    // === NHẬN DIỆN CƠ THỂ & VÙNG DA (BODY PARTS / SKIN) ===
    BODY_PART("BODY PART", "Bộ phận cơ thể"),
    SKIN("SKIN", "Vùng da người"),
    CHEST("CHEST", "Ngực"),
    TORSO("TORSO", "Thân người"),
    ARM("ARM", "Cánh tay"),
    LEG("LEG", "Chân"),

    // === TRANG PHỤC NHẠY CẢM / ĐỒ LÓT ===
    UNDERWEAR("UNDERWEAR", "Đồ lót");

    private final String awsLabel;
    private final String vietnameseName;

    private static final Map<String, HumanPresenceLabel> AWS_LABEL_MAP = new HashMap<>();

    static {
        for (HumanPresenceLabel item : values()) {
            AWS_LABEL_MAP.put(item.awsLabel.toUpperCase(), item);
        }
    }

    public static boolean isHumanPresence(String labelName) {
        if (labelName == null || labelName.isBlank()) {
            return false;
        }
        return AWS_LABEL_MAP.containsKey(labelName.trim().toUpperCase());
    }

    public static Optional<HumanPresenceLabel> findByAwsLabel(String labelName) {
        if (labelName == null || labelName.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(AWS_LABEL_MAP.get(labelName.trim().toUpperCase()));
    }

    public static String getVietnameseNameOrDefault(String labelName) {
        return findByAwsLabel(labelName)
                .map(HumanPresenceLabel::getVietnameseName)
                .orElse(labelName);
    }
}
