package com.sang.be_capnong.dto.request.campaign;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Banner quảng bá chiến dịch ở trang chủ do Admin tuỳ chỉnh.
 * mode = AUTO: banner dựng từ thông tin chiến dịch theo mẫu màu (theme), chữ có thể sửa.
 * mode = IMAGE: dùng nguyên ảnh banner Admin tải lên.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CampaignBannerRequest {
    /** AUTO | IMAGE */
    String mode;
    /** ORANGE | GREEN | RED */
    String theme;
    String headline;
    String subline;
    /** Chữ trên huy hiệu nổ, vd "CHỈ CỌC 30%" */
    String badge;
    String ctaText;
    /** AUTO: ảnh minh hoạ thay ảnh bìa; IMAGE: ảnh banner hoàn chỉnh */
    String imageUrl;
    /** Có bật popup / thẻ góc ở trang chủ không */
    Boolean showOnHome;
}
