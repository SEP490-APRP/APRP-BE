package com.sang.be_capnong.dto.request;

import com.sang.be_capnong.enums.BlogCategory;
import com.sang.be_capnong.enums.BlogStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BlogUpdateRequest {

    String title;
    String content;
    String pictureUrl;
    BlogStatus status;
    BlogCategory category;
}
