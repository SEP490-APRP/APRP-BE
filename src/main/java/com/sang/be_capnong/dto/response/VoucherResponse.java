package com.sang.be_capnong.dto.response;

import com.sang.be_capnong.enums.VoucherScope;
import com.sang.be_capnong.enums.VoucherTarget;
import com.sang.be_capnong.enums.VoucherType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VoucherResponse {
    int voucherId;
    String voucherCode;
    VoucherType voucherType;
    VoucherScope voucherScope;
    VoucherTarget voucherTarget;
    BigDecimal discountValue;
    BigDecimal maxDiscount;
    BigDecimal minOrderValue;
    Integer claimLimit;
    Integer claimedCount;
    Integer usedCount;
    Integer perUserLimit;
    Boolean active;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime expiryDate;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime createAt;
    
    // Thuộc tính mở rộng để UI tiện hiển thị
    String creatorRole; // "ADMIN" hoặc "SHOP"
    Integer shopId; // Trả về shopId nếu là shop voucher
    Boolean isPublic; // Phân biệt mã hiển thị hay mã gõ tay
    Boolean isCollected; // User hiện tại đã lưu mã này vào ví chưa
    Boolean isUsed; // User hiện tại đã sử dụng hết lượt chưa
    Integer userRemainingQuantity; // Số lượt dùng còn lại của user (nếu đã lưu)
}
