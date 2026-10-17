package com.sang.be_capnong.service;

import com.sang.be_capnong.dto.request.*;
import com.sang.be_capnong.dto.response.BuildPlanResponse;
import com.sang.be_capnong.dto.response.PageResponse;
import com.sang.be_capnong.dto.response.PlanMealResponse;
import com.sang.be_capnong.enums.PlanStatus;

public interface BuildPlanService {
    BuildPlanResponse generateAiPlan(PlanGenerateRequest request);
    BuildPlanResponse createManualPlan(ManualPlanCreateRequest request);
    BuildPlanResponse getPlanById(int planId);
    PageResponse<BuildPlanResponse> getMyPlans(PlanStatus status, int page, int size);
    BuildPlanResponse updatePlanHeader(int planId, PlanHeaderUpdateRequest request);
    void deletePlan(int planId);
    BuildPlanResponse confirmPlan(int planId);
    BuildPlanResponse archivePlan(int planId);
    PlanMealResponse toggleMealCompleted(int mealId, boolean completed);

    PlanMealResponse addMealItem(int planId, int mealId, AddMealItemRequest request);
    PlanMealResponse replaceMealItem(int planId, int mealId, int itemId, ReplaceMealItemRequest request);
    PlanMealResponse removeMealItem(int planId, int mealId, int itemId);
    PlanMealResponse updateMealItemQuantity(int planId, int mealId, int itemId, int quantity);

    com.sang.be_capnong.dto.response.ShoppingListResponse getShoppingList(int planId);
    com.sang.be_capnong.dto.response.ShoppingListResponse addPlanToCart(int planId);
    PlanMealResponse regenerateMeal(int planId, int mealId);
}
