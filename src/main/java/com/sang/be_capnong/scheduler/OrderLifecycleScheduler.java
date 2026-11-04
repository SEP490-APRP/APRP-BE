package com.sang.be_capnong.scheduler;

import com.sang.be_capnong.entity.Order;
import com.sang.be_capnong.enums.OrderStatus;
import com.sang.be_capnong.repository.OrderRepository;
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

    @Value("${capnong.order.auto-complete-days:3}")
    private int autoCompleteDays;

    @Value("${capnong.payment.expire-minutes:30}")
    private int expireMinutes;

    /** Đơn DELIVERED quá N ngày mà Buyer chưa xác nhận -> COMPLETED và chia tiền vào ví */
    @Scheduled(fixedDelayString = "${capnong.scheduler.auto-complete-delay-ms:600000}", initialDelay = 60000)
    public void autoCompleteDeliveredOrders() {
        if (autoCompleteDays <= 0) return;
        LocalDateTime before = LocalDateTime.now().minusDays(autoCompleteDays);
        for (Order o : orderRepository.findTop200ByStatusAndDeliveredAtBefore(OrderStatus.DELIVERED, before)) {
            try {
                orderService.autoCompleteOrder(o.getOrderId());
                log.info("[Scheduler] Auto-completed order #{}", o.getOrderId());
            } catch (Exception e) {
                log.error("[Scheduler] Auto-complete failed for order #{}: {}", o.getOrderId(), e.getMessage(), e);
            }
        }
    }

    /** Đơn COMPLETED mà chưa được chia tiền (ví dụ dữ liệu cũ) -> chia bù */
    @Scheduled(fixedDelayString = "${capnong.scheduler.settle-delay-ms:600000}", initialDelay = 90000)
    public void settleCompletedOrders() {
        for (Order o : orderRepository.findTop200ByStatusAndSettledAtIsNull(OrderStatus.COMPLETED)) {
            try {
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
}
