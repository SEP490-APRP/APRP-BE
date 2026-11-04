package com.sang.be_capnong.dto.request.wallet;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

/** Shop / Shipper / Buyer yêu cầu rút tiền từ ví về tài khoản ngân hàng */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WithdrawCreateRequest {
    String bankName;
    String bankAccountNumber;
    String bankAccountHolder;
    BigDecimal amount;
    /** Ghi chú của người rút (không bắt buộc) */
    String note;
}
