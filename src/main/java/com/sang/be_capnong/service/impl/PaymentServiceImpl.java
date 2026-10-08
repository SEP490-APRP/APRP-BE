package com.sang.be_capnong.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.sang.be_capnong.dto.request.CreatePaymentRequest;
import com.sang.be_capnong.dto.response.PaymentResponse;
import com.sang.be_capnong.entity.CheckoutBatch;
import com.sang.be_capnong.entity.Order;
import com.sang.be_capnong.entity.Payment;
import com.sang.be_capnong.enums.BatchStatus;
import com.sang.be_capnong.enums.OrderStatus;
import com.sang.be_capnong.enums.PaymentStatus;
import com.sang.be_capnong.exception.AppException;
import com.sang.be_capnong.exception.ErrorCode;
import com.sang.be_capnong.repository.CheckoutBatchRepository;
import com.sang.be_capnong.repository.OrderRepository;
import com.sang.be_capnong.repository.PaymentRepository;
import com.sang.be_capnong.service.PaymentService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLink;
import vn.payos.model.v2.paymentRequests.PaymentLinkStatus;
import vn.payos.model.webhooks.WebhookData;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    final PaymentRepository paymentRepository;
    final OrderRepository orderRepository;
    final CheckoutBatchRepository checkoutBatchRepository;
    final PayOS payOS;

    @Value("${payos.return-url:http://localhost:3000/payment/result}")
    String defaultReturnUrl;

    @Value("${payos.cancel-url:http://localhost:3000/payment/result}")
    String defaultCancelUrl;

    @Override
    @Transactional
    public PaymentResponse createPayosPayment(CreatePaymentRequest request) {
        String returnUrl = (request.getReturnUrl() != null && !request.getReturnUrl().isBlank())
                ? request.getReturnUrl() : defaultReturnUrl;
        String cancelUrl = (request.getCancelUrl() != null && !request.getCancelUrl().isBlank())
                ? request.getCancelUrl() : defaultCancelUrl;

        // Trường hợp 1: Thanh toán cho một CheckoutBatch (gồm 1 hoặc nhiều order)
        if (request.getBatchId() != null) {
            CheckoutBatch batch = checkoutBatchRepository.findById(request.getBatchId())
                    .orElseThrow(() -> new AppException(ErrorCode.BATCH_NOT_FOUND));

            long orderCode = System.currentTimeMillis();
            long amount = batch.getTotalAmount().longValue();

            try {
                CreatePaymentLinkRequest paymentData = CreatePaymentLinkRequest.builder()
                        .orderCode(orderCode)
                        .amount(amount)
                        .description("Thanh toan batch " + batch.getId())
                        .returnUrl(returnUrl)
                        .cancelUrl(cancelUrl)
                        .build();

                CreatePaymentLinkResponse checkoutData = payOS.paymentRequests().create(paymentData);

                batch.setPayosOrderCode(String.valueOf(orderCode));
                batch.setCheckoutUrl(checkoutData.getCheckoutUrl());
                batch.setQrCodeUrl(checkoutData.getQrCode());
                batch.setUpdatedAt(LocalDateTime.now());
                checkoutBatchRepository.save(batch);

                Payment payment = Payment.builder()
                        .amount(batch.getTotalAmount())
                        .paymentGateway("PAYOS")
                        .status(PaymentStatus.PENDING)
                        .payosOrderCode(String.valueOf(orderCode))
                        .checkoutUrl(checkoutData.getCheckoutUrl())
                        .qrCodeUrl(checkoutData.getQrCode())
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();
                Payment savedPayment = paymentRepository.save(payment);

                return PaymentResponse.builder()
                        .paymentId(savedPayment.getPaymentId())
                        .batchId(batch.getId())
                        .amount(batch.getTotalAmount())
                        .paymentGateway("PAYOS")
                        .status(PaymentStatus.PENDING)
                        .payosOrderCode(String.valueOf(orderCode))
                        .checkoutUrl(checkoutData.getCheckoutUrl())
                        .qrCodeUrl(checkoutData.getQrCode())
                        .createdAt(savedPayment.getCreatedAt())
                        .build();

            } catch (Exception e) {
                log.error("Failed to create PayOS payment link for batch {}: {}", batch.getId(), e.getMessage(), e);
                throw new AppException(ErrorCode.PAYMENT_FAILED);
            }
        }

        // Trường hợp 2: Thanh toán cho 1 đơn hàng cụ thể
        if (request.getOrderId() != null) {
            Order order = orderRepository.findById(request.getOrderId())
                    .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

            if (order.getStatus() != OrderStatus.PENDING) {
                throw new AppException(ErrorCode.ORDER_NOT_PAYABLE);
            }

            long orderCode = System.currentTimeMillis();
            long amount = order.getTotalAmount().longValue();

            try {
                CreatePaymentLinkRequest paymentData = CreatePaymentLinkRequest.builder()
                        .orderCode(orderCode)
                        .amount(amount)
                        .description("Thanh toan don " + order.getOrderId())
                        .returnUrl(returnUrl)
                        .cancelUrl(cancelUrl)
                        .build();

                CreatePaymentLinkResponse checkoutData = payOS.paymentRequests().create(paymentData);

                Payment payment = Payment.builder()
                        .order(order)
                        .amount(order.getTotalAmount())
                        .paymentGateway("PAYOS")
                        .status(PaymentStatus.PENDING)
                        .payosOrderCode(String.valueOf(orderCode))
                        .checkoutUrl(checkoutData.getCheckoutUrl())
                        .qrCodeUrl(checkoutData.getQrCode())
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();

                Payment savedPayment = paymentRepository.save(payment);

                return PaymentResponse.builder()
                        .paymentId(savedPayment.getPaymentId())
                        .orderId(order.getOrderId())
                        .amount(order.getTotalAmount())
                        .paymentGateway("PAYOS")
                        .status(PaymentStatus.PENDING)
                        .payosOrderCode(String.valueOf(orderCode))
                        .checkoutUrl(checkoutData.getCheckoutUrl())
                        .qrCodeUrl(checkoutData.getQrCode())
                        .createdAt(savedPayment.getCreatedAt())
                        .build();

            } catch (Exception e) {
                log.error("Failed to create PayOS payment link for order {}: {}", order.getOrderId(), e.getMessage(), e);
                throw new AppException(ErrorCode.PAYMENT_FAILED);
            }
        }

        throw new AppException(ErrorCode.INVALID_KEY);
    }

    @Override
    public PaymentResponse getPaymentByOrderCode(String orderCode) {
        Payment payment = paymentRepository.findByPayosOrderCode(orderCode)
                .orElse(null);

        if (payment != null) {
            return toPaymentResponse(payment);
        }

        CheckoutBatch batch = checkoutBatchRepository.findByPayosOrderCode(orderCode)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));

        return PaymentResponse.builder()
                .paymentId(0)
                .batchId(batch.getId())
                .amount(batch.getTotalAmount())
                .paymentGateway("PAYOS")
                .status("COMPLETED".equalsIgnoreCase(String.valueOf(batch.getStatus())) ? PaymentStatus.PAID : PaymentStatus.PENDING)
                .payosOrderCode(batch.getPayosOrderCode())
                .checkoutUrl(batch.getCheckoutUrl())
                .qrCodeUrl(batch.getQrCodeUrl())
                .createdAt(batch.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public PaymentResponse confirmPaymentFromClient(String orderCode) {
        log.info("[PaymentConfirm] Client requesting confirm for orderCode: {}", orderCode);

        // 1. Kiểm tra xem đã xử lý PAID trước đó chưa (idempotent)
        var paymentOpt = paymentRepository.findByPayosOrderCode(orderCode);
        if (paymentOpt.isPresent() && paymentOpt.get().getStatus() == PaymentStatus.PAID) {
            log.info("[PaymentConfirm] Payment already settled as PAID: {}", orderCode);
            return toPaymentResponse(paymentOpt.get());
        }

        // 2. Xác thực với PayOS API để đảm bảo tiền thực sự đã thanh toán
        try {
            PaymentLink linkData = payOS.paymentRequests().get(Long.parseLong(orderCode));
            if (linkData != null && PaymentLinkStatus.PAID.equals(linkData.getStatus())) {
                settlePaidOrderCode(orderCode);
            }
        } catch (Exception e) {
            log.warn("[PaymentConfirm] Error checking PayOS API for orderCode {}: {}", orderCode, e.getMessage());
        }

        return getPaymentByOrderCode(orderCode);
    }

    @Override
    @Transactional
    public void processPayosWebhook(JsonNode webhookBody) {
        log.info("[PayOS Webhook] Received webhook payload: {}", webhookBody);
        try {
            WebhookData webhookData = payOS.webhooks().verify(webhookBody);
            if (webhookData != null) {
                String orderCode = String.valueOf(webhookData.getOrderCode());
                log.info("[PayOS Webhook] Verified webhook for orderCode: {}, code: {}", orderCode, webhookData.getCode());
                if ("00".equals(webhookData.getCode())) {
                    settlePaidOrderCode(orderCode);
                }
            }
        } catch (Exception e) {
            log.error("[PayOS Webhook] Invalid webhook signature or processing error: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.PAYOS_WEBHOOK_INVALID);
        }
    }

    private void settlePaidOrderCode(String orderCode) {
        log.info("[PaymentSettle] Settling orderCode to PAID: {}", orderCode);

        // Settle Payment nếu có
        paymentRepository.findByPayosOrderCode(orderCode).ifPresent(payment -> {
            payment.setStatus(PaymentStatus.PAID);
            payment.setPaymentDate(LocalDateTime.now());
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);

            if (payment.getOrder() != null) {
                Order order = payment.getOrder();
                order.setStatus(OrderStatus.PAID);
                orderRepository.save(order);
                log.info("[PaymentSettle] Single order #{} updated to PAID", order.getOrderId());
            }
        });

        // Settle Batch nếu có
        checkoutBatchRepository.findByPayosOrderCode(orderCode).ifPresent(batch -> {
            batch.setStatus(BatchStatus.COMPLETED);
            batch.setUpdatedAt(LocalDateTime.now());
            checkoutBatchRepository.save(batch);

            List<Order> batchOrders = orderRepository.findByCheckoutBatch_Id(batch.getId());
            for (Order o : batchOrders) {
                o.setStatus(OrderStatus.PAID);
                orderRepository.save(o);
                log.info("[PaymentSettle] Batch order #{} in batch #{} updated to PAID", o.getOrderId(), batch.getId());
            }
        });
    }

    private PaymentResponse toPaymentResponse(Payment payment) {
        return PaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .orderId(payment.getOrder() != null ? payment.getOrder().getOrderId() : null)
                .amount(payment.getAmount())
                .paymentGateway(payment.getPaymentGateway())
                .status(payment.getStatus())
                .payosOrderCode(payment.getPayosOrderCode())
                .checkoutUrl(payment.getCheckoutUrl())
                .qrCodeUrl(payment.getQrCodeUrl())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
