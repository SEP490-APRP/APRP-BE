package com.sang.be_capnong.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShopRevenueReportResponse {
    BigDecimal totalRevenue;
    long completedOrders;
    long totalOrders;
    BigDecimal totalRescuedWeight;
    double successRate;
    List<AdminOrderReportItemResponse> chartData;
}

