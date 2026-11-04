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
public class WalletResponse {
    int walletId;
    /** SHOP | SHIPPER | PLATFORM */
    String type;
    int userId;
    String ownerName;
    String status;
    /** Số dư hiện tại. Với Shipper có thể âm khi đang giữ tiền mặt COD chưa nộp về sàn */
    BigDecimal balance;
    BigDecimal frozenBalance;
    /** Tổng thu nhập đã cộng vào ví từ trước tới nay */
    BigDecimal totalRevenueAllTime;
    BigDecimal totalWithdrawn;
    /** Chỉ ví sàn: tổng phí dịch vụ đã thu */
    BigDecimal commissionEarned;
    /** Chỉ ví Shipper: tiền mặt COD đang giữ, chưa nộp về sàn */
    BigDecimal codCashHolding;
    LocalDateTime updatedAt;
}
