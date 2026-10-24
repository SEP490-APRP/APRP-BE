package com.sang.be_capnong.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NutritionInfoResponse {
    int id;
    Integer productId;
    BigDecimal calories;
    BigDecimal proteinG;
    BigDecimal carbsG;
    BigDecimal fatG;
}
