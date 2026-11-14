package com.sang.be_capnong.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

/**
 * Tin nhắn STOMP gửi riêng cho Shipper qua /user/queue/shipper-offers.
 * type = NEW_ORDER (mời nhận đơn) | ORDER_TAKEN (đơn đã có người nhận, gỡ khỏi danh sách)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShipperOfferMessage {
    String type;
    Integer orderId;
    String shopName;
    String shopAddress;
    String shippingAddress;
    Double distanceToShopKm;
    Double distanceKm;
    BigDecimal shippingFee;
    BigDecimal totalAmount;
    String paymentMethod;
    /** Số tiền phải ký quỹ trong ví nếu là đơn COD (0 với đơn đã thanh toán online) */
    BigDecimal codAmount;
    Integer wave;
}
