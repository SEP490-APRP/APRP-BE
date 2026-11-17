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
public class AdminOrderReportResponse {
    long totalOrders;
    BigDecimal totalRevenue;
    long completedOrders;
    long deliveringOrders;
    long pendingOrders;
    long cancelledOrders;
    long refundedOrders;
    List<AdminOrderReportItemResponse> chartData;
}

