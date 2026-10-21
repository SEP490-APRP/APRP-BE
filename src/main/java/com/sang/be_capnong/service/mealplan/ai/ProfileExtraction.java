package com.sang.be_capnong.service.mealplan.ai;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProfileExtraction {

    @JsonProperty("add_conditions")
    @JsonAlias({"addConditions", "add_conditions"})
    @Builder.Default
    List<String> addConditions = new ArrayList<>();

    @JsonProperty("remove_conditions")
    @JsonAlias({"removeConditions", "remove_conditions"})
    @Builder.Default
    List<String> removeConditions = new ArrayList<>();

    @JsonProperty("add_allergies")
    @JsonAlias({"addAllergies", "add_allergies"})
    @Builder.Default
    List<String> addAllergies = new ArrayList<>();

    @JsonProperty("remove_allergies")
    @JsonAlias({"removeAllergies", "remove_allergies"})
    @Builder.Default
    List<String> removeAllergies = new ArrayList<>();

    @JsonProperty("diet_preference")
    @JsonAlias({"dietPreference", "diet_preference"})
    String dietPreference;

    @JsonProperty("advice")
    @JsonAlias({"advice"})
    String advice;

    @JsonProperty("notes")
    @JsonAlias({"notes"})
    String notes;

    @JsonProperty("suggested_options")
    @JsonAlias({"suggestedOptions", "suggested_options"})
    @Builder.Default
    List<String> suggestedOptions = new ArrayList<>();

    @JsonProperty("plan_request")
    @JsonAlias({"planRequest", "plan_request"})
    ExtractedPlanRequest planRequest;

    @JsonProperty("needs_clarification")
    @JsonAlias({"needsClarification", "needs_clarification"})
    String needsClarification;
}
