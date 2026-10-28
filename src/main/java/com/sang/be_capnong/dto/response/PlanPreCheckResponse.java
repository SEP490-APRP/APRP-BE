package com.sang.be_capnong.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PlanPreCheckResponse {

    Boolean isFeasible;
    Boolean hasPortionWarning;
    Boolean hasBudgetWarning;
    Boolean hasStockShortage;
    String stockShortageMessage;

    Integer numberOfDays;
    Integer numberOfPeople;
    BigDecimal targetBudget;

    BigDecimal estimatedMinCost;
    BigDecimal estimatedStandardCost;
    Integer combosPerMeal;
    Integer totalMeals;
    Integer totalCombosNeeded;

    String portionAdvice;
    String budgetAdvice;

    List<ScenarioOption> suggestedScenarios;
    List<ScenarioOption> scenarios;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ScenarioOption {
        String scenarioId;       // OPTIMIZE_MEALS | REDUCE_DAYS | REALISTIC_BUDGET
        String scenarioKey;
        String title;            // Tên phương án
        String description;      // Mô tả giải pháp
        BigDecimal estimatedCost;
        Integer adjustedDays;
        Integer suggestedDays;
        Integer adjustedPeople;
        List<String> adjustedMealTypes;
        List<String> suggestedMealTypes;
        BigDecimal adjustedBudget;
        BigDecimal suggestedBudget;
    }
}
