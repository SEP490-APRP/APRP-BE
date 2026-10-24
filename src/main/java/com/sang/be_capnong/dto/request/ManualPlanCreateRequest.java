package com.sang.be_capnong.dto.request;

import com.sang.be_capnong.enums.MealType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
public class ManualPlanCreateRequest {

    String planName;

    @NotNull(message = "Number of days is required")
    @Min(value = 1, message = "Number of days must be at least 1")
    @Max(value = 14, message = "Number of days must not exceed 14")
    Integer numberOfDays;

    @NotNull(message = "Number of people is required")
    @Min(value = 1, message = "Number of people must be at least 1")
    @Max(value = 20, message = "Number of people must not exceed 20")
    Integer numberOfPeople;

    @NotEmpty(message = "At least one meal type is required")
    List<MealType> mealTypes;

    BigDecimal targetBudget;

    String mealType; // Style/goal (e.g. FAMILY, VEGAN, DIET)

    LocalDate startDate;
}
