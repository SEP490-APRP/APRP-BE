package com.sang.be_capnong.service.impl;

import com.sang.be_capnong.entity.Order;
import com.sang.be_capnong.entity.ReferralCommission;
import com.sang.be_capnong.entity.ReturnRequest;
import com.sang.be_capnong.enums.CommissionStatus;
import com.sang.be_capnong.enums.ReturnStatus;
import com.sang.be_capnong.repository.ReferralCommissionRepository;
import com.sang.be_capnong.repository.ReturnRequestRepository;
import com.sang.be_capnong.service.ReferralSettlementService;
import com.sang.be_capnong.service.WalletService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ReferralSettlementServiceImpl implements ReferralSettlementService {

    ReferralCommissionRepository referralCommissionRepository;
    ReturnRequestRepository returnRequestRepository;
    WalletService walletService;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void settleOrderCommissions(int orderId) {
        List<ReferralCommission> commissions = referralCommissionRepository.findByOrder_OrderId(orderId);
        if (commissions.isEmpty()) {
            return;
        }

        log.info("[ReferralSettlement] Processing {} referral commissions for settled order #{}",
                commissions.size(), orderId);

        for (ReferralCommission comm : commissions) {
            if (comm.getStatus() == CommissionStatus.PENDING) {
                settleSingleCommissionInternal(comm);
            }
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void settleSingleCommission(Long commissionId) {
        ReferralCommission comm = referralCommissionRepository.findById(commissionId).orElse(null);
        if (comm != null && comm.getStatus() == CommissionStatus.PENDING) {
            settleSingleCommissionInternal(comm);
        }
    }

    private void settleSingleCommissionInternal(ReferralCommission comm) {
        BigDecimal finalBase = comm.getBaseAmount();
        BigDecimal finalCommission = comm.getCommissionAmount();

        // Kiểm tra xem dòng hàng này có phát sinh khiếu nại hoàn tiền / trả hàng không
        if (comm.getOrderDetail() != null) {
            List<ReturnRequest> returnRequests = returnRequestRepository
                    .findByOrderDetail_OrderDetailId(comm.getOrderDetail().getOrderDetailId());

            if (returnRequests != null && !returnRequests.isEmpty()) {
                for (ReturnRequest req : returnRequests) {
                    // Chỉ giảm trừ hoa hồng khi hoàn trả hàng đã hoàn tất thành công (COMPLETED)
                    if (req.getStatus() == ReturnStatus.COMPLETED) {
                        BigDecimal itemGross = comm.getOrderDetail().getUnitPrice()
                                .multiply(BigDecimal.valueOf(comm.getOrderDetail().getQuantity()));
                        BigDecimal refundAmount = req.getRefundAmount() != null ? req.getRefundAmount() : BigDecimal.ZERO;

                        if (itemGross.signum() > 0 && refundAmount.signum() > 0) {
                            BigDecimal refundRatio = refundAmount.divide(itemGross, 4, RoundingMode.HALF_UP);
                            BigDecimal remainingFactor = BigDecimal.ONE.subtract(refundRatio).max(BigDecimal.ZERO);

                            finalBase = finalBase.multiply(remainingFactor).setScale(2, RoundingMode.HALF_UP);
                            finalCommission = finalCommission.multiply(remainingFactor).setScale(0, RoundingMode.HALF_UP);
                        }

                        // Nếu hoàn trả toàn bộ hoặc số tiền hoa hồng còn lại <= 0: huỷ hoa hồng
                        if (finalCommission.compareTo(BigDecimal.ZERO) <= 0) {
                            comm.setStatus(CommissionStatus.CANCELLED);
                            comm.setUpdatedAt(LocalDateTime.now());
                            referralCommissionRepository.save(comm);
                            log.info("[ReferralSettlement] Commission #{} marked CANCELLED due to full refund", comm.getId());
                            return;
                        }
                        break;
                    }
                }
            }
        }

        LocalDateTime now = LocalDateTime.now();

        // Tầng Idempotency 2: Cập nhật atomic có điều kiện WHERE status = 'PENDING'
        int updated = referralCommissionRepository.markCompletedAtomic(
                comm.getId(), finalBase, finalCommission, now, now);

        if (updated == 0) {
            log.warn("[ReferralSettlement] Commission #{} already processed or not in PENDING status, skipping",
                    comm.getId());
            return;
        }

        // Tầng Idempotency 3: Cộng tiền vào ví TYPE_BUYER của người giới thiệu (chống cộng trùng bằng referral_commission_id UNIQUE)
        Order order = comm.getOrder();
        String productName = comm.getOrderDetail() != null && comm.getOrderDetail().getProduct() != null
                ? comm.getOrderDetail().getProduct().getProductName()
                : "Nông sản";

        String desc = "Hoa hồng tiếp thị liên kết đơn #" + order.getOrderId() + " (" + productName + "): +" + finalCommission + "đ";

        walletService.creditReferralCommission(
                comm.getReferrer(),
                order,
                comm.getId(),
                finalCommission,
                desc
        );

        log.info("[ReferralSettlement] Successfully settled commission #{} (+{}đ) to referrer #{}, order #{}",
                comm.getId(), finalCommission, comm.getReferrer().getId(), order.getOrderId());
    }
}

