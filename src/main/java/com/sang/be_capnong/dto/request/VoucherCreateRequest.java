package com.sang.be_capnong.dto.request;

import com.sang.be_capnong.enums.VoucherType;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VoucherCreateRequest {

    @NotBlank(message = "Voucher code is required")
    String voucherCode;

    @NotNull(message = "Voucher type is required")
    VoucherType voucherType;

    @NotNull(message = "Discount value is required")
    @Min(value = 0, message = "Discount value must be at least 0")
    BigDecimal discountValue;

    BigDecimal maxDiscount;

    @Min(value = 0, message = "Min order value must be at least 0")
    BigDecimal minOrderValue;

    @NotNull(message = "Total claim limit is required")
    @Min(value = 1, message = "Total claim limit must be at least 1")
    Integer claimLimit;

    @NotNull(message = "Usage limit per user is required")
    @Min(value = 1, message = "Usage limit per user must be at least 1")
    Integer perUserLimit;

    @NotNull(message = "Expiry date is required")
    @Future(message = "Expiry date must be in the future")
    LocalDateTime expiryDate;

    // Truyền true nếu là public, truyền false nếu là mã ẩn (hidden)
    Boolean isPublic = true;
}
