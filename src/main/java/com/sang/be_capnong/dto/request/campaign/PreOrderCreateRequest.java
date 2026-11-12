package com.sang.be_capnong.dto.request.campaign;

import com.sang.be_capnong.enums.CrateSize;
import lombok.*;
import lombok.experimental.FieldDefaults;

/** Người mua đặt trước thùng chuẩn 10/20/50 kg */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PreOrderCreateRequest {
    CrateSize crateSize;
    Integer crateQuantity;
    String recipientName;
    String recipientPhone;
    String shippingAddress;
    Double shippingLatitude;
    Double shippingLongitude;
    String note;
}
