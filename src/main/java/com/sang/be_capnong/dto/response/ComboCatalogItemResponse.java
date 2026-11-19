package com.sang.be_capnong.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ComboCatalogItemResponse {
    String id;
    Integer realId;
    String itemType;
    String name;
    BigDecimal price;
    BigDecimal originalPrice;
    Integer discountPercent;
    String unit;
    String description;
    String imageUrl;
    String category;
    String badgeHighlight;
    Integer soldQuantity;
    Integer stockQuantity;
    String region;
    ShopSummaryResponse shop;
}
