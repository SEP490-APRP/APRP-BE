package com.sang.be_capnong.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonProperty("allergens")
    public List<AllergenResponse> getAllergens() {
        return allergies;
    }

    @JsonProperty("healthConditions")
    public List<HealthConditionResponse> getHealthConditions() {
        return conditions;
    }
}
