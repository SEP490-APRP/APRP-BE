package com.sang.be_capnong.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sang.be_capnong.enums.BlogCategory;
import com.sang.be_capnong.enums.BlogStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BlogResponse {

    int blogId;
    String title;
    String content;
    String pictureUrl;
    BlogStatus status;
    BlogCategory category;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime createAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime updatedAt;
    Integer views;

    // Thông tin shop
    int shopId;
    String shopName;
    String shopAvatar;

    // Thống kê tương tác
    long likeCount;
    long commentCount;

    // Trạng thái tương tác của user hiện tại (nếu đã đăng nhập)
    Boolean isLikedByCurrentUser;
}
