package com.sang.be_capnong.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShipperLocationRequest {
    Integer orderId;
    Double latitude;
    Double longitude;
    Boolean isFake;
    Boolean gpsOff;
}
