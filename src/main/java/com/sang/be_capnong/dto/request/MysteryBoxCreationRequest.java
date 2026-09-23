package com.sang.be_capnong.dto.request;

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
public class MysteryBoxCreationRequest {

    @NotBlank(message = "BOX_TYPE_REQUIRED")
    @Size(max = 50, message = "BOX_TYPE_TOO_LONG")
    String boxType;

    @Size(max = 500, message = "IMAGE_URL_TOO_LONG")
    String imageUrl;

    @NotNull(message = "MYSTERY_BOX_PRICE_REQUIRED")
    @DecimalMin(value = "0.0", message = "MYSTERY_BOX_PRICE_INVALID")
    BigDecimal price;

    @Size(max = 500, message = "DESCRIPTION_TOO_LONG")
    String description;

    @Size(max = 255, message = "NOTE_TOO_LONG")
    String note;

    @NotNull(message = "MYSTERY_BOX_QUANTITY_REQUIRED")
    @Min(value = 1, message = "MYSTERY_BOX_QUANTITY_INVALID")
    Integer totalQuantity;

    @NotEmpty(message = "MYSTERY_BOX_ITEMS_REQUIRED")
    @Valid
    List<ProductMysteryItemRequest> items;
}
