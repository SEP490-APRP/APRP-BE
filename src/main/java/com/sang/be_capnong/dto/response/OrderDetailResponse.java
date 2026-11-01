package com.sang.be_capnong.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderDetailResponse {
    int orderDetailId;
    String itemType;   // PRODUCT, MYSTERY_BOX, BUILD_COMBO
    String itemName;   // tên sản phẩm/combo/box
    String imageUrl;   // ảnh đại diện
    Integer quantity;
    BigDecimal unitPrice;
    BigDecimal subtotal; // unitPrice * quantity
    Boolean isReviewed;
}
