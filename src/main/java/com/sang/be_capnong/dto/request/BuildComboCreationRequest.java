package com.sang.be_capnong.dto.request;

import com.sang.be_capnong.enums.MealType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BuildComboCreationRequest {

    @NotBlank(message = "COMBO_NAME_REQUIRED")
    @Size(max = 255, message = "COMBO_NAME_TOO_LONG")
    String comboName;

    @NotNull(message = "COMBO_PRICE_REQUIRED")
    @DecimalMin(value = "0.0", message = "COMBO_PRICE_INVALID")
    BigDecimal price;

    @Size(max = 500, message = "DESCRIPTION_TOO_LONG")
    String description;

    @Size(max = 50, message = "COMBO_TYPE_TOO_LONG")
    String type;

    @NotNull(message = "MEAL_TYPE_REQUIRED")
    MealType mealType;

    @Size(max = 100, message = "COMBO_REGION_TOO_LONG")
    String region;

    @NotNull(message = "COMBO_QUANTITY_REQUIRED")
    @Min(value = 1, message = "COMBO_QUANTITY_INVALID")
    Integer totalQuantity;

    @Size(max = 500, message = "IMAGE_URL_TOO_LONG_COMBO")
    String imageUrl;

    @NotEmpty(message = "COMBO_ITEMS_REQUIRED")
    @Valid
    List<ProductComboItemRequest> items;
}
