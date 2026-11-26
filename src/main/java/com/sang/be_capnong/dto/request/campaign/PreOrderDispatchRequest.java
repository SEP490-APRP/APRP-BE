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
    /** Hub đưa các đơn này tới; bỏ trống thì giữ hub đã gán hoặc chọn hub gần người mua nhất */
    Integer hubId;
    /** true = shipper lấy hàng ngay tại điểm tập kết và giao thẳng, không qua hub */
    Boolean direct;
}
