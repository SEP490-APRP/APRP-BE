package com.sang.be_capnong.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PlanHeaderUpdateRequest {
    String planName;
    BigDecimal targetBudget;
    String mealType;
}
