package com.sang.be_capnong.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.sang.be_capnong.dto.request.CreatePaymentRequest;
import com.sang.be_capnong.dto.response.ApiResponse;
import com.sang.be_capnong.dto.response.PaymentResponse;
import com.sang.be_capnong.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Payment Management", description = "APIs for creating and confirming online payments")
@Slf4j
public class PaymentController {

    PaymentService paymentService;

    @Operation(summary = "Tạo link thanh toán PayOS", description = "Khởi tạo giao dịch PayOS và nhận checkout URL/QR Code")
    @PostMapping("/create-payos")
    public ApiResponse<PaymentResponse> createPayosPayment(@RequestBody CreatePaymentRequest request) {
        return ApiResponse.<PaymentResponse>builder()
                .result(paymentService.createPayosPayment(request))
                .message("Payment link created successfully.")
                .build();
    }

    @Operation(summary = "Lấy thông tin thanh toán theo orderCode", description = "Kiểm tra trạng thái thanh toán theo mã PayOS orderCode")
    @GetMapping("/{orderCode}")
    public ApiResponse<PaymentResponse> getPaymentByOrderCode(@PathVariable String orderCode) {
        return ApiResponse.<PaymentResponse>builder()
                .result(paymentService.getPaymentByOrderCode(orderCode))
                .build();
    }

    @Operation(summary = "Lấy tùy chọn tiếp tục thanh toán cho đơn hàng", description = "Kiểm tra xem đơn thuộc batch hay đơn lẻ và danh sách đơn chưa thanh toán")
    @GetMapping("/order/{orderId}/resume-options")
    public ApiResponse<com.sang.be_capnong.dto.response.ResumePaymentOptionsResponse> getResumePaymentOptions(@PathVariable Integer orderId) {
        return ApiResponse.<com.sang.be_capnong.dto.response.ResumePaymentOptionsResponse>builder()
                .result(paymentService.getResumePaymentOptions(orderId))
                .build();
    }

    @Operation(summary = "Xác nhận thanh toán từ Client return", description = "FE gọi khi được redirect về từ PayOS để xác thực giao dịch")
    @PostMapping("/{orderCode}/confirm")
    public ApiResponse<PaymentResponse> confirmPaymentFromClient(@PathVariable String orderCode) {
        return ApiResponse.<PaymentResponse>builder()
                .result(paymentService.confirmPaymentFromClient(orderCode))
                .message("Payment verified.")
                .build();
    }

    @Operation(summary = "PayOS Webhook", description = "Nhận thông báo tự động từ cổng PayOS khi thanh toán hoàn tất")
    @PostMapping("/payos/webhook")
    public ApiResponse<String> payosWebhook(@RequestBody JsonNode request) {
        log.info("PayOS Webhook received: {}", request);
        paymentService.processPayosWebhook(request);
        return ApiResponse.<String>builder()
                .result("OK")
                .message("Webhook processed successfully.")
                .build();
    }

    @PostMapping("/webhook")
    public ApiResponse<String> webhookAlias(@RequestBody JsonNode request) {
        return payosWebhook(request);
    }
}
