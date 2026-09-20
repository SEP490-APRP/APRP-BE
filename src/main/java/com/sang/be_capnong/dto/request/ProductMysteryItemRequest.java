package com.sang.be_capnong.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductMysteryItemRequest {

    @NotNull(message = "PRODUCT_ID_REQUIRED")
    Integer productId;

    @NotNull(message = "PRODUCT_QUANTITY_REQUIRED")
    @Min(value = 1, message = "PRODUCT_QUANTITY_INVALID")
    Integer productQuantity;
}
