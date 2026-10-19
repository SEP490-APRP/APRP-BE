package com.sang.be_capnong.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChatMessageResponse {

    int id;
    int conversationId;
    int senderId;
    String senderName;
    String senderAvatar;
    int receiverId;
    String content;
    String type;
    String attachmentUrl;
    Integer referenceId;
    boolean isRead;
    LocalDateTime sentAt;
}
