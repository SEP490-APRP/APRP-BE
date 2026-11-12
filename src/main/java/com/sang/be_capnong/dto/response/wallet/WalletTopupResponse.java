package com.sang.be_capnong.dto.response.wallet;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WalletTopupResponse {
    Integer id;
    BigDecimal amount;
    /** PENDING | PAID | CANCELLED */
    String status;
    String orderCode;
    String checkoutUrl;
    LocalDateTime createdAt;
    LocalDateTime paidAt;
}
