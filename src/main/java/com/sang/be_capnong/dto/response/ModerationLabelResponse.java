package com.sang.be_capnong.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ModerationLabelResponse {
    int id;
    String labelName;
    String labelNameVi;
    String parentLabel;
    String parentLabelVi;
    BigDecimal confidence;
}
