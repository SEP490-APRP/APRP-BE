package com.sang.be_capnong.dto.response.wallet;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WalletTransactionResponse {
    Long id;
    int walletId;
    String walletType;
    Integer orderId;
    String type;
    BigDecimal amount;
    BigDecimal balanceAfter;
    BigDecimal frozenAfter;
    /** true = bút toán tiền tạm giữ (không đổi số dư rút được) */
    boolean hold;
    String description;
    LocalDateTime createdAt;
}
