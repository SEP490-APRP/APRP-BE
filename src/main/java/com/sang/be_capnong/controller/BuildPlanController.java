package com.sang.be_capnong.controller;

import com.sang.be_capnong.dto.request.*;
import com.sang.be_capnong.dto.response.ApiResponse;
import com.sang.be_capnong.dto.response.BuildPlanResponse;
import com.sang.be_capnong.dto.response.PageResponse;
import com.sang.be_capnong.dto.response.PlanMealResponse;
import com.sang.be_capnong.dto.response.ShoppingListResponse;
import com.sang.be_capnong.enums.PlanStatus;
import com.sang.be_capnong.service.BuildPlanService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/build-plans")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class BuildPlanController {

    BuildPlanService buildPlanService;

    @PostMapping("/generate")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('BUYER')")
    public ApiResponse<BuildPlanResponse> generateAiPlan(@RequestBody @Valid PlanGenerateRequest request) {
        log.info("REST request to generate AI build plan: goal={}, days={}, people={}",
                request.getGoal(), request.getNumberOfDays(), request.getNumberOfPeople());
        return ApiResponse.<BuildPlanResponse>builder()
                .result(buildPlanService.generateAiPlan(request))
                .build();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('BUYER')")
    public ApiResponse<BuildPlanResponse> createManualPlan(@RequestBody @Valid ManualPlanCreateRequest request) {
        log.info("REST request to create manual build plan: {}", request.getPlanName());
        return ApiResponse.<BuildPlanResponse>builder()
                .result(buildPlanService.createManualPlan(request))
                .build();
    }

    @GetMapping
    @PreAuthorize("hasRole('BUYER')")
    public ApiResponse<PageResponse<BuildPlanResponse>> getMyPlans(
            @RequestParam(value = "status", required = false) PlanStatus status,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        log.info("REST request to get buyer plans list, status={}", status);
        return ApiResponse.<PageResponse<BuildPlanResponse>>builder()
                .result(buildPlanService.getMyPlans(status, page, size))
                .build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('BUYER')")
    public ApiResponse<BuildPlanResponse> getPlanById(@PathVariable("id") Integer id) {
        log.info("REST request to get build plan details id: {}", id);
        return ApiResponse.<BuildPlanResponse>builder()
                .result(buildPlanService.getPlanById(id))
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('BUYER')")
    public ApiResponse<BuildPlanResponse> updatePlanHeader(
            @PathVariable("id") Integer id,
            @RequestBody @Valid PlanHeaderUpdateRequest request
    ) {
        log.info("REST request to update build plan header id: {}", id);
        return ApiResponse.<BuildPlanResponse>builder()
                .result(buildPlanService.updatePlanHeader(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('BUYER')")
    public ApiResponse<String> deletePlan(@PathVariable("id") Integer id) {
        log.info("REST request to delete build plan id: {}", id);
        buildPlanService.deletePlan(id);
        return ApiResponse.<String>builder()
                .result("Build plan deleted successfully")
                .build();
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasRole('BUYER')")
    public ApiResponse<BuildPlanResponse> confirmPlan(@PathVariable("id") Integer id) {
        log.info("REST request to confirm build plan id: {}", id);
        return ApiResponse.<BuildPlanResponse>builder()
                .result(buildPlanService.confirmPlan(id))
                .build();
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("hasRole('BUYER')")
    public ApiResponse<BuildPlanResponse> archivePlan(@PathVariable("id") Integer id) {
        log.info("REST request to archive build plan id: {}", id);
        return ApiResponse.<BuildPlanResponse>builder()
                .result(buildPlanService.archivePlan(id))
                .build();
    }

    @PostMapping("/{planId}/meals/{mealId}/items")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('BUYER')")
    public ApiResponse<PlanMealResponse> addMealItem(
            @PathVariable("planId") Integer planId,
            @PathVariable("mealId") Integer mealId,
            @RequestBody @Valid AddMealItemRequest request
    ) {
        log.info("REST request to add item to meal id: {} in plan id: {}", mealId, planId);
        return ApiResponse.<PlanMealResponse>builder()
                .result(buildPlanService.addMealItem(planId, mealId, request))
                .build();
    }

    @PutMapping("/{planId}/meals/{mealId}/items/{itemId}")
    @PreAuthorize("hasRole('BUYER')")
    public ApiResponse<PlanMealResponse> replaceMealItem(
            @PathVariable("planId") Integer planId,
            @PathVariable("mealId") Integer mealId,
            @PathVariable("itemId") Integer itemId,
            @RequestBody @Valid ReplaceMealItemRequest request
    ) {
        log.info("REST request to replace item id: {} in meal id: {}", itemId, mealId);
        return ApiResponse.<PlanMealResponse>builder()
                .result(buildPlanService.replaceMealItem(planId, mealId, itemId, request))
                .build();
    }

    @PatchMapping("/{planId}/meals/{mealId}/items/{itemId}/quantity")
    @PreAuthorize("hasRole('BUYER')")
    public ApiResponse<PlanMealResponse> updateMealItemQuantity(
            @PathVariable("planId") Integer planId,
            @PathVariable("mealId") Integer mealId,
            @PathVariable("itemId") Integer itemId,
            @RequestBody @Valid UpdateMealItemQuantityRequest request
    ) {
        log.info("REST request to update quantity for item id: {} in meal id: {}", itemId, mealId);
        return ApiResponse.<PlanMealResponse>builder()
                .result(buildPlanService.updateMealItemQuantity(planId, mealId, itemId, request.getQuantity()))
                .build();
    }

    @DeleteMapping("/{planId}/meals/{mealId}/items/{itemId}")
    @PreAuthorize("hasRole('BUYER')")
    public ApiResponse<PlanMealResponse> removeMealItem(
            @PathVariable("planId") Integer planId,
            @PathVariable("mealId") Integer mealId,
            @PathVariable("itemId") Integer itemId
    ) {
        log.info("REST request to remove item id: {} from meal id: {}", itemId, mealId);
        return ApiResponse.<PlanMealResponse>builder()
                .result(buildPlanService.removeMealItem(planId, mealId, itemId))
                .build();
    }

    @GetMapping("/{id}/shopping-list")
    @PreAuthorize("hasRole('BUYER')")
    public ApiResponse<ShoppingListResponse> getShoppingList(@PathVariable("id") Integer id) {
        log.info("REST request to get shopping list for build plan id: {}", id);
        return ApiResponse.<ShoppingListResponse>builder()
                .result(buildPlanService.getShoppingList(id))
                .build();
    }

    @PostMapping("/{id}/add-to-cart")
    @PreAuthorize("hasRole('BUYER')")
    public ApiResponse<ShoppingListResponse> addPlanToCart(@PathVariable("id") Integer id) {
        log.info("REST request to add plan items to cart for build plan id: {}", id);
        return ApiResponse.<ShoppingListResponse>builder()
                .result(buildPlanService.addPlanToCart(id))
                .build();
    }

    @PostMapping("/{planId}/meals/{mealId}/regenerate")
    @PreAuthorize("hasRole('BUYER')")
    public ApiResponse<PlanMealResponse> regenerateMeal(
            @PathVariable("planId") Integer planId,
            @PathVariable("mealId") Integer mealId
    ) {
        log.info("REST request to regenerate meal id: {} in plan id: {}", mealId, planId);
        return ApiResponse.<PlanMealResponse>builder()
                .result(buildPlanService.regenerateMeal(planId, mealId))
                .build();
    }
}
