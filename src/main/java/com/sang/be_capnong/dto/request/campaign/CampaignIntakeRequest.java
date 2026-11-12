package com.sang.be_capnong.dto.request.campaign;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

/** Admin nhập kết quả cân / kiểm tại điểm tập kết cho một lô của nhà vườn */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CampaignIntakeRequest {
    /** Tổng kg cân thực tế nhà vườn mang tới */
    BigDecimal receivedWeightKg;
    /** Kg hư hỏng / không đạt bị loại (mặc định 0) */
    BigDecimal rejectedWeightKg;
    Boolean qualityPassed;
    List<String> photoUrls;
    String note;
}
