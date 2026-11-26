package com.sang.be_capnong.dto.request.hub;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

/** Admin đồng bộ kế hoạch xe của chiến dịch: tạo / cập nhật chuyến theo số thứ tự xe, huỷ chuyến không còn trong kế hoạch */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HubTripSyncRequest {
    /** Ngày cần xe, yyyy-MM-dd */
    String pickupDate;
    /** true = chốt kế hoạch sau khi cân hàng */
    Boolean confirmed;
    List<Trip> trips;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Trip {
        Integer tripNo;
        Double totalKg;
        List<HubTripStop> stops;
        /** Admin chỉ định tài xế cho xe này; bỏ trống = giữ tài xế hiện có / để tài xế tự nhận */
        Integer driverId;
        /** Cỡ xe (kg) admin chọn khi gọi chuyến này */
        Integer vehicleKg;
    }
}
