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

    /** Sản phẩm có sẵn trong gian hàng (tuỳ chọn) */
    Integer productId;
    /** Tên sản phẩm (bắt buộc; lấy theo productId nếu bỏ trống) */
    String productName;
    /** 1-5 ảnh minh hoạ lô hàng */
    List<String> productImageUrls;
    /** Giá đề xuất đ/kg để thương lượng */
    BigDecimal proposedPricePerKg;
    /** Phải true: nhà vườn cam kết giữ giá + số lượng + chất lượng đã khai */
    Boolean priceCommitted;
    /** DAYS_1_2 | DAYS_3_4 | DAYS_5_6 */
    ShelfLifeTier shelfLifeTier;
}
