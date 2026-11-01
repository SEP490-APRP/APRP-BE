package com.sang.be_capnong.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BotKnowledgeRequest {

    @NotBlank(message = "Loại tri thức không được để trống")
    String type;

    @NotBlank(message = "Tiêu đề không được để trống")
    String title;

    @NotBlank(message = "Nội dung không được để trống")
    String content;

    String keywords;
    Boolean active;
}
