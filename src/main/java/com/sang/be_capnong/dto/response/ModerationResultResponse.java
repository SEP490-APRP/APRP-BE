package com.sang.be_capnong.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ModerationResultResponse {
    int id;
    Boolean isFlagged;
    BigDecimal overallConfidence;
    LocalDateTime checkedAt;
    String rawResponse;
    List<ModerationLabelResponse> moderationLabels;
}
