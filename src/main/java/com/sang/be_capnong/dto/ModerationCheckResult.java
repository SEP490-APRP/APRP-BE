package com.sang.be_capnong.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ModerationCheckResult {
    boolean isAgricultural;
    boolean isFlagged;
    BigDecimal overallConfidence;
    String rejectionReason;
    String flagReason;
    String rawResponse;
    List<ModerationLabelItem> labels;

    public String getFlagReason() {
        return flagReason != null ? flagReason : rejectionReason;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ModerationLabelItem {
        String labelName;
        String parentLabel;
        BigDecimal confidence;
    }
}
