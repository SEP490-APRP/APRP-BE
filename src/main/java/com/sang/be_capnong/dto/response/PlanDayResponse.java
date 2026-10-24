package com.sang.be_capnong.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PlanDayResponse {
    int id;
    Integer dayIndex;
    LocalDate date;
    BigDecimal dayCost;
    List<PlanMealResponse> planMeals;
}
