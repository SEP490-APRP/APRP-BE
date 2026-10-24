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
public class AllergenCreationRequest {
    @NotBlank(message = "Allergen name is required")
    @Size(max = 100, message = "Allergen name must not exceed 100 characters")
    String name;
}
