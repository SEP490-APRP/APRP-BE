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
    int comboId;
    String comboName;
    String imageUrl;
    Integer quantity;
    BigDecimal unitPriceSnapshot;
    BigDecimal itemSubtotal;
    Boolean isAiSuggested;
    Integer position;
    Boolean userOverride;
    Integer calories;
    Double protein;
    Double carbs;
    Double fat;
    String aiReasonTag;
    String aiDescription;
    String region;
    String regionDisplayName;
    String comboType;
    String cookingSuggestion;
    Integer remainingStock;
    Boolean inStock;
    BuildComboSummaryResponse combo;
}
