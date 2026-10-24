package com.sang.be_capnong.service.mealplan.ai;

import com.sang.be_capnong.service.mealplan.GeneratedPlan;

public interface MealPlanAiClient {
    ProfileExtraction extractProfile(ExtractionInput in);
    GeneratedPlan generatePlan(GenerationInput in);
}
