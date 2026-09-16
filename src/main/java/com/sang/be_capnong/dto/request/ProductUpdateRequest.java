package com.sang.be_capnong.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductUpdateRequest {

    @NotBlank(message = "PRODUCT_NAME_REQUIRED")
    @Size(max = 255, message = "PRODUCT_NAME_TOO_LONG")
    String productName;

    @NotNull(message = "PRODUCT_CATEGORY_REQUIRED")
    Integer categoryId;

    @NotBlank(message = "PRODUCT_UNIT_REQUIRED")
    String unit;

    @NotNull(message = "PRODUCT_PRICE_REQUIRED")
    @DecimalMin(value = "0.0", message = "PRODUCT_PRICE_INVALID")
    BigDecimal sellingPrice;

    @NotNull(message = "PRODUCT_STOCK_REQUIRED")
    @Min(value = 0, message = "PRODUCT_STOCK_INVALID")
    Integer stockQuantity;

    String description;

    String status;

    @NotNull(message = "PRODUCT_EXPIRY_REQUIRED")
    LocalDate expiryDate;

    @Min(value = 0, message = "DISCOUNT_PERCENT_INVALID")
    @Max(value = 100, message = "DISCOUNT_PERCENT_INVALID")
    Integer discountPercent;

    LocalDateTime discountStartDate;
    LocalDateTime discountEndDate;
}
