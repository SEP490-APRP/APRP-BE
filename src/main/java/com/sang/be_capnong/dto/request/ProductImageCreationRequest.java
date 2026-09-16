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
public class ProductImageCreationRequest {

    @NotBlank(message = "IMAGE_URL_REQUIRED")
    @Size(max = 500, message = "IMAGE_URL_TOO_LONG")
    String imageUrl;

    Boolean isPrimary;

    Integer displayOrder;
}
