package com.sang.be_capnong.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PlanCopilotActionDto {
    String actionType; // "REPLACE_COMBO", "UPDATE_QUANTITY"
    Integer dayIndex;
    String mealType;
    Integer targetMealId;
    Integer targetItemId;
    Integer newComboId;
    String newComboName;
    String newComboImageUrl;
    BigDecimal newPrice;
    Integer quantity;
    String reason;
}
