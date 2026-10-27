package com.sang.be_capnong.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PlanCopilotChatRequest {

    @NotBlank(message = "Tin nhắn không được để trống")
    String message;

    List<ChatMessageDto> history;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ChatMessageDto {
        String role; // "user" or "assistant"
        String content;
    }
}
