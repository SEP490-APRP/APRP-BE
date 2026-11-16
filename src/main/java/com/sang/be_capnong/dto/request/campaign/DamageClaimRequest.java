package com.sang.be_capnong.dto.request.campaign;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

/** Người mua báo hàng hư: số kg tự cân + ảnh cân / ảnh hàng */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DamageClaimRequest {
    BigDecimal claimedKg;
    List<String> photoUrls;
    String reason;
}
