package com.sang.be_capnong.dto.request;

import com.sang.be_capnong.enums.MealType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PlanPreCheckRequest {

    @NotNull(message = "Số ngày là bắt buộc")
    @Min(value = 1, message = "Số ngày tối thiểu là 1")
    @Max(value = 14, message = "Số ngày tối đa là 14")
    @Builder.Default
    Integer numberOfDays = 7;

    @NotNull(message = "Số người là bắt buộc")
    @Min(value = 1, message = "Số người tối thiểu là 1")
    @Max(value = 20, message = "Số người tối đa là 20")
    @Builder.Default
    Integer numberOfPeople = 2;

    BigDecimal targetBudget;
    List<MealType> mealTypes;
    String goal;
}
