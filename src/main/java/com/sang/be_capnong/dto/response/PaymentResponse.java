package com.sang.be_capnong.dto.response;

import com.sang.be_capnong.enums.PaymentStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentResponse {
    int paymentId;
    Integer orderId;
    Integer batchId;
    BigDecimal amount;
    String paymentGateway;
    PaymentStatus status;
    String payosOrderCode;
    String checkoutUrl;
    String qrCodeUrl;
    LocalDateTime createdAt;
}
