package com.sang.be_capnong.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MysteryBoxUpdateRequest {

    @Size(max = 50, message = "BOX_TYPE_TOO_LONG")
    String boxType;

    @Size(max = 500, message = "IMAGE_URL_TOO_LONG")
    String imageUrl;

    @DecimalMin(value = "0.0", message = "MYSTERY_BOX_PRICE_INVALID")
    BigDecimal price;

    @Size(max = 500, message = "DESCRIPTION_TOO_LONG")
    String description;

    @Size(max = 255, message = "NOTE_TOO_LONG")
    String note;

    @Min(value = 1, message = "MYSTERY_BOX_QUANTITY_INVALID")
    Integer totalQuantity;

    Boolean isActive;
}
