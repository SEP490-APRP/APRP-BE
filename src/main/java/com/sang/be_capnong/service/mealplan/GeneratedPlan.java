package com.sang.be_capnong.service.mealplan;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GeneratedPlan {

    List<GeneratedDay> days;
    String notes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class GeneratedDay {
        @JsonProperty("day_index")
        Integer dayIndex;
        List<GeneratedMeal> meals;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class GeneratedMeal {
        @JsonProperty("meal_type")
        String mealType;
        List<GeneratedItem> items;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class GeneratedItem {
        @JsonProperty("combo_id")
        Integer comboId;
        Integer quantity;
    }
}
