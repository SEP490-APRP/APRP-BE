package com.sang.be_capnong.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReplaceMealItemRequest {

    @NotNull(message = "New combo ID is required")
    Integer newComboId;

    @Min(value = 1, message = "Quantity must be at least 1")
    Integer quantity; // Optional, defaults to old quantity if null
}
