package com.sang.be_capnong.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
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

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    LocalDate expiryDate;

    String rejectReason;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime updatedAt;
    Integer discountPercent;
    LocalDateTime discountStartDate;
    LocalDateTime discountEndDate;
    CategoryResponse category;
    ShopSummaryResponse shop;
    List<ProductImageResponse> images;
    ModerationResultResponse moderationResult;
}
