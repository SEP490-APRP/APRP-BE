package com.sang.be_capnong.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreatePaymentRequest {
    Integer orderId;
    Integer batchId;
    String paymentGateway;
    String returnUrl;
    String cancelUrl;
}
