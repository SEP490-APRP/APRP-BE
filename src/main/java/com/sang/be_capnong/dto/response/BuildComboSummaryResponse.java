package com.sang.be_capnong.dto.response;

import com.sang.be_capnong.enums.MealType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BuildComboSummaryResponse {
    int comboId;
    String comboName;
    BigDecimal price;
    MealType mealType;
    String type;
    String region;
    String regionDisplayName;
    String cookingSuggestion;
    String imageUrl;
    String description;
    Integer shopId;
    String shopName;
    Integer remainingStock;
}
