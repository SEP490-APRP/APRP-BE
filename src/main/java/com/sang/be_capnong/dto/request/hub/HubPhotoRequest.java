package com.sang.be_capnong.dto.request.hub;

import lombok.*;
import lombok.experimental.FieldDefaults;

/** Ảnh minh chứng tài xế gửi khi bốc hàng lên xe hoặc giao tới hub */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HubPhotoRequest {
    String photoUrl;
}
