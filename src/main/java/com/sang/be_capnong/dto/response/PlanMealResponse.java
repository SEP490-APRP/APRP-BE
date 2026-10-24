package com.sang.be_capnong.dto.response;

import com.sang.be_capnong.enums.MealType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PlanMealResponse {
    int id;
    MealType mealType;
    Integer sortOrder;
    Boolean completed;
    List<PlanMealItemResponse> planMealItems;
}
