package com.sang.be_capnong.dto.request;

import com.sang.be_capnong.enums.ReactionType;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReviewReactionRequest {
    @NotNull(message = "Reaction type is required")
    ReactionType reactionType;
}
