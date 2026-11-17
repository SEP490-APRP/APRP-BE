package com.sang.be_capnong.dto.request.campaign;

import lombok.*;
import lombok.experimental.FieldDefaults;

/** Shipper giao xong (ảnh POD) hoặc giao thất bại (lý do) */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PreOrderDeliveryRequest {
    String podImageUrl;
    String reason;
}
