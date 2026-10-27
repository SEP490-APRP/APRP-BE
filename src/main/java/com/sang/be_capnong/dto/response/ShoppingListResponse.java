package com.sang.be_capnong.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShoppingListResponse {
    int planId;
    String planName;
    BigDecimal totalEstimatedPrice;
    int totalCombosCount;
    boolean allInStock;
    List<ShoppingListItemResponse> items;
}
