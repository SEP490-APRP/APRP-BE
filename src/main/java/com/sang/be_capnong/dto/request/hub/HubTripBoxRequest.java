package com.sang.be_capnong.dto.request.hub;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

/** Tài xế đánh dấu thùng đã nhận của một đơn */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HubTripBoxRequest {
    Integer orderId;
    List<Integer> loaded;
}
