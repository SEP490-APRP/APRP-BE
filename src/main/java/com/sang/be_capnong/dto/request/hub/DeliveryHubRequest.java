package com.sang.be_capnong.dto.request.hub;

import lombok.*;
import lombok.experimental.FieldDefaults;

/** Admin thêm / sửa điểm trung chuyển */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DeliveryHubRequest {
    String name;
    String address;
    Double latitude;
    Double longitude;
    String phone;
    String openHours;
    String note;
    Boolean active;
}
