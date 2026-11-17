package com.sang.be_capnong.event;

import com.sang.be_capnong.service.ReferralSettlementService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ReferralSettlementListener {

    ReferralSettlementService referralSettlementService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderSettled(OrderSettledEvent event) {
        log.info("[ReferralListener] Received OrderSettledEvent for order #{}", event.getOrderId());
        try {
            referralSettlementService.settleOrderCommissions(event.getOrderId());
        } catch (Exception e) {
            log.error("[ReferralListener] Failed settling referral commissions for order #{}: {}",
                    event.getOrderId(), e.getMessage(), e);
            // Non-blocking: Scheduler quét bù sẽ retry sau mà không ảnh hưởng quyết toán Shop
        }
    }
}

