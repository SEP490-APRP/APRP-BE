package com.sang.be_capnong.dto.response.hub;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DeliveryHubResponse {
    int hubId;
    String name;
    String address;
    Double latitude;
    Double longitude;
    String phone;
    String openHours;
    String note;
    boolean active;
    /** Số đơn chiến dịch đang nằm ở hub (chờ shipper hoặc shipper chưa lấy) */
    long waitingOrders;
    /** Đơn xe tải đã giao tới mà hub chưa xác nhận nhận hàng */
    long unconfirmedOrders;
    LocalDateTime createdAt;
}
