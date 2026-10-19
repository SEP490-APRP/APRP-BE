package com.sang.be_capnong.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChatMessageRequest {

    @NotNull(message = "RECEIVER_REQUIRED")
    Integer receiverId;

    String content;

    @Builder.Default
    String type = "TEXT";

    String attachmentUrl;
    Integer referenceId;
}
