package com.sang.be_capnong.scheduler;

import com.sang.be_capnong.entity.Campaign;
import com.sang.be_capnong.entity.PreOrder;
import com.sang.be_capnong.enums.CampaignStatus;
import com.sang.be_capnong.enums.PreOrderStatus;
import com.sang.be_capnong.repository.CampaignRepository;
import com.sang.be_capnong.repository.PreOrderRepository;
import com.sang.be_capnong.service.CampaignService;
import com.sang.be_capnong.service.campaign.CampaignProposalService;
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
    private final CampaignProposalService proposalService;

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

    /** Tới hạn chốt góp hàng nhà vườn: tổng kg góp (chờ duyệt + đã duyệt) chưa đủ ngưỡng tối thiểu -> tự huỷ, báo nhà vườn */
    @Scheduled(fixedDelayString = "${capnong.scheduler.campaign-farm-cutoff-delay-ms:60000}", initialDelay = 55000)
    public void evaluateFarmCutoffs() {
        for (Campaign c : campaignRepository.findTop100ByStatusInAndFarmCutoffAtLessThanEqualAndFarmCutoffCheckedAtIsNull(
                java.util.List.of(CampaignStatus.DRAFT, CampaignStatus.ACTIVE), LocalDateTime.now())) {
            try {
                if (campaignService.evaluateFarmCutoff(c.getCampaignId())) {
                    log.info("[CampaignScheduler] Campaign {} cancelled at farm cutoff (not enough farm supply)", c.getCode());
                }
            } catch (Exception e) {
                log.error("[CampaignScheduler] Farm cutoff check for campaign {} failed: {}", c.getCode(), e.getMessage(), e);
            }
        }
    }

    /** Chiến dịch nháp tới đúng ngày giờ đăng bài admin chọn -> tự gửi lời kêu gọi nhà vườn góp hàng */
    @Scheduled(fixedDelayString = "${capnong.scheduler.campaign-publish-delay-ms:30000}", initialDelay = 40000)
    public void publishScheduledCampaigns() {
        for (Campaign c : campaignRepository.findTop100ByStatusAndPublishAtLessThanEqualAndAutoPublishFailedAtIsNullAndCallPostedAtIsNull(
                CampaignStatus.DRAFT, LocalDateTime.now())) {
            try {
                if (campaignService.autoPublishCampaign(c.getCampaignId())) {
                    log.info("[CampaignScheduler] Auto-posted farm call for campaign {} at {}", c.getCode(), c.getPublishAt());
                }
            } catch (Exception e) {
                log.error("[CampaignScheduler] Auto-publish campaign {} failed: {}", c.getCode(), e.getMessage(), e);
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

    /** Lời mời cùng đề xuất chiến dịch quá 72 giờ không trả lời -> hết hạn */
    @Scheduled(fixedDelayString = "${capnong.scheduler.proposal-invite-expire-delay-ms:300000}", initialDelay = 60000)
    public void expireProposalInvites() {
        try {
            int n = proposalService.expireStaleInvites();
            if (n > 0) log.info("[CampaignScheduler] Expired {} proposal invites", n);
        } catch (Exception e) {
            log.error("[CampaignScheduler] Expire proposal invites failed: {}", e.getMessage(), e);
        }
    }
}
