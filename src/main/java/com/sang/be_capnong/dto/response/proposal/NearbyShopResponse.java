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
    /** null khi shop chưa ghim vị trí (vd tìm theo tên) */
    Double distanceKm;
    /** Toạ độ làm tròn ~1 km để vẽ bản đồ */
    Double approxLatitude;
    Double approxLongitude;
    List<String> categories;
    boolean sameCategory;
    /** Trạng thái lời mời nếu đã mời trong đơn này, null nếu chưa */
    String inviteStatus;
    /** Admin xem trong chiến dịch: trạng thái lô gần nhất của shop (PENDING / APPROVED...), null nếu chưa góp */
    String registrationStatus;
    /** Tổng kg shop đã góp (chờ duyệt + đã duyệt) trong chiến dịch */
    java.math.BigDecimal registeredKg;
}
