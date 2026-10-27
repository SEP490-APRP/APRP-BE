package com.sang.be_capnong.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ToggleMealCompletedRequest {

    @NotNull(message = "Completed status is required")
    Boolean completed;
}
