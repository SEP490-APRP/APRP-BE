package com.sang.be_capnong.dto.request;

import com.sang.be_capnong.enums.AiItemType;
import com.sang.be_capnong.enums.ChatbotClient;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChatbotMessageRequest {

    @Size(max = 2000, message = "Tin nhắn không được vượt quá 2000 ký tự")
    String message;

    String sessionId;

    Integer selectedOptionId;
    AiItemType selectedOptionType;
    Integer selectedQuantity;

    /** Không gửi thì coi là WEB, để client cũ vẫn chạy như trước. */
    ChatbotClient client;
}
