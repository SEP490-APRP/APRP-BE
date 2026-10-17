package com.sang.be_capnong.dto.response;

import com.sang.be_capnong.enums.DietPreference;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserHealthProfileResponse {
    Integer buyerId;
    DietPreference dietPreference;
    List<AllergenResponse> allergies;
    List<HealthConditionResponse> conditions;
}
