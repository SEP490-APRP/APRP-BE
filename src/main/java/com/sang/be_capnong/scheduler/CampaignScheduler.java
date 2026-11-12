package com.sang.be_capnong.scheduler;

import com.sang.be_capnong.entity.Campaign;
import com.sang.be_capnong.entity.PreOrder;
import com.sang.be_capnong.enums.CampaignStatus;
import com.sang.be_capnong.enums.PreOrderStatus;
import com.sang.be_capnong.repository.CampaignRepository;
import com.sang.be_capnong.repository.PreOrderRepository;
import com.sang.be_capnong.service.CampaignService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Việc nền của chiến dịch giải cứu (DS-01b). Mỗi chiến dịch / đơn chạy trong transaction riêng.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CampaignScheduler {

    private final CampaignRepository campaignRepository;
    private final PreOrderRepository preOrderRepository;
    private final CampaignService campaignService;

    @Value("${capnong.campaign.preorder-expire-minutes:15}")
    private int preOrderExpireMinutes;

    /** Chiến dịch ACTIVE đã qua hạn chốt: đủ ngưỡng -> THRESHOLD_MET + phân bổ; thiếu -> CANCELLED + hoàn cọc */
    @Scheduled(fixedDelayString = "${capnong.scheduler.campaign-evaluate-delay-ms:60000}", initialDelay = 45000)
    public void evaluateDueCampaigns() {
        for (Campaign c : campaignRepository.findTop100ByStatusAndCutoffAtBefore(CampaignStatus.ACTIVE, LocalDateTime.now())) {
            try {
                campaignService.evaluateCampaign(c.getCampaignId());
            } catch (Exception e) {
                log.error("[CampaignScheduler] Evaluate campaign {} failed: {}", c.getCode(), e.getMessage(), e);
            }
        }
    }

    /** Đơn đặt trước chờ cọc quá hạn -> huỷ, trả lại sản lượng cho người khác đặt */
    @Scheduled(fixedDelayString = "${capnong.scheduler.preorder-expire-delay-ms:60000}", initialDelay = 50000)
    public void expireStalePreOrders() {
        LocalDateTime before = LocalDateTime.now().minusMinutes(Math.max(preOrderExpireMinutes, 1));
        for (PreOrder p : preOrderRepository.findTop200ByStatusAndCreatedAtBefore(PreOrderStatus.PENDING, before)) {
            try {
                campaignService.expirePendingPreOrder(p.getPreOrderId());
            } catch (Exception e) {
                log.error("[CampaignScheduler] Expire pre-order {} failed: {}", p.getCode(), e.getMessage(), e);
            }
        }
    }
}
