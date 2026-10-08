package com.sang.be_capnong.dto.request;

import com.sang.be_capnong.enums.BlogCategory;
import com.sang.be_capnong.enums.BlogStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BlogCreateRequest {

    @NotBlank(message = "Tiêu đề bài viết không được để trống")
    String title;

    @NotBlank(message = "Nội dung bài viết không được để trống")
    String content;

    String pictureUrl;

    @NotNull(message = "Trạng thái bài viết không được để trống")
    BlogStatus status;

    BlogCategory category;
}
