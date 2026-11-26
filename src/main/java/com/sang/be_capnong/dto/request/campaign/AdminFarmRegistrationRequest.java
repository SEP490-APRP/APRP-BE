package com.sang.be_capnong.dto.request.campaign;

import com.sang.be_capnong.enums.ShelfLifeTier;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Admin nhập hộ lô hàng cho shop đã đồng ý góp (lô được duyệt luôn) */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminFarmRegistrationRequest {
    Integer shopId;
    BigDecimal surplusVolumeKg;
    LocalDate harvestDate;
    ShelfLifeTier shelfLifeTier;
    String note;
}
