package com.sang.be_capnong.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.sang.be_capnong.dto.request.CreatePaymentRequest;
import com.sang.be_capnong.dto.response.PaymentResponse;

public interface PaymentService {
    PaymentResponse createPayosPayment(CreatePaymentRequest request);
    PaymentResponse getPaymentByOrderCode(String orderCode);
    PaymentResponse confirmPaymentFromClient(String orderCode);
    void processPayosWebhook(JsonNode webhookBody);
    com.sang.be_capnong.dto.response.ResumePaymentOptionsResponse getResumePaymentOptions(int orderId);
}
