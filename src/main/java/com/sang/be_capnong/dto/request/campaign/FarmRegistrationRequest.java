package com.sang.be_capnong.dto.request.campaign;

import com.sang.be_capnong.enums.ShelfLifeTier;
import java.util.List;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Nhà vườn tham gia chiến dịch trong 2 bước: số kg dư + ngày thu hoạch */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FarmRegistrationRequest {
    BigDecimal surplusVolumeKg;
    LocalDate harvestDate;
    String note;

    /** 1-5 ảnh minh hoạ lô hàng */
    List<String> productImageUrls;
    /** Phải true: nhà vườn cam kết giữ giá + số lượng + chất lượng đã khai */
    Boolean priceCommitted;
    /** DAYS_2_3 | DAYS_3_4 | DAYS_5_6 */
    ShelfLifeTier shelfLifeTier;
}
