package com.sang.be_capnong.dto.response.campaign;

import lombok.*;
import lombok.experimental.FieldDefaults;

/** Kết quả gửi kêu gọi xe: số tài xế đã báo và số tài xế bị bỏ qua vì xe chưa đủ tải */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShipperCallResult {
    int sent;
    /** Tài xế đang hoạt động nhưng xe nhỏ (xe máy...) nên không gửi */
    int skippedSmallVehicle;
    /** Sức chở tối thiểu (kg) dùng để lọc */
    int minVehicleKg;
}
