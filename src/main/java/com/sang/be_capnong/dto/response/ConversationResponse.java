package com.sang.be_capnong.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
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

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime lastMessageTime;

    long unreadCount;
}
