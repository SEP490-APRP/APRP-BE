package com.sang.be_capnong.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminOrderReportItemResponse {
    String label;
    String date;
    BigDecimal revenue;
    long orderCount;
    long completedCount;
    long cancelledCount;
}

