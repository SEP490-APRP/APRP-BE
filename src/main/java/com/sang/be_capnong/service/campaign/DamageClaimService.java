package com.sang.be_capnong.service.campaign;

import com.sang.be_capnong.dto.request.campaign.DamageClaimRequest;
import com.sang.be_capnong.dto.request.campaign.DamageClaimReviewRequest;
import com.sang.be_capnong.dto.response.campaign.DamageClaimResponse;
import com.sang.be_capnong.entity.PreOrder;
import com.sang.be_capnong.entity.PreOrderDamageClaim;
import com.sang.be_capnong.entity.User;
import com.sang.be_capnong.enums.DamageClaimStatus;
import com.sang.be_capnong.enums.PreOrderStatus;
import com.sang.be_capnong.exception.AppException;
import com.sang.be_capnong.exception.ErrorCode;
import com.sang.be_capnong.repository.PreOrderDamageClaimRepository;
import com.sang.be_capnong.repository.PreOrderRepository;
import com.sang.be_capnong.repository.UserRepository;
import com.sang.be_capnong.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Báo hàng hư cho đơn đặt trước chiến dịch (thay cho hoàn hàng).
 * <ol>
 *   <li>Đơn đã giao: trong {@value #CLAIM_WINDOW_HOURS} giờ, người mua tự cân phần hư, chụp 1–5 ảnh (cân + hàng) và gửi.</li>
 *   <li>Admin xem ảnh, chốt số kg chấp nhận (≤ số kg khai), sàn trả tiền từ ví sàn vào ví người mua = kg x giá/kg.</li>
 *   <li>Không cần gửi hàng về; mỗi đơn chỉ có 1 yêu cầu chờ duyệt, tổng kg báo không vượt số kg của đơn.</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DamageClaimService {

    public static final int CLAIM_WINDOW_HOURS = 24;
    private static final List<DamageClaimStatus> COUNTED = List.of(DamageClaimStatus.PENDING, DamageClaimStatus.APPROVED);

    private final PreOrderDamageClaimRepository claimRepository;
    private final PreOrderRepository preOrderRepository;
    private final UserRepository userRepository;
    private final CampaignEscrowService escrowService;
    private final NotificationService notificationService;

    /** Hạn báo hư của đơn (null nếu chưa giao) */
    public static LocalDateTime deadline(PreOrder p) {
        return p.getDeliveredAt() == null ? null : p.getDeliveredAt().plusHours(CLAIM_WINDOW_HOURS);
    }

    @Transactional
    public DamageClaimResponse submit(int preOrderId, DamageClaimRequest request) {
        User buyer = currentUser();
        PreOrder p = preOrderRepository.findByIdForUpdate(preOrderId)
                .orElseThrow(() -> new AppException(ErrorCode.PRE_ORDER_NOT_FOUND));
        if (p.getBuyer().getId() != buyer.getId()) {
            throw new AppException(ErrorCode.PRE_ORDER_NOT_OWNER);
        }
        if (p.getStatus() != PreOrderStatus.DELIVERED || p.getDeliveredAt() == null) {
            throw new AppException(ErrorCode.PRE_ORDER_INVALID_STATUS);
        }
        if (LocalDateTime.now().isAfter(deadline(p))) {
            throw new AppException(ErrorCode.DAMAGE_CLAIM_EXPIRED);
        }
        if (claimRepository.existsByPreOrder_PreOrderIdAndStatus(preOrderId, DamageClaimStatus.PENDING)) {
            throw new AppException(ErrorCode.DAMAGE_CLAIM_PENDING_EXISTS);
        }
        List<String> photos = request == null || request.getPhotoUrls() == null ? List.of()
                : request.getPhotoUrls().stream().filter(Objects::nonNull).map(String::trim).filter(s -> !s.isEmpty()).toList();
        BigDecimal kg = request == null ? null : request.getClaimedKg();
        if (kg == null || kg.signum() <= 0 || photos.isEmpty() || photos.size() > 5) {
            throw new AppException(ErrorCode.DAMAGE_CLAIM_INVALID);
        }
        BigDecimal left = p.getTotalKg().subtract(nz(claimRepository.sumKg(preOrderId, COUNTED)));
        if (kg.compareTo(left) > 0) {
            throw new AppException(ErrorCode.DAMAGE_CLAIM_KG_EXCEEDED);
        }
        String reason = request.getReason() == null ? null : request.getReason().trim();
        PreOrderDamageClaim c = claimRepository.save(PreOrderDamageClaim.builder()
                .preOrder(p)
                .buyer(buyer)
                .status(DamageClaimStatus.PENDING)
                .claimedKg(kg.setScale(2, RoundingMode.HALF_UP))
                .photoUrls(String.join(",", photos))
                .reason(reason == null || reason.isEmpty() ? null : (reason.length() > 500 ? reason.substring(0, 500) : reason))
                .createdAt(LocalDateTime.now())
                .build());
        userRepository.findByRole_NameIn(List.of("ADMIN")).forEach(a -> notify(a, "Báo hàng hư đơn chiến dịch",
                "Đơn " + p.getCode() + " báo hư " + plain(c.getClaimedKg()) + " kg, kèm " + photos.size() + " ảnh. Vào chiến dịch \""
                        + p.getCampaign().getTitle() + "\" > tab Báo hư để duyệt."));
        return toResponse(c);
    }

    @Transactional(readOnly = true)
    public List<DamageClaimResponse> myClaims(int preOrderId) {
        PreOrder p = preOrderRepository.findById(preOrderId).orElseThrow(() -> new AppException(ErrorCode.PRE_ORDER_NOT_FOUND));
        if (p.getBuyer().getId() != currentUser().getId()) {
            throw new AppException(ErrorCode.PRE_ORDER_NOT_OWNER);
        }
        return claimRepository.findByPreOrder_PreOrderIdOrderByCreatedAtDesc(preOrderId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<DamageClaimResponse> campaignClaims(int campaignId) {
        return claimRepository.findByPreOrder_Campaign_CampaignIdOrderByCreatedAtDesc(campaignId).stream().map(this::toResponse).toList();
    }

    /** Admin duyệt: sàn trả tiền phần hư từ ví sàn vào ví người mua */
    @Transactional
    public DamageClaimResponse approve(int claimId, DamageClaimReviewRequest request) {
        PreOrderDamageClaim c = pending(claimId);
        PreOrder p = c.getPreOrder();
        BigDecimal kg = request != null && request.getApprovedKg() != null ? request.getApprovedKg() : c.getClaimedKg();
        if (kg.signum() <= 0 || kg.compareTo(c.getClaimedKg()) > 0) {
            throw new AppException(ErrorCode.DAMAGE_CLAIM_INVALID);
        }
        BigDecimal pricePerKg = p.getUnitPrice() != null ? p.getUnitPrice() : p.getCampaign().getRescuePricePerKg();
        BigDecimal refund = kg.multiply(pricePerKg).setScale(0, RoundingMode.HALF_UP).setScale(2, RoundingMode.HALF_UP);
        escrowService.compensateDamage(p, refund, kg);
        c.setStatus(DamageClaimStatus.APPROVED);
        c.setApprovedKg(kg.setScale(2, RoundingMode.HALF_UP));
        c.setRefundAmount(refund);
        review(c, request);
        notify(p.getBuyer(), "Báo hàng hư đã được duyệt",
                "CapNong chấp nhận " + plain(c.getApprovedKg()) + " kg hư của đơn " + p.getCode() + ". " + plain(refund)
                        + "đ đã được hoàn vào ví CapNong của bạn. Bạn không cần gửi lại hàng.");
        return toResponse(c);
    }

    @Transactional
    public DamageClaimResponse reject(int claimId, DamageClaimReviewRequest request) {
        if (request == null || request.getNote() == null || request.getNote().isBlank()) {
            throw new AppException(ErrorCode.DAMAGE_CLAIM_NOTE_REQUIRED);
        }
        PreOrderDamageClaim c = pending(claimId);
        c.setStatus(DamageClaimStatus.REJECTED);
        review(c, request);
        notify(c.getPreOrder().getBuyer(), "Báo hàng hư bị từ chối",
                "Yêu cầu báo hư đơn " + c.getPreOrder().getCode() + " bị từ chối: " + c.getAdminNote()
                        + ". Nếu còn trong hạn, bạn có thể gửi lại với ảnh rõ hơn.");
        return toResponse(c);
    }

    private PreOrderDamageClaim pending(int claimId) {
        PreOrderDamageClaim c = claimRepository.findByIdForUpdate(claimId)
                .orElseThrow(() -> new AppException(ErrorCode.DAMAGE_CLAIM_NOT_FOUND));
        if (c.getStatus() != DamageClaimStatus.PENDING) {
            throw new AppException(ErrorCode.DAMAGE_CLAIM_ALREADY_REVIEWED);
        }
        return c;
    }

    private void review(PreOrderDamageClaim c, DamageClaimReviewRequest request) {
        String note = request != null && request.getNote() != null ? request.getNote().trim() : null;
        c.setAdminNote(note == null || note.isEmpty() ? null : (note.length() > 500 ? note.substring(0, 500) : note));
        c.setReviewedBy(currentUser());
        c.setReviewedAt(LocalDateTime.now());
        claimRepository.save(c);
    }

    public DamageClaimResponse toResponse(PreOrderDamageClaim c) {
        PreOrder p = c.getPreOrder();
        var lot = p.getSourceRegistration();
        return DamageClaimResponse.builder()
                .claimId(c.getClaimId())
                .preOrderId(p.getPreOrderId())
                .preOrderCode(p.getCode())
                .campaignId(p.getCampaign().getCampaignId())
                .campaignTitle(p.getCampaign().getTitle())
                .buyerName(p.getRecipientName() != null ? p.getRecipientName() : p.getBuyer().getFullName())
                .buyerPhone(p.getRecipientPhone())
                .orderKg(p.getTotalKg())
                .pricePerKg(p.getUnitPrice())
                .status(c.getStatus().name())
                .claimedKg(c.getClaimedKg())
                .photoUrls(c.getPhotoUrls() == null || c.getPhotoUrls().isBlank() ? List.of() : Arrays.asList(c.getPhotoUrls().split(",")))
                .reason(c.getReason())
                .approvedKg(c.getApprovedKg())
                .refundAmount(c.getRefundAmount())
                .adminNote(c.getAdminNote())
                .sourceShopName(lot != null && lot.getShop() != null
                        ? (lot.getShop().getShopName() != null ? lot.getShop().getShopName() : lot.getShop().getFullName()) : null)
                .deliveredAt(p.getDeliveredAt())
                .createdAt(c.getCreatedAt())
                .reviewedAt(c.getReviewedAt())
                .build();
    }

    private void notify(User user, String title, String message) {
        if (user == null) return;
        try {
            notificationService.sendNotificationToUser(user, title, message, null);
        } catch (Exception e) {
            log.warn("[DamageClaim] Notify user #{} failed: {}", user.getId(), e.getMessage());
        }
    }

    private User currentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null || !auth.getName().matches("\\d+")) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        return userRepository.findById(Integer.parseInt(auth.getName()))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static String plain(BigDecimal v) {
        return v == null ? "0" : v.stripTrailingZeros().toPlainString();
    }
}
