package com.sang.be_capnong.dto.response.hub;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

/** Các thùng của một đơn mà tài xế xe ra hub phải nhận lên xe */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HubTripBox {
    int orderId;
    String code;
    String recipientName;
    String hubName;
    int crateKg;
    int crates;
    /** Số thứ tự các thùng đã nhận (1..crates) */
    List<Integer> loaded;
    /** Mã tem QR của đơn, chỉ trả khi chuyến đã có tài xế */
    String qrCode;
}
