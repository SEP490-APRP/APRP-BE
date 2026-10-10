package com.sang.be_capnong.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sang.be_capnong.enums.OrderStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderResponse {
    int orderId;
    Integer batchId;
    Integer shopId;
    String buyerName;
    String shopName;
    String shopAddress;
    String shopPhone;
    String recipientName;
    String recipientPhone;
    String shippingAddress;
    OrderStatus status;
    String paymentMethod;
    BigDecimal shippingFee;
    BigDecimal totalAmount;
    BigDecimal codAmount;
    BigDecimal voucherDiscountAmount;
    Integer shipperId;
    String shipperName;
    String note;
    @JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime createdAt;
    Double shippingLatitude;
    Double shippingLongitude;
    Double shopLatitude;
    Double shopLongitude;
    Double distanceKm;
    List<OrderDetailResponse> items;
}
