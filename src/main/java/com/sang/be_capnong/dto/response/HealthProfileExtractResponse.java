package com.sang.be_capnong.dto.response;

import com.sang.be_capnong.service.mealplan.ai.ExtractedPlanRequest;
import com.sang.be_capnong.service.mealplan.ai.ProfileExtraction;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HealthProfileExtractResponse {
    int requestId;
    ProfileExtraction diff;
    UserHealthProfileResponse currentProfile;
    ExtractedPlanRequest planRequest;
    String needsClarification;
}
