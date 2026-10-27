package com.sang.be_capnong.dto.request;

import com.sang.be_capnong.enums.DietPreference;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ManualHealthProfileUpdateRequest {
    DietPreference dietPreference;
    List<Integer> allergenIds;
    List<Integer> conditionIds;
}
