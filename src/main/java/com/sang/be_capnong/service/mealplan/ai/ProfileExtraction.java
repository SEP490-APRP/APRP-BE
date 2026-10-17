package com.sang.be_capnong.service.mealplan.ai;

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
    @Builder.Default
    List<String> addConditions = new ArrayList<>();

    @JsonProperty("remove_conditions")
    @Builder.Default
    List<String> removeConditions = new ArrayList<>();

    @JsonProperty("add_allergies")
    @Builder.Default
    List<String> addAllergies = new ArrayList<>();

    @JsonProperty("remove_allergies")
    @Builder.Default
    List<String> removeAllergies = new ArrayList<>();

    @JsonProperty("plan_request")
    ExtractedPlanRequest planRequest;

    @JsonProperty("needs_clarification")
    String needsClarification;
}
