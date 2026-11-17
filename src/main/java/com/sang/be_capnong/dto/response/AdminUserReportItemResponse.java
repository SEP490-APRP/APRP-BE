package com.sang.be_capnong.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminUserReportItemResponse {
    String period;
    String date;
    String label;
    long buyers;
    long shops;
    long shippers;
    long total;
    long activeUsers;
    long newUsers;
    long locked;

    // Tương thích cả format totalX của FE UserReport
    long totalBuyers;
    long totalShops;
    long totalShipper;
    long totalUserLock;
}

