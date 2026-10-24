package com.sang.be_capnong.service.mealplan.ai;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExtractionInput {
    String text;
    List<String> knownAllergens;
    List<String> knownConditions;
    List<String> currentAllergies;
    List<String> currentConditions;
}
