package com.sang.be_capnong.service.mealplan;

import com.sang.be_capnong.entity.Allergen;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

@Component
public class PlanValidator {

    public ValidationResult validate(
            GeneratedPlan plan,
            Map<Integer, CandidateComboDto> candidateMap,
            int expectedDays,
            BigDecimal targetBudget,
            List<Allergen> buyerAllergies
    ) {
        ValidationResult result = ValidationResult.builder().valid(true).build();

        if (plan == null) {
            result.addError("Generated plan is null");
            return result;
        }

        if (plan.getDays() == null || plan.getDays().isEmpty()) {
            result.addError("Generated plan has no days");
            return result;
        }

        if (plan.getDays().size() != expectedDays) {
            result.addWarning(String.format("Expected %d days but plan has %d days", expectedDays, plan.getDays().size()));
        }

        Set<String> buyerAllergenNames = new HashSet<>();
        if (buyerAllergies != null) {
            for (Allergen a : buyerAllergies) {
                if (a != null && a.getName() != null) {
                    buyerAllergenNames.add(a.getName().toLowerCase().trim());
                }
            }
        }

        BigDecimal totalCost = BigDecimal.ZERO;
        Set<Integer> distinctCombos = new HashSet<>();
        int totalItemsCount = 0;

        for (GeneratedPlan.GeneratedDay day : plan.getDays()) {
            if (day.getMeals() == null || day.getMeals().isEmpty()) {
                result.addError(String.format("Day %s has no meal slots", day.getDayIndex()));
                continue;
            }

            for (GeneratedPlan.GeneratedMeal meal : day.getMeals()) {
                if (meal.getItems() == null || meal.getItems().isEmpty()) {
                    result.addError(String.format("Day %s meal %s has no items", day.getDayIndex(), meal.getMealType()));
                    continue;
                }

                String slotMealType = meal.getMealType() != null ? meal.getMealType().toUpperCase().trim() : "";

                for (GeneratedPlan.GeneratedItem item : meal.getItems()) {
                    totalItemsCount++;
                    Integer comboId = item.getComboId();
                    Integer quantity = item.getQuantity();

                    if (comboId == null) {
                        result.addError("Item is missing combo_id");
                        continue;
                    }

                    CandidateComboDto candidate = candidateMap.get(comboId);
                    if (candidate == null) {
                        result.addError(String.format("Combo ID %d is not among approved candidate combos", comboId));
                        continue;
                    }

                    distinctCombos.add(comboId);

                    // Validate mealType match
                    if (candidate.getMealType() != null && !slotMealType.isEmpty()) {
                        if (!candidate.getMealType().name().equalsIgnoreCase(slotMealType)) {
                            result.addError(String.format("Combo %s (%s) does not match meal slot %s",
                                    candidate.getName(), candidate.getMealType().name(), slotMealType));
                        }
                    }

                    // Validate quantity
                    if (quantity == null || quantity < 1) {
                        result.addError(String.format("Invalid quantity %s for combo %d", quantity, comboId));
                        continue;
                    }

                    if (quantity > candidate.getStockLeft()) {
                        result.addError(String.format("Quantity %d for combo %s exceeds remaining stock (%d)",
                                quantity, candidate.getName(), candidate.getStockLeft()));
                    }

                    // Re-check allergen conflict (Rule 1)
                    if (candidate.getAllergens() != null) {
                        for (String aName : candidate.getAllergens()) {
                            if (aName != null && buyerAllergenNames.contains(aName.toLowerCase().trim())) {
                                result.addError(String.format("Combo %s contains allergen '%s' registered in buyer profile",
                                        candidate.getName(), aName));
                            }
                        }
                    }

                    if (candidate.getPrice() != null) {
                        totalCost = totalCost.add(candidate.getPrice().multiply(BigDecimal.valueOf(quantity)));
                    }
                }
            }
        }

        result.setComputedTotalCost(totalCost);

        // Rule 5: Budget check
        if (targetBudget != null && targetBudget.compareTo(BigDecimal.ZERO) > 0) {
            if (totalCost.compareTo(targetBudget) > 0) {
                result.addWarning(String.format("Total plan cost (%s đ) exceeds target budget (%s đ)",
                        totalCost.toPlainString(), targetBudget.toPlainString()));
            }
        }

        // Diversity warning
        if (distinctCombos.size() < 2 && totalItemsCount > 3) {
            result.addWarning("Plan has very low variety (only 1 combo repeated across all meals)");
        }

        return result;
    }
}
