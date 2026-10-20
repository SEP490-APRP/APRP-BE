package com.sang.be_capnong.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

    @Size(max = 2000, message = "Nội dung tin nhắn không được vượt quá 2000 ký tự")
    String content;

    @Builder.Default
    String type = "TEXT";

    String attachmentUrl;
    Integer referenceId;
}
