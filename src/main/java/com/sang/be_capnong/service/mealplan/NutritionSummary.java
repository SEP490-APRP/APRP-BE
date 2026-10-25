package com.sang.be_capnong.service.mealplan;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NutritionSummary {
    @Builder.Default
    BigDecimal calories = BigDecimal.ZERO;
    @Builder.Default
    BigDecimal proteinG = BigDecimal.ZERO;
    @Builder.Default
    BigDecimal carbsG = BigDecimal.ZERO;
    @Builder.Default
    BigDecimal fatG = BigDecimal.ZERO;

    @Builder.Default
    boolean incomplete = false;
}
