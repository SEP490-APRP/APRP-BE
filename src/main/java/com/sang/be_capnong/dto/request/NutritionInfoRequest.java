package com.sang.be_capnong.dto.request;

import jakarta.validation.constraints.DecimalMin;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NutritionInfoRequest {

    @DecimalMin(value = "0.0", message = "Calories must be non-negative")
    BigDecimal calories;

    @DecimalMin(value = "0.0", message = "Protein must be non-negative")
    BigDecimal proteinG;

    @DecimalMin(value = "0.0", message = "Carbs must be non-negative")
    BigDecimal carbsG;

    @DecimalMin(value = "0.0", message = "Fat must be non-negative")
    BigDecimal fatG;
}
