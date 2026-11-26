package com.sang.be_capnong.dto.response.campaign;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

/** Hàng tại điểm tập kết của chiến dịch (kg), xem SupplySummaryCalculator */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SupplySummaryResponse {
    /** Người mua đã đặt cọc, kể cả phần sau đó bị bớt */
    BigDecimal orderedKg;
    /** Nhà vườn giao đợt đầu (cân thực tế, chưa trừ hư) */
    BigDecimal deliveredByFarmsKg;
    BigDecimal rejectedKg;
    BigDecimal firstAcceptedKg;
    BigDecimal shortAfterWeighingKg;
    BigDecimal supplementKg;
    List<Supplier> supplements;
    /** Không bù được nên đã bớt khỏi đơn */
    BigDecimal cutKg;
    int reducedOrderCount;
    int cancelledOrderCount;
    BigDecimal refundAmount;
    BigDecimal remainingShortKg;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Supplier {
        String shopName;
        BigDecimal kg;
    }
}
