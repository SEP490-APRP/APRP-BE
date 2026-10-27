package com.sang.be_capnong.controller;

import com.sang.be_capnong.dto.request.ToggleMealCompletedRequest;
import com.sang.be_capnong.dto.response.ApiResponse;
import com.sang.be_capnong.dto.response.PlanMealResponse;
import com.sang.be_capnong.service.BuildPlanService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/plan-meals")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class PlanMealController {

    BuildPlanService buildPlanService;

    @PatchMapping("/{id}/completed")
    @PreAuthorize("hasRole('BUYER')")
    public ApiResponse<PlanMealResponse> toggleMealCompleted(
            @PathVariable("id") Integer id,
            @RequestBody @Valid ToggleMealCompletedRequest request
    ) {
        log.info("REST request to toggle meal completed for id: {}, completed={}", id, request.getCompleted());
        return ApiResponse.<PlanMealResponse>builder()
                .result(buildPlanService.toggleMealCompleted(id, request.getCompleted()))
                .build();
    }
}
