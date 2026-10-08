package com.sang.be_capnong.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BlogCommentResponse {

    int commentId;
    String content;
    LocalDateTime createAt;

    // Thông tin người bình luận
    int userId;
    String userName;
    String userAvatar;
}
