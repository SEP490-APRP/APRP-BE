package com.sang.be_capnong.dto.request.hub;

import lombok.*;
import lombok.experimental.FieldDefaults;

/** Một điểm dừng (hub) trên lộ trình xe: hub nào, bao nhiêu kg, bao nhiêu đơn */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HubTripStop {
    Integer hubId;
    String hubName;
    String hubAddress;
    Double hubLatitude;
    Double hubLongitude;
    Double kg;
    Integer orderCount;
    /** Mã các đơn chở tới hub này trên chuyến: BE dùng để tính cước shipper giao chặng cuối */
    java.util.List<Integer> orderIds;
    /** Lúc tài xế giao xong hàng của hub này (chuỗi ISO); chưa giao thì trống */
    String arrivedAt;
    /** Ảnh tài xế chụp hàng giao tại hub này */
    String photoUrl;
}
