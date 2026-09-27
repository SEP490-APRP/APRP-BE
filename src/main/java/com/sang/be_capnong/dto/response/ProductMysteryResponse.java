package com.sang.be_capnong.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductMysteryResponse {
    int productMysteryId;
    int productId;
    String productName;
    String unit;
    String imageUrl;
    Integer productQuantity;
}
