package com.sang.be_capnong.dto.request.campaign;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

/** Admin mời shop lân cận điểm tập kết góp hàng cho chiến dịch */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminCampaignInviteRequest {
    /** Shop được mời (tối đa 50 mỗi lần) */
    List<Integer> shopIds;
    /** Lời nhắn kèm theo (tối đa 300 ký tự) */
    String message;
}
