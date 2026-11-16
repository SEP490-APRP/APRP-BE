package com.sang.be_capnong.dto.request.campaign;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

/** Chia đơn: đưa các đơn đã đóng gói ra hub, giao thẳng cho một shipper (hoặc để shipper tự nhận) */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PreOrderDispatchRequest {
    List<Integer> preOrderIds;
    /** null = để shipper tự nhận trong danh sách "tại hub" */
    Integer shipperId;
}
