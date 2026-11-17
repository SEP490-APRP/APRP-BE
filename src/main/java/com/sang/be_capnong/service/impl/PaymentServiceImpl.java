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
import com.sang.be_capnong.service.WalletService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
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
    final UserRepository userRepository;
    final WalletService walletService;
    final com.sang.be_capnong.repository.WalletTopupRepository walletTopupRepository;
    final com.sang.be_capnong.service.ReferralService referralService;

    /** Số phút giữ đơn chờ thanh toán PayOS. 0 = không tự huỷ */
    @Value("${capnong.payment.expire-minutes:30}")
    int expireMinutes;

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
        User me = currentUser();

        // Trường hợp 1: Thanh toán cho một CheckoutBatch (gồm nhiều order)
        if (request.getBatchId() != null) {
            CheckoutBatch batch = checkoutBatchRepository.findById(request.getBatchId())
                    .orElseThrow(() -> new AppException(ErrorCode.BATCH_NOT_FOUND));
            assertOwner(me, batch.getBuyer());

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

            // Huỷ mọi link cũ còn mở (link batch trước đó + link lẻ của từng đơn) để khách không trả 2 lần
            cancelOpenLink(batch.getPayosOrderCode(), "Tao link thanh toan moi");
            for (Order o : unpaid) {
                cancelOpenSingleOrderLinks(o, "Gop vao thanh toan batch");
            }

            BigDecimal remainingTotal = unpaid.stream()
                    .map(Order::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            long orderCode = nextOrderCode();
            long amount = remainingTotal.setScale(0, java.math.RoundingMode.HALF_UP).longValue();
            String desc = truncateDescription("Batch " + batch.getId());

            try {
                CheckoutData checkoutData = createLink(orderCode, amount, desc, returnUrl, cancelUrl);

                batch.setPayosOrderCode(String.valueOf(orderCode));
                batch.setCheckoutUrl(checkoutData.checkoutUrl());
                batch.setQrCodeUrl(checkoutData.qrCode());
                batch.setTotalAmount(remainingTotal);
                batch.setStatus(BatchStatus.PENDING);
                batch.setUpdatedAt(LocalDateTime.now());
                checkoutBatchRepository.save(batch);

                Payment payment = Payment.builder()
                        .amount(remainingTotal)
                        .paymentGateway("PAYOS")
                        .status(PaymentStatus.PENDING)
                        .payosOrderCode(String.valueOf(orderCode))
                        .checkoutUrl(checkoutData.checkoutUrl())
                        .qrCodeUrl(checkoutData.qrCode())
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
                        .checkoutUrl(checkoutData.checkoutUrl())
                        .qrCodeUrl(checkoutData.qrCode())
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
            assertOwner(me, order.getBuyer());

            if (order.getStatus() != OrderStatus.AWAITING_PAYMENT && order.getStatus() != OrderStatus.PENDING) {
                throw new AppException(ErrorCode.ORDER_NOT_PAYABLE);
            }

            // Huỷ link batch đang mở (nó đã tính cả tiền đơn này) và link lẻ cũ của đơn này
            cancelOpenBatchLink(order.getCheckoutBatch(), "Khach tra le tung don");
            cancelOpenSingleOrderLinks(order, "Tao link thanh toan moi");

            long orderCode = nextOrderCode();
            long amount = order.getTotalAmount().setScale(0, java.math.RoundingMode.HALF_UP).longValue();
            String desc = truncateDescription("Don " + order.getOrderId());

            try {
                CheckoutData checkoutData = createLink(orderCode, amount, desc, returnUrl, cancelUrl);

                Payment payment = Payment.builder()
                        .order(order)
                        .amount(order.getTotalAmount())
                        .paymentGateway("PAYOS")
                        .status(PaymentStatus.PENDING)
                        .payosOrderCode(String.valueOf(orderCode))
                        .checkoutUrl(checkoutData.checkoutUrl())
                        .qrCodeUrl(checkoutData.qrCode())
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
                        .checkoutUrl(checkoutData.checkoutUrl())
                        .qrCodeUrl(checkoutData.qrCode())
                        .createdAt(savedPayment.getCreatedAt())
                        .build();

            } catch (Exception e) {
                log.error("Failed to create PayOS payment link for order {}: {}", order.getOrderId(), e.getMessage(), e);
                throw new AppException(ErrorCode.PAYMENT_FAILED);
            }
        }

        throw new AppException(ErrorCode.INVALID_KEY);
    }

    private record CheckoutData(String checkoutUrl, String qrCode) {
    }

    private CheckoutData createLink(long orderCode, long amount, String desc, String returnUrl, String cancelUrl) {
        CreatePaymentLinkRequest.CreatePaymentLinkRequestBuilder b = CreatePaymentLinkRequest.builder()
                .orderCode(orderCode)
                .amount(amount)
                .description(desc)
                .returnUrl(returnUrl)
                .cancelUrl(cancelUrl);
        if (expireMinutes > 0) {
            // Link tự hết hạn cùng lúc với đơn chờ thanh toán (OrderLifecycleScheduler huỷ đơn quá hạn)
            b.expiredAt(java.time.Instant.now().plusSeconds(expireMinutes * 60L).getEpochSecond());
        }
        CreatePaymentLinkResponse res = payOS.paymentRequests().create(b.build());
        return new CheckoutData(res.getCheckoutUrl(), res.getQrCode());
    }

    /**
     * Mã PayOS duy nhất: mili-giây x 1000 + 3 chữ số ngẫu nhiên (vẫn nhỏ hơn 2^53 theo giới hạn PayOS).
     * Bản cũ dùng thuần mili-giây nên 2 yêu cầu cùng 1 ms bị trùng mã.
     */
    private long nextOrderCode() {
        for (int i = 0; i < 5; i++) {
            long code = System.currentTimeMillis() * 1000L + java.util.concurrent.ThreadLocalRandom.current().nextInt(1000);
            String c = String.valueOf(code);
            if (paymentRepository.findByPayosOrderCode(c).isEmpty() && checkoutBatchRepository.findByPayosOrderCode(c).isEmpty()
                    && !walletTopupRepository.existsByPayosOrderCode(c)) {
                return code;
            }
        }
        throw new AppException(ErrorCode.PAYMENT_FAILED);
    }

    // ───────────────── Huỷ link PayOS còn mở ─────────────────

    @Override
    @Transactional
    public void cancelOpenPayosLinksForOrder(Order order) {
        if (order == null) return;
        cancelOpenSingleOrderLinks(order, "Don da huy");
        cancelOpenBatchLink(order.getCheckoutBatch(), "Mot don trong batch da huy");
    }

    private void cancelOpenSingleOrderLinks(Order order, String reason) {
        for (Payment p : paymentRepository.findByOrder_OrderId(order.getOrderId())) {
            if (p.getStatus() == PaymentStatus.PENDING) {
                cancelOpenLink(p.getPayosOrderCode(), reason);
            }
        }
    }

    private void cancelOpenBatchLink(CheckoutBatch batch, String reason) {
        if (batch == null || batch.getStatus() == BatchStatus.COMPLETED) return;
        if (cancelOpenLink(batch.getPayosOrderCode(), reason)) {
            // Giữ payosOrderCode để webhook trễ (nếu có) vẫn tìm được batch, chỉ gỡ link đã chết
            batch.setCheckoutUrl(null);
            batch.setQrCodeUrl(null);
            batch.setUpdatedAt(LocalDateTime.now());
            checkoutBatchRepository.save(batch);
        }
    }

    /** Huỷ link trên PayOS và đánh dấu Payment CANCELLED. Trả về true nếu có link PENDING bị huỷ. */
    private boolean cancelOpenLink(String orderCode, String reason) {
        if (orderCode == null || orderCode.isBlank()) return false;
        var paymentOpt = paymentRepository.findByPayosOrderCode(orderCode);
        if (paymentOpt.isEmpty() || paymentOpt.get().getStatus() != PaymentStatus.PENDING) return false;
        try {
            payOS.paymentRequests().cancel(Long.parseLong(orderCode), reason);
            log.info("[PaymentCancel] Cancelled PayOS link {} ({})", orderCode, reason);
        } catch (Exception e) {
            // Link có thể đã hết hạn / đã huỷ phía PayOS. Nếu khách vẫn trả, settlePaidOrderCode sẽ ghi nhận hoàn tiền.
            log.warn("[PaymentCancel] PayOS cancel failed for {}: {}", orderCode, e.getMessage());
        }
        Payment p = paymentOpt.get();
        p.setStatus(PaymentStatus.CANCELLED);
        p.setUpdatedAt(LocalDateTime.now());
        paymentRepository.save(p);
        return true;
    }

    @Override
    public ResumePaymentOptionsResponse getResumePaymentOptions(int orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
        assertOwner(currentUser(), order.getBuyer());

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
        assertCanAccessOrderCode(currentUser(), orderCode);
        return loadPaymentByOrderCode(orderCode);
    }

    private PaymentResponse loadPaymentByOrderCode(String orderCode) {
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
            return loadPaymentByOrderCode(orderCode);
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

        return loadPaymentByOrderCode(orderCode);
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
                    // Mã PayOS có thể là lệnh nạp ví (WalletTopup) hoặc thanh toán đơn
                    if (!walletService.settleTopupIfExists(orderCode)) {
                        settlePaidOrderCode(orderCode);
                    }
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

            var paymentOpt = paymentRepository.findByPayosOrderCode(orderCode);
            if (paymentOpt.isPresent() && paymentOpt.get().getStatus() == PaymentStatus.PAID) {
                log.info("[PaymentSettle] Payment #{} already settled as PAID. Skipping duplicate processing.", paymentOpt.get().getPaymentId());
                return;
            }
            var batchOpt = checkoutBatchRepository.findByPayosOrderCode(orderCode);
            if (batchOpt.isPresent() && batchOpt.get().getStatus() == BatchStatus.COMPLETED && paymentOpt.isEmpty()) {
                log.info("[PaymentSettle] Batch #{} already settled as COMPLETED. Skipping duplicate processing.", batchOpt.get().getId());
                return;
            }

            BigDecimal paidAmount = BigDecimal.ZERO;
            if (paymentOpt.isPresent()) {
                Payment payment = paymentOpt.get();
                paidAmount = payment.getAmount() != null ? payment.getAmount() : BigDecimal.ZERO;
                payment.setStatus(PaymentStatus.PAID);
                payment.setPaymentDate(LocalDateTime.now());
                payment.setUpdatedAt(LocalDateTime.now());
                paymentRepository.save(payment);
            } else if (batchOpt.isPresent()) {
                paidAmount = batchOpt.get().getTotalAmount() != null ? batchOpt.get().getTotalAmount() : BigDecimal.ZERO;
            }

            // Danh sách đơn mà mã thanh toán này chi trả
            List<Order> coveredOrders = new java.util.ArrayList<>();
            if (paymentOpt.isPresent() && paymentOpt.get().getOrder() != null) {
                coveredOrders.add(paymentOpt.get().getOrder());
            } else if (batchOpt.isPresent()) {
                coveredOrders.addAll(orderRepository.findByCheckoutBatch_Id(batchOpt.get().getId()));
            }

            List<Order> notPayable = new java.util.ArrayList<>();
            BigDecimal appliedAmount = BigDecimal.ZERO;
            for (Order o : coveredOrders) {
                if (o.getStatus() == OrderStatus.AWAITING_PAYMENT || o.getStatus() == OrderStatus.PENDING) {
                    o.setStatus(OrderStatus.PAID);
                    orderRepository.save(o);
                    // Tiền đã vào sàn -> Shop và Sàn thấy phần của mình ở mục "tạm giữ"
                    walletService.holdShopAndPlatformIncome(o.getOrderId());
                    newlyPaidOrders.add(o);
                    appliedAmount = appliedAmount.add(o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO);
                    if (buyer == null) buyer = o.getBuyer();
                    log.info("[PaymentSettle] Order #{} updated to PAID by orderCode {}", o.getOrderId(), orderCode);
                } else {
                    notPayable.add(o);
                }
            }
            totalPaidAmount = appliedAmount;

            // Tiền khách trả dư (đơn đã huỷ / đã được trả bằng link khác) -> ghi nhận để sàn hoàn lại
            BigDecimal overpaid = paidAmount.subtract(appliedAmount);
            if (overpaid.signum() > 0) {
                recordOverpayment(orderCode, overpaid, notPayable, coveredOrders);
            }

            // Cập nhật trạng thái batch
            java.util.Set<CheckoutBatch> batches = new java.util.LinkedHashSet<>();
            batchOpt.ifPresent(batches::add);
            for (Order o : coveredOrders) {
                if (o.getCheckoutBatch() != null) batches.add(o.getCheckoutBatch());
            }
            for (CheckoutBatch b : batches) {
                List<Order> batchOrders = orderRepository.findByCheckoutBatch_Id(b.getId());
                boolean anyUnpaid = batchOrders.stream()
                        .anyMatch(o -> o.getStatus() == OrderStatus.AWAITING_PAYMENT || o.getStatus() == OrderStatus.PENDING);
                boolean anyPaid = batchOrders.stream()
                        .anyMatch(o -> o.getStatus() != OrderStatus.CANCELLED && o.getStatus() != OrderStatus.AWAITING_PAYMENT && o.getStatus() != OrderStatus.PENDING);
                if (!anyUnpaid) {
                    b.setStatus(anyPaid ? BatchStatus.COMPLETED : BatchStatus.CANCELLED);
                    b.setUpdatedAt(LocalDateTime.now());
                    checkoutBatchRepository.save(b);
                }
            }

            // Gửi Email Hóa Đơn - Idempotent (Chỉ gửi duy nhất 1 lần khi có đơn vừa chuyển sang PAID)
            if (!newlyPaidOrders.isEmpty() && buyer != null) {
                log.info("[PaymentSettle] Sending single invoice email for orderCode: {} ({} orders) to {}",
                        orderCode, newlyPaidOrders.size(), buyer.getEmail());
                emailService.sendPaymentSuccessInvoiceEmail(buyer, newlyPaidOrders, orderCode, totalPaidAmount);
            }
        }
    }

    /** Ghi số tiền cần hoàn vào các đơn liên quan (orders.refund_amount) để Admin xử lý ở /wallets/refunds */
    private void recordOverpayment(String orderCode, BigDecimal overpaid, List<Order> notPayable, List<Order> covered) {
        log.warn("[PaymentSettle] orderCode {} overpaid by {} (orders not payable: {})", orderCode, overpaid,
                notPayable.stream().map(Order::getOrderId).toList());
        BigDecimal remaining = overpaid;
        List<Order> targets = notPayable.isEmpty() ? covered : notPayable;
        for (int i = 0; i < targets.size() && remaining.signum() > 0; i++) {
            Order o = targets.get(i);
            boolean last = i == targets.size() - 1;
            BigDecimal part = last ? remaining : remaining.min(o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO);
            if (part.signum() <= 0) continue;
            BigDecimal current = o.getRefundAmount() != null ? o.getRefundAmount() : BigDecimal.ZERO;
            o.setRefundAmount(current.add(part));
            String why = o.getStatus() == OrderStatus.CANCELLED
                    ? "Khách thanh toán (mã " + orderCode + ") sau khi đơn đã huỷ"
                    : "Thanh toán trùng (mã " + orderCode + ")";
            o.setRefundReason(o.getRefundReason() == null ? why : o.getRefundReason() + "; " + why);
            orderRepository.save(o);
            remaining = remaining.subtract(part);
        }
    }

    @Override
    @Transactional
    public void cancelPaymentByOrderCode(String orderCode) {
        log.info("[PaymentCancel] Cancelling payment for orderCode: {}", orderCode);
        if (orderCode == null || orderCode.isBlank()) {
            return;
        }
        assertCanAccessOrderCode(currentUser(), orderCode);

        // Huỷ link phía PayOS trước, để khách không thể chuyển khoản cho đơn sắp bị huỷ
        var openPayment = paymentRepository.findByPayosOrderCode(orderCode);
        if (openPayment.isPresent() && openPayment.get().getStatus() == PaymentStatus.PENDING) {
            try {
                payOS.paymentRequests().cancel(Long.parseLong(orderCode), "Khach huy thanh toan");
            } catch (Exception e) {
                log.warn("[PaymentCancel] PayOS cancel failed for {}: {}", orderCode, e.getMessage());
            }
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

    @Override
    @Transactional
    public boolean expireAwaitingOrder(int orderId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null || order.getStatus() != OrderStatus.AWAITING_PAYMENT) {
            return false;
        }
        // Khách có thể vừa trả xong mà webhook chưa tới: hỏi PayOS trước khi huỷ
        List<String> codes = new java.util.ArrayList<>();
        paymentRepository.findByOrder_OrderId(orderId).stream()
                .filter(p -> p.getStatus() == PaymentStatus.PENDING)
                .forEach(p -> codes.add(p.getPayosOrderCode()));
        if (order.getCheckoutBatch() != null && order.getCheckoutBatch().getPayosOrderCode() != null) {
            codes.add(order.getCheckoutBatch().getPayosOrderCode());
        }
        for (String code : codes) {
            try {
                PaymentLink link = payOS.paymentRequests().get(Long.parseLong(code));
                if (link != null && PaymentLinkStatus.PAID.equals(link.getStatus())) {
                    settlePaidOrderCode(code);
                    return false;
                }
            } catch (Exception e) {
                log.warn("[PaymentExpire] Cannot check PayOS link {}: {}", code, e.getMessage());
            }
        }

        cancelOpenPayosLinksForOrder(order);
        restoreOrderStockAndVouchers(order);
        cartService.restoreOrderItemsToCart(order);
        referralService.handleOrderCancelled(order.getOrderId());
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        CheckoutBatch b = order.getCheckoutBatch();
        if (b != null && orderRepository.findByCheckoutBatch_Id(b.getId()).stream()
                .allMatch(o -> o.getStatus() == OrderStatus.CANCELLED)) {
            b.setStatus(BatchStatus.CANCELLED);
            b.setUpdatedAt(LocalDateTime.now());
            checkoutBatchRepository.save(b);
        }
        log.info("[PaymentExpire] Order #{} expired after {} minutes without payment, cancelled", orderId, expireMinutes);
        return true;
    }

    // ───────────────── Quyền truy cập ─────────────────

    private User currentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null || !auth.getName().matches("\\d+")) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        return userRepository.findById(Integer.parseInt(auth.getName()))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    private static boolean isAdmin(User u) {
        return u.getRole() != null && ("ADMIN".equalsIgnoreCase(u.getRole().getName()) || "STAFF".equalsIgnoreCase(u.getRole().getName()));
    }

    private static void assertOwner(User me, User owner) {
        if (isAdmin(me)) return;
        if (owner == null || owner.getId() != me.getId()) {
            throw new AppException(ErrorCode.PAYMENT_NOT_OWNER);
        }
    }

    /** Chỉ Buyer sở hữu đơn/batch (hoặc Admin) được xem / huỷ thanh toán theo orderCode */
    private void assertCanAccessOrderCode(User me, String orderCode) {
        if (isAdmin(me)) return;
        var paymentOpt = paymentRepository.findByPayosOrderCode(orderCode);
        if (paymentOpt.isPresent() && paymentOpt.get().getOrder() != null) {
            assertOwner(me, paymentOpt.get().getOrder().getBuyer());
            return;
        }
        var batchOpt = checkoutBatchRepository.findByPayosOrderCode(orderCode);
        if (batchOpt.isPresent()) {
            assertOwner(me, batchOpt.get().getBuyer());
            return;
        }
        throw new AppException(ErrorCode.PAYMENT_NOT_FOUND);
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
