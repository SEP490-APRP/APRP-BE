package com.sang.be_capnong.dto.response.campaign;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

/** Giới hạn để shipper tạo chuyến giao: xe chở được bao nhiêu, đang chở bao nhiêu, ví còn ký quỹ được bao nhiêu */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShipperCapacityResponse {
    String vehicleType;
    String vehicleLabel;
    int capacityKg;
    BigDecimal carryingKg;
    BigDecimal availableBalance;
}
