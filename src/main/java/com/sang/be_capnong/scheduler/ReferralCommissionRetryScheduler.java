package com.sang.be_capnong.scheduler;

import com.sang.be_capnong.entity.ReferralCommission;
import com.sang.be_capnong.repository.ReferralCommissionRepository;
import com.sang.be_capnong.service.ReferralSettlementService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Quét bù định kỳ (mỗi 15 phút) các khoản hoa hồng PENDING của những đơn hàng đã quyết toán xong (settledAt != null),
 * đảm bảo không bao giờ bị sót hoa hồng khi mạng gián đoạn hoặc sự kiện AFTER_COMMIT gặp lỗi.
 */
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ReferralCommissionRetryScheduler {

    ReferralCommissionRepository referralCommissionRepository;
    ReferralSettlementService referralSettlementService;

    @Scheduled(fixedDelayString = "${capnong.referral.retry-delay-ms:900000}", initialDelay = 180000)
    public void retryPendingCommissionsOfSettledOrders() {
        try {
            List<ReferralCommission> pendings = referralCommissionRepository.findPendingCommissionsOfSettledOrders();
            if (pendings.isEmpty()) {
                return;
            }

            log.info("[ReferralRetryScheduler] Found {} pending referral commissions for settled orders to retry", pendings.size());
            for (ReferralCommission comm : pendings) {
                try {
                    referralSettlementService.settleSingleCommission(comm.getId());
                } catch (Exception e) {
                    log.error("[ReferralRetryScheduler] Failed to settle commission #{}: {}", comm.getId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("[ReferralRetryScheduler] Scheduled scan failed: {}", e.getMessage(), e);
        }
    }
}

