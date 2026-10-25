package com.sang.be_capnong.service.mealplan.ai;

import com.sang.be_capnong.enums.DietPreference;
import com.sang.be_capnong.enums.MealType;
import com.sang.be_capnong.service.mealplan.CandidateComboDto;
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
public class GenerationInput {

    Constraints constraints;
    BuyerContext buyer;
    List<CandidateComboDto> candidates;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Constraints {
        int numberOfDays;
        int numberOfPeople;
        BigDecimal targetBudget;
        List<MealType> mealTypes;
        String goal;
        LocalDate startDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class BuyerContext {
        DietPreference dietPreference;
        List<String> conditions;
        List<String> allergies;
        List<Integer> recentComboIds;
    }
}
