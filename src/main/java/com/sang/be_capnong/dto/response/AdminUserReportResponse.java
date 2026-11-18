package com.sang.be_capnong.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminUserReportResponse {
    long totalUsers;
    long totalBuyers;
    long totalShops;
    long totalShipper;
    long totalActiveUsers;
    long totalInactiveUsers;
    long totalShopPending;
    long totalShopSelling;
    long totalShopNotSelling;
    long totalUserLock;
    List<AdminUserReportItemResponse> userReports;
}

