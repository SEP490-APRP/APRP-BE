package com.sang.be_capnong.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PlanMealItemResponse {
    int id;
    Integer quantity;
    BigDecimal unitPriceSnapshot;
    BigDecimal itemSubtotal;
    Boolean isAiSuggested;
    Integer position;
    Boolean userOverride;
    BuildComboSummaryResponse combo;
}
