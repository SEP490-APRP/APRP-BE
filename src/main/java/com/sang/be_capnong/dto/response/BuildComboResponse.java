package com.sang.be_capnong.dto.response;

import com.sang.be_capnong.enums.MealType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BuildComboResponse {

    Integer comboId;
    String comboName;
    BigDecimal price;
    String description;
    String type;
    MealType mealType;
    String region;
    Integer totalQuantity;
    Integer soldQuantity;
    String status;
    String cookingSuggestion;
    String imageUrl;
    ShopSummaryResponse shop;
    List<ProductComboResponse> items;
}
