package com.sang.be_capnong.mapper;

import com.sang.be_capnong.dto.response.*;
import com.sang.be_capnong.entity.*;
import org.mapstruct.Mapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring")
public interface BuildPlanMapper {

    default BuildPlanResponse toBuildPlanResponse(BuildPlan plan) {
        if (plan == null) return null;

        BigDecimal totalCost = BigDecimal.ZERO;
        List<PlanDayResponse> dayResponses = new ArrayList<>();

        if (plan.getPlanDays() != null) {
            for (PlanDay day : plan.getPlanDays()) {
                PlanDayResponse dayResponse = toPlanDayResponse(day);
                dayResponses.add(dayResponse);
                if (dayResponse.getDayCost() != null) {
                    totalCost = totalCost.add(dayResponse.getDayCost());
                }
            }
        }

        return BuildPlanResponse.builder()
                .planId(plan.getPlanId())
                .buyerId(plan.getBuyer() != null ? plan.getBuyer().getId() : null)
                .planName(plan.getPlanName())
                .numberOfDays(plan.getNumberOfDays())
                .numberOfPeople(plan.getNumberOfPeople())
                .mealType(plan.getMealType())
                .targetBudget(plan.getTargetBudget())
                .totalCost(totalCost)
                .status(plan.getStatus())
                .source(plan.getSource())
                .createdAt(plan.getCreatedAt())
                .updatedAt(plan.getUpdatedAt())
                .planDays(dayResponses)
                .build();
    }

    default PlanDayResponse toPlanDayResponse(PlanDay day) {
        if (day == null) return null;

        BigDecimal dayCost = BigDecimal.ZERO;
        List<PlanMealResponse> mealResponses = new ArrayList<>();

        if (day.getPlanMeals() != null) {
            for (PlanMeal meal : day.getPlanMeals()) {
                PlanMealResponse mealResponse = toPlanMealResponse(meal);
                mealResponses.add(mealResponse);
                if (mealResponse.getPlanMealItems() != null) {
                    for (PlanMealItemResponse item : mealResponse.getPlanMealItems()) {
                        if (item.getItemSubtotal() != null) {
                            dayCost = dayCost.add(item.getItemSubtotal());
                        }
                    }
                }
            }
        }

        return PlanDayResponse.builder()
                .id(day.getId())
                .dayIndex(day.getDayIndex())
                .date(day.getDate())
                .dayCost(dayCost)
                .planMeals(mealResponses)
                .build();
    }

    default PlanMealResponse toPlanMealResponse(PlanMeal meal) {
        if (meal == null) return null;

        List<PlanMealItemResponse> itemResponses = new ArrayList<>();
        if (meal.getPlanMealItems() != null) {
            for (PlanMealItem item : meal.getPlanMealItems()) {
                itemResponses.add(toPlanMealItemResponse(item));
            }
        }

        return PlanMealResponse.builder()
                .id(meal.getId())
                .mealType(meal.getMealType())
                .sortOrder(meal.getSortOrder())
                .completed(meal.getCompleted())
                .planMealItems(itemResponses)
                .build();
    }

    default PlanMealItemResponse toPlanMealItemResponse(PlanMealItem item) {
        if (item == null) return null;

        BigDecimal subtotal = BigDecimal.ZERO;
        if (item.getUnitPriceSnapshot() != null && item.getQuantity() != null) {
            subtotal = item.getUnitPriceSnapshot().multiply(BigDecimal.valueOf(item.getQuantity()));
        }

        return PlanMealItemResponse.builder()
                .id(item.getId())
                .quantity(item.getQuantity())
                .unitPriceSnapshot(item.getUnitPriceSnapshot())
                .itemSubtotal(subtotal)
                .isAiSuggested(item.getIsAiSuggested())
                .position(item.getPosition())
                .userOverride(item.getUserOverride())
                .combo(toBuildComboSummaryResponse(item.getBuildCombo()))
                .build();
    }

    default BuildComboSummaryResponse toBuildComboSummaryResponse(BuildCombo combo) {
        if (combo == null) return null;

        int total = combo.getTotalQuantity() != null ? combo.getTotalQuantity() : 0;
        int sold = combo.getSoldQuantity() != null ? combo.getSoldQuantity() : 0;
        int remaining = Math.max(0, total - sold);

        return BuildComboSummaryResponse.builder()
                .comboId(combo.getComboId())
                .comboName(combo.getComboName())
                .price(combo.getPrice())
                .mealType(combo.getMealType())
                .type(combo.getType())
                .imageUrl(combo.getImageUrl())
                .shopId(combo.getShop() != null ? combo.getShop().getId() : null)
                .shopName(combo.getShop() != null ? combo.getShop().getShopName() : null)
                .remainingStock(remaining)
                .build();
    }
}
