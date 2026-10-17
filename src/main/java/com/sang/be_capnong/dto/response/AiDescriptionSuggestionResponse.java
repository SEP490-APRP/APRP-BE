package com.sang.be_capnong.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AiDescriptionSuggestionResponse {
    String suggestedDescription;
    List<String> keyHighlights;
    String generatedSource;
}
