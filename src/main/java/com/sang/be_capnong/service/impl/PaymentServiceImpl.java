package com.sang.be_capnong.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.sang.be_capnong.dto.request.CreatePaymentRequest;
import com.sang.be_capnong.dto.response.PaymentResponse;
import com.sang.be_capnong.dto.response.ResumePaymentOptionsResponse;
import com.sang.be_capnong.dto.response.ResumePaymentOrderOption;
import java.math.BigDecimal;
import java.text.Normalizer;
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
import com.sang.be_capnong.entity.*;
import com.sang.be_capnong.repository.*;
import com.sang.be_capnong.service.CartService;
import com.sang.be_capnong.service.EmailService;
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
    final CartService cartService;
    final EmailService emailService;
    final OrderDetailRepository orderDetailRepository;
    final ProductRepository productRepository;
    final MysteryBoxRepository mysteryBoxRepository;
    final BuildComboRepository buildComboRepository;
    final VoucherRepository voucherRepository;
    final VoucherUserRepository voucherUserRepository;
    final VoucherUsageRepository voucherUsageRepository;

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

        // Trường hợp 1: Thanh toán cho một CheckoutBatch (gồm nhiều order)
        if (request.getBatchId() != null) {
            CheckoutBatch batch = checkoutBatchRepository.findById(request.getBatchId())
                    .orElseThrow(() -> new AppException(ErrorCode.BATCH_NOT_FOUND));

            if (batch.getStatus() == BatchStatus.COMPLETED) {
                throw new AppException(ErrorCode.ORDER_NOT_PAYABLE);
            }

            List<Order> batchOrders = orderRepository.findByCheckoutBatch_Id(batch.getId());
            List<Order> unpaid = batchOrders.stream()
                    .filter(o -> o.getStatus() == OrderStatus.AWAITING_PAYMENT || o.getStatus() == OrderStatus.PENDING)
                    .toList();

            if (unpaid.isEmpty()) {
                throw new AppException(ErrorCode.ORDER_NOT_PAYABLE);
            }

            BigDecimal remainingTotal = unpaid.stream()
                    .map(Order::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            long orderCode = System.currentTimeMillis() % 900000000000000L;
            long amount = Math.max(1000L, remainingTotal.longValue());
            String desc = truncateDescription("Batch " + batch.getId());

            try {
                CreatePaymentLinkRequest paymentData = CreatePaymentLinkRequest.builder()
                        .orderCode(orderCode)
                        .amount(amount)
                        .description(desc)
                        .returnUrl(returnUrl)
                        .cancelUrl(cancelUrl)
                        .build();

                CreatePaymentLinkResponse checkoutData = payOS.paymentRequests().create(paymentData);

                batch.setPayosOrderCode(String.valueOf(orderCode));
                batch.setCheckoutUrl(checkoutData.getCheckoutUrl());
                batch.setQrCodeUrl(checkoutData.getQrCode());
                batch.setTotalAmount(remainingTotal);
                batch.setUpdatedAt(LocalDateTime.now());
                checkoutBatchRepository.save(batch);

                Payment payment = Payment.builder()
                        .amount(remainingTotal)
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
                        .amount(remainingTotal)
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

        // Trường hợp 2: Thanh toán cho 1 đơn hàng cụ thể (dù thuộc batch hay đơn lẻ)
        if (request.getOrderId() != null) {
            Order order = orderRepository.findById(request.getOrderId())
                    .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

            if (order.getStatus() != OrderStatus.AWAITING_PAYMENT && order.getStatus() != OrderStatus.PENDING) {
                throw new AppException(ErrorCode.ORDER_NOT_PAYABLE);
            }

            long orderCode = System.currentTimeMillis() % 900000000000000L;
            long amount = Math.max(1000L, order.getTotalAmount().longValue());
            String desc = truncateDescription("Don " + order.getOrderId());

            try {
                CreatePaymentLinkRequest paymentData = CreatePaymentLinkRequest.builder()
                        .orderCode(orderCode)
                        .amount(amount)
                        .description(desc)
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
    public ResumePaymentOptionsResponse getResumePaymentOptions(int orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        CheckoutBatch batch = order.getCheckoutBatch();
        if (batch != null) {
            List<Order> batchOrders = orderRepository.findByCheckoutBatch_Id(batch.getId());
            List<Order> unpaid = batchOrders.stream()
                    .filter(o -> o.getStatus() == OrderStatus.AWAITING_PAYMENT || o.getStatus() == OrderStatus.PENDING)
                    .toList();

            BigDecimal remainingTotal = unpaid.stream()
                    .map(Order::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            List<ResumePaymentOrderOption> unpaidOptions = unpaid.stream()
                    .map(o -> ResumePaymentOrderOption.builder()
                            .orderId(o.getOrderId())
                            .shopId(o.getShop() != null ? o.getShop().getId() : 0)
                            .shopName(o.getShop() != null ? (o.getShop().getShopName() != null ? o.getShop().getShopName() : o.getShop().getFullName()) : "Shop")
                            .amount(o.getTotalAmount())
                            .status(o.getStatus() != null ? o.getStatus().name() : null)
                            .build())
                    .toList();

            ResumePaymentOptionsResponse.PayAllOption payAll = ResumePaymentOptionsResponse.PayAllOption.builder()
                    .amount(remainingTotal)
                    .checkoutUrl(batch.getCheckoutUrl())
                    .payosOrderCode(batch.getPayosOrderCode())
                    .qrCodeUrl(batch.getQrCodeUrl())
                    .build();

            return ResumePaymentOptionsResponse.builder()
                    .isBatch(batchOrders.size() > 1 && !unpaid.isEmpty())
                    .batchId(batch.getId())
                    .triggerOrderId(orderId)
                    .totalAmount(remainingTotal)
                    .payosOrderCode(batch.getPayosOrderCode())
                    .checkoutUrl(batch.getCheckoutUrl())
                    .qrCodeUrl(batch.getQrCodeUrl())
                    .unpaidOrders(unpaidOptions)
                    .payAllOption(payAll)
                    .build();
        }

        // Single order
        return ResumePaymentOptionsResponse.builder()
                .isBatch(false)
                .batchId(null)
                .triggerOrderId(orderId)
                .totalAmount(order.getTotalAmount())
                .payosOrderCode(null)
                .checkoutUrl(null)
                .qrCodeUrl(null)
                .unpaidOrders(List.of(ResumePaymentOrderOption.builder()
                        .orderId(order.getOrderId())
                        .shopId(order.getShop() != null ? order.getShop().getId() : 0)
                        .shopName(order.getShop() != null ? (order.getShop().getShopName() != null ? order.getShop().getShopName() : order.getShop().getFullName()) : "Shop")
                        .amount(order.getTotalAmount())
                        .status(order.getStatus() != null ? order.getStatus().name() : null)
                        .build()))
                .build();
    }

    private String truncateDescription(String desc) {
        if (desc == null || desc.isBlank()) return "Thanh toan CapNong";
        String normalized = Normalizer.normalize(desc, Normalizer.Form.NFD);
        String nonAccented = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        String sanitized = nonAccented.replaceAll("[^a-zA-Z0-9 ]", "").trim();
        if (sanitized.length() > 25) {
            return sanitized.substring(0, 25);
        }
        return sanitized.isEmpty() ? "Thanh toan CapNong" : sanitized;
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

        // 1. Kiểm tra xem đã xử lý PAID hoặc COMPLETED trước đó chưa (idempotent guard)
        var paymentOpt = paymentRepository.findByPayosOrderCode(orderCode);
        if (paymentOpt.isPresent() && paymentOpt.get().getStatus() == PaymentStatus.PAID) {
            log.info("[PaymentConfirm] Payment already settled as PAID: {}", orderCode);
            return toPaymentResponse(paymentOpt.get());
        }

        var batchOpt = checkoutBatchRepository.findByPayosOrderCode(orderCode);
        if (batchOpt.isPresent() && batchOpt.get().getStatus() == BatchStatus.COMPLETED) {
            log.info("[PaymentConfirm] Batch already settled as COMPLETED: {}", orderCode);
            return getPaymentByOrderCode(orderCode);
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
        if (orderCode == null || orderCode.isBlank()) {
            return;
        }

        // Khóa đồng bộ theo orderCode để tránh Webhook và Client confirm chạy song song cùng lúc
        synchronized (orderCode.intern()) {
            log.info("[PaymentSettle] Settling orderCode to PAID: {}", orderCode);

            List<Order> newlyPaidOrders = new java.util.ArrayList<>();
            BigDecimal totalPaidAmount = BigDecimal.ZERO;
            User buyer = null;

            // 1. Settle Payment đơn lẻ nếu có
            var paymentOpt = paymentRepository.findByPayosOrderCode(orderCode);
            if (paymentOpt.isPresent()) {
                Payment payment = paymentOpt.get();
                if (payment.getStatus() == PaymentStatus.PAID) {
                    log.info("[PaymentSettle] Payment #{} already settled as PAID. Skipping duplicate processing.", payment.getPaymentId());
                    return;
                }

                payment.setStatus(PaymentStatus.PAID);
                payment.setPaymentDate(LocalDateTime.now());
                payment.setUpdatedAt(LocalDateTime.now());
                paymentRepository.save(payment);

                if (payment.getOrder() != null) {
                    Order order = payment.getOrder();
                    if (order.getStatus() == OrderStatus.AWAITING_PAYMENT || order.getStatus() == OrderStatus.PENDING) {
                        order.setStatus(OrderStatus.PAID);
                        orderRepository.save(order);
                        newlyPaidOrders.add(order);
                        totalPaidAmount = payment.getAmount() != null ? payment.getAmount() : order.getTotalAmount();
                        buyer = order.getBuyer();
                        log.info("[PaymentSettle] Single order #{} updated to PAID", order.getOrderId());
                    }

                    if (order.getCheckoutBatch() != null) {
                        CheckoutBatch b = order.getCheckoutBatch();
                        List<Order> batchOrders = orderRepository.findByCheckoutBatch_Id(b.getId());
                        boolean allPaid = batchOrders.stream()
                                .allMatch(o -> o.getStatus() == OrderStatus.PAID || o.getStatus() == OrderStatus.COMPLETED);
                        if (allPaid) {
                            b.setStatus(BatchStatus.COMPLETED);
                            checkoutBatchRepository.save(b);
                            log.info("[PaymentSettle] All orders in batch #{} paid, batch marked COMPLETED", b.getId());
                        }
                    }
                }
            }

            // 2. Settle Batch nếu có
            var batchOpt = checkoutBatchRepository.findByPayosOrderCode(orderCode);
            if (batchOpt.isPresent()) {
                CheckoutBatch batch = batchOpt.get();
                if (batch.getStatus() == BatchStatus.COMPLETED) {
                    log.info("[PaymentSettle] Batch #{} already settled as COMPLETED. Skipping duplicate processing.", batch.getId());
                    return;
                }

                batch.setStatus(BatchStatus.COMPLETED);
                batch.setUpdatedAt(LocalDateTime.now());
                checkoutBatchRepository.save(batch);
                totalPaidAmount = batch.getTotalAmount() != null ? batch.getTotalAmount() : BigDecimal.ZERO;

                List<Order> batchOrders = orderRepository.findByCheckoutBatch_Id(batch.getId());
                for (Order o : batchOrders) {
                    if (o.getStatus() == OrderStatus.AWAITING_PAYMENT || o.getStatus() == OrderStatus.PENDING) {
                        o.setStatus(OrderStatus.PAID);
                        orderRepository.save(o);
                        newlyPaidOrders.add(o);
                        if (buyer == null) {
                            buyer = o.getBuyer();
                        }
                        log.info("[PaymentSettle] Batch order #{} in batch #{} updated to PAID", o.getOrderId(), batch.getId());
                    }
                }
            }

            // 3. Gửi Email Hóa Đơn - Idempotent (Chỉ gửi duy nhất 1 lần khi có đơn vừa chuyển sang PAID)
            if (!newlyPaidOrders.isEmpty() && buyer != null) {
                log.info("[PaymentSettle] Sending single invoice email for orderCode: {} ({} orders) to {}",
                        orderCode, newlyPaidOrders.size(), buyer.getEmail());
                emailService.sendPaymentSuccessInvoiceEmail(buyer, newlyPaidOrders, orderCode, totalPaidAmount);
            }
        }
    }

    @Override
    @Transactional
    public void cancelPaymentByOrderCode(String orderCode) {
        log.info("[PaymentCancel] Cancelling payment for orderCode: {}", orderCode);
        if (orderCode == null || orderCode.isBlank()) {
            return;
        }

        // 1. Kiểm tra nếu orderCode thuộc về CheckoutBatch
        var batchOpt = checkoutBatchRepository.findByPayosOrderCode(orderCode);
        if (batchOpt.isPresent()) {
            CheckoutBatch batch = batchOpt.get();
            if (batch.getStatus() == BatchStatus.COMPLETED) {
                log.warn("[PaymentCancel] Batch #{} already COMPLETED, skip cancel.", batch.getId());
                return;
            }

            batch.setStatus(BatchStatus.CANCELLED);
            batch.setUpdatedAt(LocalDateTime.now());
            checkoutBatchRepository.save(batch);

            List<Order> orders = orderRepository.findByCheckoutBatch_Id(batch.getId());
            for (Order o : orders) {
                if (o.getStatus() == OrderStatus.AWAITING_PAYMENT || o.getStatus() == OrderStatus.PENDING) {
                    restoreOrderStockAndVouchers(o);
                    cartService.restoreOrderItemsToCart(o);
                    o.setStatus(OrderStatus.CANCELLED);
                    orderRepository.save(o);
                    log.info("[PaymentCancel] Restored items to cart and marked Order #{} as CANCELLED", o.getOrderId());
                }
            }

            paymentRepository.findByPayosOrderCode(orderCode).ifPresent(p -> {
                p.setStatus(PaymentStatus.CANCELLED);
                p.setUpdatedAt(LocalDateTime.now());
                paymentRepository.save(p);
            });
            log.info("[PaymentCancel] Cancelled batch #{} successfully", batch.getId());
            return;
        }

        // 2. Kiểm tra nếu orderCode thuộc về Payment đơn lẻ
        paymentRepository.findByPayosOrderCode(orderCode).ifPresent(payment -> {
            if (payment.getStatus() == PaymentStatus.PAID) {
                log.warn("[PaymentCancel] Payment #{} already PAID, skip cancel.", payment.getPaymentId());
                return;
            }

            payment.setStatus(PaymentStatus.CANCELLED);
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);

            Order order = payment.getOrder();
            if (order != null && (order.getStatus() == OrderStatus.AWAITING_PAYMENT || order.getStatus() == OrderStatus.PENDING)) {
                restoreOrderStockAndVouchers(order);
                cartService.restoreOrderItemsToCart(order);
                order.setStatus(OrderStatus.CANCELLED);
                orderRepository.save(order);
                log.info("[PaymentCancel] Restored items to cart and marked Order #{} as CANCELLED", order.getOrderId());

                if (order.getCheckoutBatch() != null) {
                    CheckoutBatch b = order.getCheckoutBatch();
                    List<Order> batchOrders = orderRepository.findByCheckoutBatch_Id(b.getId());
                    boolean allUnpaidOrCancelled = batchOrders.stream()
                            .allMatch(bo -> bo.getStatus() == OrderStatus.CANCELLED);
                    if (allUnpaidOrCancelled) {
                        b.setStatus(BatchStatus.CANCELLED);
                        b.setUpdatedAt(LocalDateTime.now());
                        checkoutBatchRepository.save(b);
                    }
                }
            }
        });
    }

    private void restoreOrderStockAndVouchers(Order order) {
        if (order == null) return;

        // Hoàn kho tồn kho nguyên tử
        List<OrderDetail> details = orderDetailRepository.findByOrder_OrderId(order.getOrderId());
        for (OrderDetail detail : details) {
            int qty = detail.getQuantity() != null ? detail.getQuantity() : 0;
            if (qty <= 0) continue;

            if ("PRODUCT".equals(detail.getItemType()) && detail.getProduct() != null) {
                productRepository.increaseStockAtomic(detail.getProduct().getProductId(), qty);
            } else if ("MYSTERY_BOX".equals(detail.getItemType()) && detail.getMysteryBox() != null) {
                mysteryBoxRepository.increaseStockAtomic(detail.getMysteryBox().getMysteryId(), qty);
            } else if ("BUILD_COMBO".equals(detail.getItemType()) && detail.getBuildCombo() != null) {
                buildComboRepository.increaseStockAtomic(detail.getBuildCombo().getComboId(), qty);
            }
        }

        // Hoàn Voucher
        List<VoucherUsage> usages = voucherUsageRepository.findByOrder(order);
        if (usages != null) {
            for (VoucherUsage usage : usages) {
                Voucher v = usage.getVoucher();
                if (v != null) {
                    v.setUsedCount(Math.max(0, (v.getUsedCount() != null ? v.getUsedCount() : 0) - 1));
                    voucherRepository.save(v);
                }
                if (order.getBuyer() != null && v != null) {
                    voucherUserRepository.findByUser_IdAndVoucher_VoucherId(order.getBuyer().getId(), v.getVoucherId())
                            .ifPresent(vu -> {
                                vu.setQuantityUsed(Math.max(0, (vu.getQuantityUsed() != null ? vu.getQuantityUsed() : 0) - 1));
                                voucherUserRepository.save(vu);
                            });
                }
                voucherUsageRepository.delete(usage);
            }
        }
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
