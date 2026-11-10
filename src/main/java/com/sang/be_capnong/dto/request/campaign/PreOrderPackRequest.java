package com.sang.be_capnong.dto.request.campaign;

import lombok.*;
import lombok.experimental.FieldDefaults;

/** Đóng gói đơn đặt trước, gán khu giao / hub */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PreOrderPackRequest {
    String deliveryZone;
}
