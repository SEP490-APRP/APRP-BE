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
public class RefundDueResponse {
    int orderId;
    Integer buyerId;
    String buyerName;
    String buyerEmail;
    String orderStatus;
    String paymentMethod;
    BigDecimal totalAmount;
    BigDecimal refundAmount;
    String refundReason;
    LocalDateTime createdAt;
}
