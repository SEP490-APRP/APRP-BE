package com.sang.be_capnong.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShoppingListItemResponse {
    int comboId;
    String comboName;
    String imageUrl;
    Integer shopId;
    String shopName;
    String region;
    String regionDisplayName;
    String comboType;
    String cookingSuggestion;
    int totalQuantity;
    BigDecimal unitPrice;
    BigDecimal subtotal;
    int stockAvailable;
    boolean inStock;
}
