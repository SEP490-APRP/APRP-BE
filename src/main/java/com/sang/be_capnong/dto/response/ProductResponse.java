package com.sang.be_capnong.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductResponse {
    int productId;
    String productName;
    String unit;
    BigDecimal sellingPrice;
    Integer stockQuantity;
    String description;
    String status;
    LocalDate expiryDate;
    Integer discountPercent;
    LocalDateTime discountStartDate;
    LocalDateTime discountEndDate;
    CategoryResponse category;
    ShopSummaryResponse shop;
    List<ProductImageResponse> images;
}
