package com.sang.be_capnong.service.mealplan;

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
public class CandidateComboDto {
    int comboId;
    String name;
    MealType mealType;
    BigDecimal price;
    String region;
    Integer shopId;
    String shopName;
    List<String> ingredients;
    List<String> allergens;
    NutritionSummary nutrition;
    int stockLeft;
    boolean allergenDataIncomplete;
}
