package com.sang.be_capnong.dto.response.campaign;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

/** Banner chiến dịch ở đầu trang chủ: số giây tự chuyển + chiến dịch theo thứ tự */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HomeBannerConfigResponse {
    int intervalSeconds;
    List<CampaignResponse> campaigns;
}
