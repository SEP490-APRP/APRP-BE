package com.sang.be_capnong.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShipperLocationResponse {
    Integer orderId;
    Integer shipperId;
    String shipperName;
    String shipperPhone;
    String vehicleNumber;
    Double latitude;
    Double longitude;
    LocalDateTime updatedAt;

    Double shopLatitude;
    Double shopLongitude;
    String shopAddress;
    String shopName;

    Double destinationLatitude;
    Double destinationLongitude;
    String destinationAddress;
    String recipientName;
    String recipientPhone;

    String orderStatus;
    Boolean isFake;
    Boolean gpsOff;
    String deliveryPhase;
}
