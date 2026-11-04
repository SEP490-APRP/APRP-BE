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
public class WithdrawResponse {
    int id;
    int walletId;
    /** SHOP | SHIPPER | REFERRAL */
    String walletType;
    int userId;
    String ownerName;
    String ownerEmail;
    BigDecimal amount;
    BigDecimal fee;
    BigDecimal receiveAmount;
    /** PENDING | COMPLETED | REJECTED */
    String status;
    String bankName;
    String bankAccountNumber;
    String bankAccountHolder;
    String note;
    String adminNote;
    String adminName;
    LocalDateTime createdAt;
    LocalDateTime processedAt;
}
