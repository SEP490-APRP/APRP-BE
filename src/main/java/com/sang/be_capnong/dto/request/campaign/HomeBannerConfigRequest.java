package com.sang.be_capnong.dto.request.campaign;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

/** Admin chọn chiến dịch gắn lên banner đầu trang chủ (theo thứ tự) và số giây tự chuyển */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HomeBannerConfigRequest {
    /** 3 - 30 giây */
    Integer intervalSeconds;
    /** Tối đa 10 chiến dịch, đúng thứ tự hiển thị */
    List<Integer> campaignIds;
}
