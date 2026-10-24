package com.sang.be_capnong.dto.request;

import com.sang.be_capnong.enums.MealType;
import jakarta.validation.constraints.DecimalMin;
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
public class PlanGenerateRequest {

    String planName;

    @NotNull(message = "Number of days is required")
    @Min(value = 1, message = "Number of days must be at least 1")
    @Max(value = 14, message = "Number of days must not exceed 14")
    @Builder.Default
    Integer numberOfDays = 7;

    @NotNull(message = "Number of people is required")
    @Min(value = 1, message = "Number of people must be at least 1")
    @Max(value = 20, message = "Number of people must not exceed 20")
    @Builder.Default
    Integer numberOfPeople = 2;

    @DecimalMin(value = "0.0", message = "Target budget must be greater than or equal to 0")
    BigDecimal targetBudget;

    @NotEmpty(message = "Meal types must not be empty")
    @Builder.Default
    List<MealType> mealTypes = List.of(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER);

    String goal;

    LocalDate startDate;

    @Builder.Default
    Boolean singleShopPreferred = true;
}
