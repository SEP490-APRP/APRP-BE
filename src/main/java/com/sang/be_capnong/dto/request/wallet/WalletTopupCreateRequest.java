package com.sang.be_capnong.dto.request.wallet;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WalletTopupCreateRequest {
    BigDecimal amount;
    /** Trang PayOS quay về sau khi trả tiền (web: /shipper?tab=wallet, mobile: deep link). Bỏ trống = mặc định */
    String returnUrl;
    String cancelUrl;
}
