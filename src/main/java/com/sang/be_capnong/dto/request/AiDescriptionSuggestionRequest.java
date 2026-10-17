package com.sang.be_capnong.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AiDescriptionSuggestionRequest {

    @NotBlank(message = "PRODUCT_NAME_REQUIRED")
    @Size(max = 255, message = "PRODUCT_NAME_TOO_LONG")
    String productName;

    /**
     * Loai hinh san pham:
     * - "SINGLE" (mac dinh: nong san don le)
     * - "MYSTERY_BOX" (tui mu / hop nong san bi an)
     * - "COMBO" (combo mon an / mam com gia dinh)
     */
    @Size(max = 50, message = "Product type must not exceed 50 characters")
    String productType;

    /**
     * Loai bua an khi productType la "COMBO":
     * FAMILY_DINNER, HOTPOT, BBQ, VEGETARIAN, etc.
     */
    @Size(max = 50, message = "Meal type must not exceed 50 characters")
    String mealType;

    /**
     * Danh sach ten cac nong san / nguyen lieu thanh phan (cho Combo va Tui mu).
     */
    @Size(max = 30, message = "Included items list must not exceed 30 items")
    List<@Size(max = 100, message = "Item name must not exceed 100 characters") String> includedItems;

    @Size(max = 100, message = "Category name must not exceed 100 characters")
    String categoryName;

    @Size(max = 150, message = "Origin must not exceed 150 characters")
    String origin;

    @Size(max = 500, message = "Image URL must not exceed 500 characters")
    String imageUrl;

    @Size(max = 1000, message = "Current description must not exceed 1000 characters")
    String currentDescription;
}
