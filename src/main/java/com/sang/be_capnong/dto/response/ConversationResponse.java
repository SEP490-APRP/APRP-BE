package com.sang.be_capnong.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ConversationResponse {

    int id;
    String roomKey;
    int otherUserId;
    String otherUserName;
    String otherUserAvatar;
    String otherUserRole;
    String lastMessage;
    LocalDateTime lastMessageTime;
    long unreadCount;
}
