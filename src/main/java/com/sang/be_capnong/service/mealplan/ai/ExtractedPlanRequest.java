package com.sang.be_capnong.service.mealplan.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExtractedPlanRequest {

    @JsonProperty("number_of_days")
    Integer numberOfDays;

    @JsonProperty("number_of_people")
    Integer numberOfPeople;

    @JsonProperty("target_budget")
    BigDecimal targetBudget;

    String goal;

    @JsonProperty("meal_types")
    List<String> mealTypes;
}
