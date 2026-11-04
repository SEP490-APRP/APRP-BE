package com.sang.be_capnong.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.sang.be_capnong.dto.request.CreatePaymentRequest;
import com.sang.be_capnong.dto.response.PaymentResponse;

public interface PaymentService {
    PaymentResponse createPayosPayment(CreatePaymentRequest request);
    PaymentResponse getPaymentByOrderCode(String orderCode);
    PaymentResponse confirmPaymentFromClient(String orderCode);
    void processPayosWebhook(JsonNode webhookBody);
    void cancelPaymentByOrderCode(String orderCode);
    com.sang.be_capnong.dto.response.ResumePaymentOptionsResponse getResumePaymentOptions(int orderId);

    /** Huỷ mọi link PayOS còn mở có tính tiền đơn này (link lẻ + link batch) */
    void cancelOpenPayosLinksForOrder(com.sang.be_capnong.entity.Order order);

    /** Huỷ đơn AWAITING_PAYMENT quá hạn: hỏi PayOS trước, chưa trả thì huỷ link, hoàn kho, voucher, giỏ hàng */
    boolean expireAwaitingOrder(int orderId);
}
