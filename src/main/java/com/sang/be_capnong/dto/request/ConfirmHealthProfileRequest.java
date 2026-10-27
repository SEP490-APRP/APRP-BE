package com.sang.be_capnong.dto.request;

import com.sang.be_capnong.service.mealplan.ai.ProfileExtraction;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ConfirmHealthProfileRequest {
    ProfileExtraction diff; // Optional override by user before confirming
}
