package com.sang.be_capnong.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ResumePaymentOptionsResponse {
    @JsonProperty("isBatch")
    boolean isBatch;
    Integer batchId;
    Integer triggerOrderId;
    BigDecimal totalAmount;
    String payosOrderCode;
    String checkoutUrl;
    String qrCodeUrl;
    List<ResumePaymentOrderOption> unpaidOrders;
    PayAllOption payAllOption;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PayAllOption {
        BigDecimal amount;
        String checkoutUrl;
        String payosOrderCode;
        String qrCodeUrl;
    }
}
