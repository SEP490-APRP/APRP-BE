package com.sang.be_capnong.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import jakarta.validation.constraints.Max;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CartAddRequest {

    @NotBlank(message = "Item type is required (PRODUCT, MYSTERY_BOX, BUILD_COMBO)")
    String itemType;

    @NotNull(message = "Item ID is required")
    Integer itemId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Max(value = 999, message = "Max quantity allowed per item is 999")
    Integer quantity;
}
