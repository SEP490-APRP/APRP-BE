package com.sang.be_capnong.dto.request.campaign;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

/** Shipper nhận một lần nhiều đơn cùng hub thành một chuyến giao */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PreOrderBatchRequest {
    List<Integer> preOrderIds;
}
