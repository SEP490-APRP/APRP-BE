package com.sang.be_capnong.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AiDescriptionSuggestionRequest {

    @NotBlank(message = "PRODUCT_NAME_REQUIRED")
    @Size(max = 255, message = "PRODUCT_NAME_TOO_LONG")
    String productName;

    String categoryName;

    String origin;

    String imageUrl;
}
