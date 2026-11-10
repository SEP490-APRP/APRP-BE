package com.sang.be_capnong.dto.request.campaign;

import lombok.*;
import lombok.experimental.FieldDefaults;

/** Ghi chú / lý do khi Admin duyệt, từ chối, huỷ */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CampaignNoteRequest {
    String note;
    /** Chỉ dùng khi duyệt đăng ký nhà vườn: giá chốt sau thương lượng (đ/kg), bỏ trống = giữ giá đề xuất */
    java.math.BigDecimal agreedPricePerKg;
}
