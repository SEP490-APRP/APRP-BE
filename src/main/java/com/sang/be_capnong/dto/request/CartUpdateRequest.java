package com.sang.be_capnong.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import jakarta.validation.constraints.Max;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CartUpdateRequest {

    @NotNull(message = "Cart item ID is required")
    Integer cartItemId;

    @NotNull(message = "Quantity is required")
    @Min(value = 0, message = "Quantity must be at least 0 (0 to remove)")
    @Max(value = 999, message = "Max quantity allowed per item is 999")
    Integer quantity;
}
