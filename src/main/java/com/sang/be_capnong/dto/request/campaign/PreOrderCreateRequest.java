package com.sang.be_capnong.dto.request.campaign;

import com.sang.be_capnong.enums.CrateSize;
import lombok.*;
import lombok.experimental.FieldDefaults;

/** Người mua đặt trước: nhập số kg (bội số của 5), máy chủ chia thùng */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PreOrderCreateRequest {
    /** Số kg muốn mua; có giá trị này thì cỡ và số thùng do máy chủ tính */
    Integer totalKg;
    CrateSize crateSize;
    Integer crateQuantity;
    String recipientName;
    String recipientPhone;
    String shippingAddress;
    Double shippingLatitude;
    Double shippingLongitude;
    /** true = trừ ví người mua trước cho tiền cọc, phần còn lại qua PayOS */
    Boolean useWallet;
    /** DELIVERY (mặc định) | PICKUP: tự đến điểm tập kết lấy hàng */
    String fulfillmentMethod;
    /** Phường/xã giao hàng (để gom đơn) */
    String deliveryWard;
    String note;
}
