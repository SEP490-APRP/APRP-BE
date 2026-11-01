package com.sang.be_capnong.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BotKnowledgeResponse {
    int id;
    String type;
    String title;
    String content;
    String keywords;
    Boolean active;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
