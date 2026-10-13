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
    String productType;

    /**
     * Loai bua an khi productType la "COMBO":
     * FAMILY_DINNER, HOTPOT, BBQ, VEGETARIAN, etc.
     */
    String mealType;

    /**
     * Danh sach ten cac nong san / nguyen lieu thanh phan (cho Combo va Tui mu).
     */
    List<String> includedItems;

    String categoryName;

    String origin;

    String imageUrl;

    String currentDescription;
}
