package com.sang.be_capnong.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductComboResponse {

    Integer productId;
    String productName;
    String unit;
    Integer productQuantity;
    BigDecimal sellingPrice;
    String imageUrl;
}
