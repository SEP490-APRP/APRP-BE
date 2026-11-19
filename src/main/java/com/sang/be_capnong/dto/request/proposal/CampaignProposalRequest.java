package com.sang.be_capnong.dto.request.proposal;

import com.sang.be_capnong.enums.ShelfLifeTier;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Shop tạo / sửa đơn đề xuất chiến dịch (nháp được lưu dở, đủ thông tin mới gửi duyệt được) */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CampaignProposalRequest {
    Integer categoryId;
    String productName;
    List<String> photoUrls;
    String story;
    /** Số kg dư dự kiến của chính shop khởi xướng */
    BigDecimal estimatedKg;
    LocalDate harvestFrom;
    LocalDate harvestTo;
    /** Ngày đề xuất mở bán / kết thúc chiến dịch */
    LocalDate proposedOpenDate;
    LocalDate proposedCloseDate;
    ShelfLifeTier shelfLifeTier;
    BigDecimal desiredPricePerKg;
    BigDecimal marketPricePerKg;
    String siteName;
    String siteAddress;
    Double siteLatitude;
    Double siteLongitude;
}
