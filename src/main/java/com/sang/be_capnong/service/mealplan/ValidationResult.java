package com.sang.be_capnong.service.mealplan;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ValidationResult {
    boolean valid;

    @Builder.Default
    List<String> errors = new ArrayList<>();

    @Builder.Default
    List<String> warnings = new ArrayList<>();

    @Builder.Default
    BigDecimal computedTotalCost = BigDecimal.ZERO;

    public void addError(String error) {
        if (errors == null) errors = new ArrayList<>();
        errors.add(error);
        valid = false;
    }

    public void addWarning(String warning) {
        if (warnings == null) warnings = new ArrayList<>();
        warnings.add(warning);
    }
}
