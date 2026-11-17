package com.sang.be_capnong.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
    /** Quãng đường còn lại tới điểm đến của chặng hiện tại (km, ước tính) */
    Double distanceKm;
    /** Số phút dự kiến tới nơi */
    Integer estimatedMinutes;
    /** Tracking còn đang diễn ra realtime (true) hay đã hoàn tất/chốt lại (false) */
    Boolean trackingActive;
    /** Danh sách ảnh bằng chứng giao hàng (nếu đã DELIVERED) */
    List<String> proofImageUrls;

    /** Phương thức thanh toán của đơn (COD, PAYOS...) */
    String paymentMethod;
    /** Số tiền COD cần thu của khách (0 nếu đã trả online) */
    BigDecimal codAmount;
    /** Tổng tiền đơn hàng */
    BigDecimal totalAmount;
}
