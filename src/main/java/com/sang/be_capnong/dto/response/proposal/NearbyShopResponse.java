package com.sang.be_capnong.dto.response.proposal;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

/** Shop lân cận điểm tập kết. Không lộ số điện thoại / địa chỉ chính xác, khoảng cách làm tròn 0,5 km */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NearbyShopResponse {
    int shopId;
    String shopName;
    String logoUrl;
    Double ratingAverage;
    double distanceKm;
    /** Toạ độ làm tròn ~1 km để vẽ bản đồ */
    double approxLatitude;
    double approxLongitude;
    List<String> categories;
    boolean sameCategory;
    /** Trạng thái lời mời nếu đã mời trong đơn này, null nếu chưa */
    String inviteStatus;
}
