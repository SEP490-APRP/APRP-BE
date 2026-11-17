package com.sang.be_capnong.dto.response.campaign;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

/** Báo giá phí giao hàng cho đơn đặt trước (hiển thị trước khi đặt) */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CampaignShippingQuoteResponse {
    /** DELIVERY | PICKUP */
    String fulfillmentMethod;
    BigDecimal totalKg;
    Double distanceKm;
    /** Phí gốc theo khoảng cách, chưa giảm */
    BigDecimal baseFee;
    /** Phí phải trả sau khi giảm theo kg */
    BigDecimal shippingFee;
    int discountPercent;
    /** Mua thêm bấy nhiêu kg để lên mốc giảm kế tiếp */
    BigDecimal nextTierKg;
    String nextTierLabel;
    String note;

    // ── Quy tắc của chiến dịch (để FE hiển thị bảng cách tính) ──
    BigDecimal halfShipKg;
    BigDecimal freeShipKg;
    Double freeShipRadiusKm;
    Integer groupDiscountMinOrders;
    Boolean pickupEnabled;
    String collectionSiteName;
    String collectionSiteAddress;
}
