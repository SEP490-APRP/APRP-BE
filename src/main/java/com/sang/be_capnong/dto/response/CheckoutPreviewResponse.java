package com.sang.be_capnong.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CheckoutPreviewResponse {
    List<ShopOrderPreview> shopOrders;
    BigDecimal totalGoodsAmount;
    BigDecimal totalShippingFee;
    BigDecimal totalVoucherDiscount;
    BigDecimal finalTotalAmount;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class ShopOrderPreview {
        int shopId;
        String shopName;
        BigDecimal goodsAmount;
        BigDecimal shippingFee;
        BigDecimal voucherDiscount;
        BigDecimal totalAmount;
        List<OrderDetailResponse> items;
        java.util.Map<String, BigDecimal> voucherDiscountsDetail;
    }
}
