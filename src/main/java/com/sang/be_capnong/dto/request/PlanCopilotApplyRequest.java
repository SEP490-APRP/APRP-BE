package com.sang.be_capnong.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PlanCopilotApplyRequest {

    @NotBlank(message = "actionType is required")
    String actionType; // "REPLACE_COMBO", "UPDATE_QUANTITY"

    @NotNull(message = "targetMealId is required")
    Integer targetMealId;

    @NotNull(message = "targetItemId is required")
    Integer targetItemId;

    Integer newComboId;
    Integer quantity;
}
