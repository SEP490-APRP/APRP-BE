package com.sang.be_capnong.scheduler;

import com.sang.be_capnong.entity.Order;
import com.sang.be_capnong.enums.OrderStatus;
import com.sang.be_capnong.enums.ReturnStatus;
import com.sang.be_capnong.repository.OrderRepository;
import com.sang.be_capnong.repository.ReturnRequestRepository;
import com.sang.be_capnong.service.OrderService;
import com.sang.be_capnong.service.PaymentService;
import com.sang.be_capnong.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Các việc chạy nền của vòng đời đơn hàng. Mỗi đơn xử lý trong transaction riêng,
 * 1 đơn lỗi không chặn các đơn khác; lần chạy sau sẽ thử lại.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderLifecycleScheduler {

    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final PaymentService paymentService;
    private final WalletService walletService;
    private final ReturnRequestRepository returnRequestRepository;
    private final com.sang.be_capnong.service.ReturnRequestService returnRequestService;
    private final com.sang.be_capnong.service.DispatchService dispatchService;
    private final com.sang.be_capnong.service.ShipperService shipperService;

    /** Điều phối đơn READY: mở rộng bán kính quét shipper, quét lại, cảnh báo chưa có tài xế */
    @Scheduled(fixedDelayString = "${capnong.scheduler.dispatch-delay-ms:20000}", initialDelay = 30000)
    public void dispatchReadyOrders() {
        try {
            dispatchService.runDispatchCycle();
        } catch (Exception e) {
            log.error("[Scheduler] Dispatch cycle failed: {}", e.getMessage(), e);
        }
    }

    /** Shipper mất heartbeat quá hạn -> chuyển offline để không được mời nhận đơn */
    @Scheduled(fixedDelayString = "${capnong.scheduler.stale-shipper-delay-ms:60000}", initialDelay = 60000)
    public void markStaleShippersOffline() {
        try {
            int n = shipperService.markStaleShippersOffline();
            if (n > 0) {
                log.info("[Scheduler] Marked {} stale shipper(s) offline", n);
            }
        } catch (Exception e) {
            log.error("[Scheduler] Stale shipper check failed: {}", e.getMessage(), e);
        }
    }

    /** Thời gian giữ tiền và chờ hoàn hàng sau khi DELIVERED (mặc định 24 giờ) */
    @Value("${capnong.order.auto-complete-hours:24}")
    private int autoCompleteHours;

    @Value("${capnong.payment.expire-minutes:30}")
    private int expireMinutes;

    /** Đơn DELIVERED quá 24h mà Buyer chưa khiếu nại/hoàn hàng -> tự động COMPLETED và chia tiền vào ví */
    @Scheduled(fixedDelayString = "${capnong.scheduler.auto-complete-delay-ms:600000}", initialDelay = 60000)
    public void autoCompleteDeliveredOrders() {
        if (autoCompleteHours <= 0) return;
        LocalDateTime before = LocalDateTime.now().minusHours(autoCompleteHours);
        for (Order o : orderRepository.findTop200ByStatusAndDeliveredAtBefore(OrderStatus.DELIVERED, before)) {
            try {
                if (o.getStatus() == OrderStatus.REFUND_REQUESTED || hasActiveReturnRequest(o.getOrderId())) {
                    log.info("[Scheduler] Order #{} has active return request, skip auto-complete", o.getOrderId());
                    continue;
                }
                orderService.autoCompleteOrder(o.getOrderId());
                log.info("[Scheduler] Auto-completed order #{}", o.getOrderId());
            } catch (Exception e) {
                log.error("[Scheduler] Auto-complete failed for order #{}: {}", o.getOrderId(), e.getMessage(), e);
            }
        }
    }

    /** Đơn COMPLETED mà chưa được chia tiền (hoặc vừa hết 24h tạm giữ) -> quyết toán chia tiền vào ví */
    @Scheduled(fixedDelayString = "${capnong.scheduler.settle-delay-ms:600000}", initialDelay = 90000)
    public void settleCompletedOrders() {
        LocalDateTime twentyFourHoursAgo = LocalDateTime.now().minusHours(autoCompleteHours > 0 ? autoCompleteHours : 24);
        for (Order o : orderRepository.findTop200ByStatusInAndSettledAtIsNull(java.util.List.of(OrderStatus.COMPLETED, OrderStatus.REFUNDED))) {
            try {
                if (o.getCompletedAt() == null && o.getStatus() == OrderStatus.COMPLETED && o.getDeliveredAt() != null && o.getDeliveredAt().isAfter(twentyFourHoursAgo)) {
                    log.debug("[Scheduler] Order #{} within 24h return window, defer settlement", o.getOrderId());
                    continue;
                }
                if (hasActiveReturnRequest(o.getOrderId())) {
                    log.info("[Scheduler] Order #{} has active return request, defer settlement", o.getOrderId());
                    continue;
                }
                walletService.settleOrder(o.getOrderId());
            } catch (Exception e) {
                log.error("[Scheduler] Settlement failed for order #{}: {}", o.getOrderId(), e.getMessage(), e);
            }
        }
    }

    /** Đơn PayOS chờ thanh toán quá hạn -> huỷ, trả kho, trả voucher, trả hàng về giỏ */
    @Scheduled(fixedDelayString = "${capnong.scheduler.expire-delay-ms:300000}", initialDelay = 120000)
    public void expireUnpaidOrders() {
        if (expireMinutes <= 0) return;
        LocalDateTime before = LocalDateTime.now().minusMinutes(expireMinutes);
        for (Order o : orderRepository.findTop200ByStatusAndCreatedAtBefore(OrderStatus.AWAITING_PAYMENT, before)) {
            try {
                paymentService.expireAwaitingOrder(o.getOrderId());
            } catch (Exception e) {
                log.error("[Scheduler] Expire failed for order #{}: {}", o.getOrderId(), e.getMessage(), e);
            }
        }
    }

    
    /** Yêu cầu hoàn hàng đã được Admin duyệt (ADMIN_APPROVED) quá 24h mà Shop chưa chọn phương án lấy lại hàng hay không
     * -> Tự động xử lý hoàn tiền ngay không cần lấy lại hàng (isReturnGoods = false) để giải phóng tiền cho người mua */
    @Scheduled(fixedDelayString = "${capnong.scheduler.auto-admin-approved-delay-ms:600000}", initialDelay = 120000)
    public void autoResolveAdminApprovedReturns() {
        LocalDateTime twentyFourHoursAgo = LocalDateTime.now().minusHours(24);
        for (com.sang.be_capnong.entity.ReturnRequest r : returnRequestRepository.findByStatus(ReturnStatus.ADMIN_APPROVED, org.springframework.data.domain.PageRequest.of(0, 50)).getContent()) {
            if (r.getUpdatedAt() != null && r.getUpdatedAt().isBefore(twentyFourHoursAgo)) {
                try {
                    returnRequestService.autoResolveAdminApprovedReturn(r.getReturnId());
                    log.info("[Scheduler] Auto-resolved ADMIN_APPROVED return #{} after 24h inactivity from shop", r.getReturnId());
                } catch (Exception e) {
                    log.error("[Scheduler] Failed auto-resolve return #{}: {}", r.getReturnId(), e.getMessage(), e);
                }
            }
        }
    }

    private boolean hasActiveReturnRequest(int orderId) {
        return returnRequestRepository.findByOrderDetail_Order_OrderId(orderId).stream()
                .anyMatch(r -> r.getStatus() != ReturnStatus.COMPLETED
                        && r.getStatus() != ReturnStatus.REJECTED
                        && r.getStatus() != ReturnStatus.CANCELLED
                        && r.getStatus() != ReturnStatus.RETURN_FAILED);
    }
}
