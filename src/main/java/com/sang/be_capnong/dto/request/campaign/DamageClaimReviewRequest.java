package com.sang.be_capnong.dto.request.campaign;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

/** Admin duyệt / từ chối báo hư; approvedKg bỏ trống = chấp nhận đúng số kg người mua khai */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DamageClaimReviewRequest {
    BigDecimal approvedKg;
    String note;
}
