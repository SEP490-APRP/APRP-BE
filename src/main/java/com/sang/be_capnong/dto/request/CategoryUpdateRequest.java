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
public class CategoryUpdateRequest {

    @NotBlank(message = "CATEGORY_NAME_REQUIRED")
    @Size(max = 100, message = "CATEGORY_NAME_TOO_LONG")
    String name;

    @Size(max = 500, message = "CATEGORY_DESCRIPTION_TOO_LONG")
    String description;
}
