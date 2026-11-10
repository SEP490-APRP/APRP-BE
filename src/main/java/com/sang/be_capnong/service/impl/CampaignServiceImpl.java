package com.sang.be_capnong.service.impl;

import com.sang.be_capnong.dto.request.campaign.*;
import com.sang.be_capnong.dto.response.PageResponse;
import com.sang.be_capnong.dto.response.campaign.CampaignResponse;
import com.sang.be_capnong.dto.response.campaign.FarmRegistrationResponse;
import com.sang.be_capnong.dto.response.campaign.PreOrderResponse;
import com.sang.be_capnong.entity.*;
import com.sang.be_capnong.enums.*;
import com.sang.be_capnong.exception.AppException;
import com.sang.be_capnong.exception.ErrorCode;
import com.sang.be_capnong.repository.*;
import com.sang.be_capnong.service.CampaignService;
import com.sang.be_capnong.service.NotificationService;
import com.sang.be_capnong.service.campaign.CampaignEscrowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLink;
import vn.payos.model.v2.paymentRequests.PaymentLinkStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Chiến dịch giải cứu nhiều nhà vườn (Review 2 - F-01 / WF-01, GAP-01..04).
 * <p>
 * Admin tạo chiến dịch -> đăng lời kêu gọi -> nhà vườn đăng ký lô (kg dư + ngày thu hoạch) -> Admin duyệt, mở bán
 * -> người mua đặt thùng 10/20/50 kg và cọc PayOS -> hết hạn chốt: đủ ngưỡng thì phân bổ cho nhà vườn, thiếu thì huỷ + hoàn cọc
 * -> gom hàng tại điểm tập kết (cân, kiểm, QR) -> hub -> shipper giao -> hoàn tất: giải ngân cọc, chi trả nhà vườn.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CampaignServiceImpl implements CampaignService {

    private static final List<PreOrderStatus> PAID_STATUSES = List.of(
            PreOrderStatus.DEPOSIT_PAID, PreOrderStatus.PROCESSING, PreOrderStatus.READY,
            PreOrderStatus.DELIVERED, PreOrderStatus.FAILED);
    private static final List<PreOrderStatus> IN_PROGRESS_STATUSES = List.of(
            PreOrderStatus.DEPOSIT_PAID, PreOrderStatus.PROCESSING, PreOrderStatus.READY);
    private static final List<PreOrderStatus> PACKED_STATUSES = List.of(
            PreOrderStatus.PROCESSING, PreOrderStatus.READY, PreOrderStatus.DELIVERED, PreOrderStatus.FAILED);
    private static final DateTimeFormatter CODE_TIME = DateTimeFormatter.ofPattern("yyMMddHHmmss");

    private final CampaignRepository campaignRepository;
    private final CampaignFarmRegistrationRepository registrationRepository;
    private final PreOrderRepository preOrderRepository;
    private final CampaignDepositRepository depositRepository;
    private final PaymentRepository paymentRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final CampaignEscrowService escrowService;
    private final PayOS payOS;

    @Value("${capnong.campaign.preorder-expire-minutes:15}")
    private int preOrderExpireMinutes;

    @Value("${capnong.wallet.farmer-fee-rate:0.10}")
    private BigDecimal farmerFeeRate;

    @Value("${payos.return-url:http://localhost:3000/payment/result}")
    private String defaultReturnUrl;

    @Value("${payos.cancel-url:http://localhost:3000/payment/result}")
    private String defaultCancelUrl;

    // ═════════════════════════════ ADMIN ═════════════════════════════

    @Override
    @Transactional
    public CampaignResponse createCampaign(CampaignUpsertRequest request) {
        validateUpsert(request);
        LocalDateTime now = LocalDateTime.now();
        Campaign c = Campaign.builder()
                .code(nextCode("CD"))
                .status(CampaignStatus.DRAFT)
                .createdBy(currentUser())
                .createdAt(now)
                .updatedAt(now)
                .build();
        applyUpsert(c, request);
        return toCampaignResponse(campaignRepository.save(c));
    }

    @Override
    @Transactional
    public CampaignResponse updateCampaign(int campaignId, CampaignUpsertRequest request) {
        Campaign c = lockCampaign(campaignId);
        requireStatus(c, CampaignStatus.DRAFT);
        validateUpsert(request);
        applyUpsert(c, request);
        c.setUpdatedAt(LocalDateTime.now());
        return toCampaignResponse(campaignRepository.save(c));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CampaignResponse> getAdminCampaigns(String status, int page, int size) {
        PageRequest pr = PageRequest.of(Math.max(page, 0), clampSize(size));
        Page<Campaign> result;
        if (status == null || status.isBlank()) {
            result = campaignRepository.findAllByOrderByCreatedAtDesc(pr);
        } else {
            result = campaignRepository.findByStatusInOrderByCreatedAtDesc(List.of(parseStatus(status)), pr);
        }
        return PageResponse.from(result, result.getContent().stream().map(this::toCampaignResponse).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CampaignResponse getAdminCampaign(int campaignId) {
        return toCampaignResponse(findCampaign(campaignId));
    }

    @Override
    @Transactional
    public CampaignResponse postFarmCall(int campaignId) {
        Campaign c = lockCampaign(campaignId);
        requireStatus(c, CampaignStatus.DRAFT);
        if (c.getCallPostedAt() == null) {
            c.setCallPostedAt(LocalDateTime.now());
            c.setUpdatedAt(LocalDateTime.now());
            campaignRepository.save(c);
            for (User shop : userRepository.findByRole_NameInAndStatus(List.of("SHOP"), UserStatus.ACTIVE)) {
                notify(shop, "Lời kêu gọi chiến dịch giải cứu mới",
                        "CapNong cần " + plain(c.getTargetVolumeKg()) + " kg cho chiến dịch \"" + c.getTitle()
                                + "\". Bấm tham gia và khai số kg dư cùng ngày thu hoạch trước " + c.getCutoffAt().toLocalDate() + ".");
            }
        }
        return toCampaignResponse(c);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FarmRegistrationResponse> getRegistrations(int campaignId) {
        findCampaign(campaignId);
        return registrationRepository.findByCampaign_CampaignIdOrderByCreatedAtAsc(campaignId).stream()
                .map(this::toRegistrationResponse).toList();
    }

    @Override
    @Transactional
    public FarmRegistrationResponse approveRegistration(int registrationId, CampaignNoteRequest request) {
        return reviewRegistration(registrationId, FarmRegistrationStatus.APPROVED, request);
    }

    @Override
    @Transactional
    public FarmRegistrationResponse rejectRegistration(int registrationId, CampaignNoteRequest request) {
        return reviewRegistration(registrationId, FarmRegistrationStatus.REJECTED, request);
    }

    private FarmRegistrationResponse reviewRegistration(int registrationId, FarmRegistrationStatus decision, CampaignNoteRequest request) {
        CampaignFarmRegistration r = findRegistration(registrationId);
        Campaign c = lockCampaign(r.getCampaign().getCampaignId());
        if (c.getStatus() != CampaignStatus.DRAFT && c.getStatus() != CampaignStatus.ACTIVE) {
            throw new AppException(ErrorCode.CAMPAIGN_INVALID_STATUS);
        }
        if (r.getStatus() != FarmRegistrationStatus.PENDING) {
            throw new AppException(ErrorCode.CAMPAIGN_INVALID_STATUS);
        }
        r.setStatus(decision);
        r.setReviewNote(request != null ? request.getNote() : null);
        if (decision == FarmRegistrationStatus.APPROVED) {
            BigDecimal agreed = request != null ? request.getAgreedPricePerKg() : null;
            if (agreed != null && agreed.signum() <= 0) {
                throw new AppException(ErrorCode.CAMPAIGN_PRODUCT_INVALID);
            }
            // Giá trả nhà vườn không được vượt giá bán cho người mua, nếu không sàn lỗ trên từng kg
            if (agreed != null && agreed.compareTo(c.getRescuePricePerKg()) > 0) {
                throw new AppException(ErrorCode.CAMPAIGN_PRICE_ABOVE_RESCUE);
            }
            BigDecimal fallback = r.getProposedPricePerKg() == null ? c.getRescuePricePerKg()
                    : r.getProposedPricePerKg().min(c.getRescuePricePerKg());
            r.setAgreedPricePerKg(agreed != null ? agreed.setScale(2, RoundingMode.HALF_UP) : fallback);
        }
        r.setReviewedAt(LocalDateTime.now());
        r.setUpdatedAt(LocalDateTime.now());
        registrationRepository.save(r);
        notify(r.getShop(), decision == FarmRegistrationStatus.APPROVED ? "Đăng ký chiến dịch được duyệt" : "Đăng ký chiến dịch bị từ chối",
                "Lô " + plain(r.getSurplusVolumeKg()) + " kg của bạn trong chiến dịch \"" + c.getTitle() + "\" "
                        + (decision == FarmRegistrationStatus.APPROVED ? "đã được duyệt." : "bị từ chối" + noteSuffix(r.getReviewNote())));
        return toRegistrationResponse(r);
    }

    @Override
    @Transactional
    public CampaignResponse publishCampaign(int campaignId) {
        Campaign c = lockCampaign(campaignId);
        requireStatus(c, CampaignStatus.DRAFT);
        if (!c.getCutoffAt().isAfter(LocalDateTime.now())) {
            throw new AppException(ErrorCode.CAMPAIGN_CUTOFF_PASSED);
        }
        if (registrationRepository.countByCampaign_CampaignIdAndStatus(campaignId, FarmRegistrationStatus.APPROVED) == 0) {
            throw new AppException(ErrorCode.CAMPAIGN_NO_APPROVED_FARM);
        }
        LocalDateTime now = LocalDateTime.now();
        if (c.getCallPostedAt() == null) c.setCallPostedAt(now);
        c.setStatus(CampaignStatus.ACTIVE);
        c.setPublishedAt(now);
        c.setUpdatedAt(now);
        return toCampaignResponse(campaignRepository.save(c));
    }

    @Override
    @Transactional
    public CampaignResponse cancelCampaign(int campaignId, CampaignNoteRequest request) {
        Campaign c = lockCampaign(campaignId);
        if (c.getStatus() != CampaignStatus.DRAFT && c.getStatus() != CampaignStatus.ACTIVE
                && c.getStatus() != CampaignStatus.THRESHOLD_MET) {
            throw new AppException(ErrorCode.CAMPAIGN_INVALID_STATUS);
        }
        String reason = request != null && request.getNote() != null && !request.getNote().isBlank()
                ? request.getNote() : "Admin huỷ chiến dịch";
        cancelAndRefund(c, reason);
        return toCampaignResponse(c);
    }

    @Override
    @Transactional
    public CampaignResponse startFulfilling(int campaignId) {
        Campaign c = lockCampaign(campaignId);
        requireStatus(c, CampaignStatus.THRESHOLD_MET);
        c.setStatus(CampaignStatus.FULFILLING);
        c.setUpdatedAt(LocalDateTime.now());
        campaignRepository.save(c);
        for (CampaignFarmRegistration r : registrationRepository
                .findByCampaign_CampaignIdAndStatusOrderByCreatedAtAsc(campaignId, FarmRegistrationStatus.APPROVED)) {
            if (r.getAllocatedVolumeKg() != null && r.getAllocatedVolumeKg().signum() > 0) {
                notify(r.getShop(), "Mang hàng tới điểm tập kết",
                        "Chiến dịch \"" + c.getTitle() + "\" bắt đầu gom hàng. Vui lòng mang " + plain(r.getAllocatedVolumeKg())
                                + " kg tới " + nvl(c.getCollectionSiteName(), "điểm tập kết") + " ("
                                + nvl(c.getCollectionSiteAddress(), "") + ")"
                                + (c.getDeliveryDate() != null ? " ngày " + c.getDeliveryDate() : "") + ".");
            }
        }
        return toCampaignResponse(c);
    }

    @Override
    @Transactional
    public FarmRegistrationResponse recordIntake(int registrationId, CampaignIntakeRequest request) {
        CampaignFarmRegistration r = findRegistration(registrationId);
        Campaign c = r.getCampaign();
        requireStatus(c, CampaignStatus.FULFILLING);
        if (r.getStatus() != FarmRegistrationStatus.APPROVED) {
            throw new AppException(ErrorCode.CAMPAIGN_INVALID_STATUS);
        }
        if (r.getPaidOutAt() != null) {
            throw new AppException(ErrorCode.CAMPAIGN_INVALID_STATUS);
        }
        BigDecimal rejected = request == null || request.getRejectedWeightKg() == null ? BigDecimal.ZERO : request.getRejectedWeightKg();
        if (request == null || request.getReceivedWeightKg() == null || request.getReceivedWeightKg().signum() < 0
                || rejected.signum() < 0 || rejected.compareTo(request.getReceivedWeightKg()) > 0) {
            throw new AppException(ErrorCode.CAMPAIGN_INTAKE_INVALID);
        }
        // Vd: khai 100 kg, mang tới 100 kg nhưng 20 kg hư -> ghi nhận 80 kg đạt chuẩn vào chiến dịch
        boolean passed = request.getQualityPassed() == null || request.getQualityPassed();
        BigDecimal accepted = passed ? scale2(request.getReceivedWeightKg().subtract(rejected)) : BigDecimal.ZERO.setScale(2);
        r.setReceivedWeightKg(scale2(request.getReceivedWeightKg()));
        r.setRejectedWeightKg(passed ? scale2(rejected) : scale2(request.getReceivedWeightKg()));
        r.setAcceptedWeightKg(accepted);
        r.setQualityPassed(accepted.signum() > 0);
        // Sửa lại số cân sau khi đã đóng gói thì không được thấp hơn số kg đã đóng gói
        BigDecimal packed = nz(preOrderRepository.sumKgByStatuses(c.getCampaignId(), PACKED_STATUSES));
        if (packed.signum() > 0 && acceptedVolume(c.getCampaignId()).compareTo(packed) < 0) {
            throw new AppException(ErrorCode.CAMPAIGN_SHORTFALL);
        }
        if (request.getPhotoUrls() != null) {
            r.setIntakePhotoUrls(String.join(",", request.getPhotoUrls().stream()
                    .filter(Objects::nonNull).map(String::trim).filter(s -> !s.isEmpty()).toList()));
        }
        r.setIntakeNote(request.getNote());
        r.setIntakeAt(LocalDateTime.now());
        r.setUpdatedAt(LocalDateTime.now());
        return toRegistrationResponse(registrationRepository.save(r));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PreOrderResponse> getCampaignPreOrders(int campaignId) {
        findCampaign(campaignId);
        return preOrderRepository.findByCampaign_CampaignIdOrderByCreatedAtAsc(campaignId).stream()
                .map(this::toPreOrderResponse).toList();
    }

    @Override
    @Transactional
    public PreOrderResponse packPreOrder(int preOrderId, PreOrderPackRequest request) {
        PreOrder p = lockPreOrder(preOrderId);
        requireStatus(p.getCampaign(), CampaignStatus.FULFILLING);
        if (p.getStatus() != PreOrderStatus.DEPOSIT_PAID) {
            throw new AppException(ErrorCode.PRE_ORDER_INVALID_STATUS);
        }
        // Không đóng gói vượt số kg đạt chuẩn đã nhận tại điểm tập kết
        int campaignId = p.getCampaign().getCampaignId();
        BigDecimal packed = nz(preOrderRepository.sumKgByStatuses(campaignId, PACKED_STATUSES));
        if (packed.add(p.getTotalKg()).compareTo(acceptedVolume(campaignId)) > 0) {
            throw new AppException(ErrorCode.CAMPAIGN_SHORTFALL);
        }
        LocalDateTime now = LocalDateTime.now();
        p.setStatus(PreOrderStatus.PROCESSING);
        p.setQrCode("CAPNONG-" + p.getCode());
        p.setDeliveryZone(request != null ? request.getDeliveryZone() : null);
        p.setPackedAt(now);
        p.setUpdatedAt(now);
        return toPreOrderResponse(preOrderRepository.save(p));
    }

    @Override
    @Transactional
    public CampaignResponse dispatchToHub(int campaignId) {
        Campaign c = lockCampaign(campaignId);
        requireStatus(c, CampaignStatus.FULFILLING);
        LocalDateTime now = LocalDateTime.now();
        for (PreOrder p : preOrderRepository.findByCampaign_CampaignIdAndStatusIn(campaignId, List.of(PreOrderStatus.PROCESSING))) {
            p.setStatus(PreOrderStatus.READY);
            p.setReadyAt(now);
            p.setUpdatedAt(now);
            preOrderRepository.save(p);
            notify(p.getBuyer(), "Đơn đặt trước đã tới hub",
                    "Đơn " + p.getCode() + " (" + plain(p.getTotalKg()) + " kg) đã tới hub " + nvl(p.getDeliveryZone(), "")
                            + " và đang chờ shipper giao. Chuẩn bị " + plain(p.getRemainingAmount()) + "đ trả khi nhận hàng.");
        }
        return toCampaignResponse(c);
    }

    @Override
    @Transactional
    public CampaignResponse completeCampaign(int campaignId) {
        Campaign c = lockCampaign(campaignId);
        requireStatus(c, CampaignStatus.FULFILLING);
        if (preOrderRepository.countByCampaign_CampaignIdAndStatusIn(campaignId, IN_PROGRESS_STATUSES) > 0) {
            throw new AppException(ErrorCode.CAMPAIGN_NOT_ALL_DELIVERED);
        }
        LocalDateTime now = LocalDateTime.now();

        // 1. Giải ngân tiền cọc đang giữ (đơn đã giao, hoặc giao thất bại do người mua -> cọc không hoàn)
        for (PreOrder p : preOrderRepository.findByCampaign_CampaignIdAndStatusIn(campaignId,
                List.of(PreOrderStatus.DELIVERED, PreOrderStatus.FAILED))) {
            CampaignDeposit d = depositRepository.findByPreOrderIdForUpdate(p.getPreOrderId()).orElse(null);
            if (d != null && d.getStatus() == CampaignDepositStatus.HELD_IN_ESCROW) {
                escrowService.releaseDeposit(d);
                d.setStatus(CampaignDepositStatus.RELEASED_TO_FARMER);
                d.setReleasedAt(now);
                d.setUpdatedAt(now);
                depositRepository.save(d);
            }
        }

        // 2. Chi trả nhà vườn theo kg cân thực tế đạt chuẩn x giá chốt thương lượng (mặc định giá giải cứu), trừ phí dịch vụ nông dân
        BigDecimal keepRate = BigDecimal.ONE.subtract(farmerFeeRate);
        for (CampaignFarmRegistration r : registrationRepository
                .findByCampaign_CampaignIdAndStatusOrderByCreatedAtAsc(campaignId, FarmRegistrationStatus.APPROVED)) {
            if (r.getPaidOutAt() != null || !Boolean.TRUE.equals(r.getQualityPassed()) || r.getReceivedWeightKg() == null) {
                continue;
            }
            // Chỉ trả tiền cho kg đạt chuẩn (đã trừ hàng hư), tối đa bằng phần được phân bổ
            BigDecimal kg = countedKg(r);
            if (kg.signum() <= 0) continue;
            BigDecimal net = effectivePrice(r).multiply(kg).multiply(keepRate).setScale(0, RoundingMode.HALF_UP);
            escrowService.payoutFarm(r, net);
            r.setPayoutAmount(net);
            r.setPaidOutAt(now);
            r.setUpdatedAt(now);
            registrationRepository.save(r);
            notify(r.getShop(), "Đã nhận tiền chiến dịch",
                    "Chiến dịch \"" + c.getTitle() + "\" hoàn tất. Ví của bạn được cộng " + plain(net) + "đ cho "
                            + plain(kg) + " kg.");
        }

        c.setStatus(CampaignStatus.COMPLETED);
        c.setCompletedAt(now);
        c.setUpdatedAt(now);
        return toCampaignResponse(campaignRepository.save(c));
    }

    @Override
    @Transactional
    public CampaignResponse resolveShortfall(int campaignId) {
        Campaign c = lockCampaign(campaignId);
        requireStatus(c, CampaignStatus.FULFILLING);
        if (intakePending(campaignId) > 0) {
            throw new AppException(ErrorCode.CAMPAIGN_INTAKE_PENDING);
        }
        // Hàng còn lại cho các đơn chưa đóng gói = đạt chuẩn - đã đóng gói / giao
        BigDecimal available = acceptedVolume(campaignId)
                .subtract(nz(preOrderRepository.sumKgByStatuses(campaignId, PACKED_STATUSES)))
                .max(BigDecimal.ZERO);
        LocalDateTime now = LocalDateTime.now();
        for (PreOrder p : preOrderRepository.findByCampaign_CampaignIdAndStatusOrderByDepositPaidAtAscPreOrderIdAsc(
                campaignId, PreOrderStatus.DEPOSIT_PAID)) {
            p = lockPreOrder(p.getPreOrderId());
            BigDecimal crateKg = p.getCrateSize().kgDecimal();
            int canServe = available.divide(crateKg, 0, RoundingMode.DOWN).min(BigDecimal.valueOf(p.getCrateQuantity())).intValue();
            if (canServe >= p.getCrateQuantity()) {
                available = available.subtract(p.getTotalKg());
                continue;
            }
            CampaignDeposit d = depositRepository.findByPreOrderIdForUpdate(p.getPreOrderId()).orElse(null);
            if (canServe == 0) {
                // Không còn hàng cho đơn này: huỷ và hoàn toàn bộ cọc
                BigDecimal refund = d != null && d.getStatus() == CampaignDepositStatus.HELD_IN_ESCROW ? d.getAmount() : BigDecimal.ZERO;
                if (refund.signum() > 0) {
                    escrowService.refundDeposit(d);
                    d.setStatus(CampaignDepositStatus.REFUNDED);
                    d.setRefundedAt(now);
                    d.setUpdatedAt(now);
                    depositRepository.save(d);
                }
                p.setStatus(PreOrderStatus.CANCELLED);
                p.setCancelReason("Thiếu hàng tại điểm tập kết: nhà vườn giao không đủ kg đạt chuẩn");
                p.setCancelledAt(now);
                p.setShortfallRefundAmount(scale2(nz(p.getShortfallRefundAmount()).add(refund)));
                p.setUpdatedAt(now);
                preOrderRepository.save(p);
                notify(p.getBuyer(), "Đơn đặt trước bị huỷ do thiếu hàng",
                        "Nhà vườn giao thiếu nên chiến dịch \"" + c.getTitle() + "\" không đủ hàng cho đơn " + p.getCode()
                                + ". " + plain(refund) + "đ tiền cọc đã được hoàn vào ví CapNong của bạn. Xin lỗi bạn vì sự bất tiện này.");
                continue;
            }
            // Giao được một phần: giảm số thùng, tính lại tiền, hoàn phần cọc dư
            int oldQty = p.getCrateQuantity();
            BigDecimal newKg = crateKg.multiply(BigDecimal.valueOf(canServe));
            BigDecimal newTotal = p.getUnitPrice().multiply(newKg).setScale(0, RoundingMode.HALF_UP);
            BigDecimal newDeposit = newTotal.multiply(c.getDepositPercent()).setScale(0, RoundingMode.HALF_UP);
            BigDecimal refund = p.getDepositAmount().subtract(newDeposit).max(BigDecimal.ZERO);
            if (d != null && d.getStatus() == CampaignDepositStatus.HELD_IN_ESCROW && refund.signum() > 0) {
                escrowService.refundDepositPart(d, refund);
                d.setAmount(newDeposit);
                d.setUpdatedAt(now);
                depositRepository.save(d);
            }
            if (p.getOriginalCrateQuantity() == null) p.setOriginalCrateQuantity(oldQty);
            p.setCrateQuantity(canServe);
            p.setTotalKg(scale2(newKg));
            p.setTotalAmount(newTotal);
            p.setDepositAmount(newDeposit);
            p.setRemainingAmount(newTotal.subtract(newDeposit));
            p.setShortfallRefundAmount(scale2(nz(p.getShortfallRefundAmount()).add(refund)));
            p.setUpdatedAt(now);
            preOrderRepository.save(p);
            available = available.subtract(newKg);
            notify(p.getBuyer(), "Đơn đặt trước được giảm số thùng do thiếu hàng",
                    "Nhà vườn giao thiếu nên đơn " + p.getCode() + " còn " + canServe + "/" + oldQty + " thùng ("
                            + plain(newKg) + " kg). " + plain(refund) + "đ tiền cọc dư đã được hoàn vào ví CapNong. Khi nhận hàng bạn trả "
                            + plain(p.getRemainingAmount()) + "đ.");
        }
        c.setUpdatedAt(now);
        return toCampaignResponse(campaignRepository.save(c));
    }

    // ═════════════════════════ CÔNG KHAI / NGƯỜI MUA ═════════════════════════

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CampaignResponse> getActiveCampaigns(int page, int size) {
        Page<Campaign> result = campaignRepository.findByStatusInOrderByCreatedAtDesc(
                List.of(CampaignStatus.ACTIVE), PageRequest.of(Math.max(page, 0), clampSize(size)));
        return PageResponse.from(result, result.getContent().stream().map(this::toCampaignResponse).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CampaignResponse> getPublicCampaigns(String phase, int page, int size) {
        PageRequest pr = PageRequest.of(Math.max(page, 0), clampSize(size));
        String p = phase == null ? "ONGOING" : phase.trim().toUpperCase(Locale.ROOT);
        Page<Campaign> result = switch (p) {
            case "UPCOMING" -> campaignRepository.findByStatusAndCallPostedAtIsNotNullOrderByCreatedAtDesc(CampaignStatus.DRAFT, pr);
            case "ONGOING" -> campaignRepository.findByStatusInOrderByCreatedAtDesc(List.of(CampaignStatus.ACTIVE), pr);
            case "DELIVERING" -> campaignRepository.findByStatusInOrderByCreatedAtDesc(
                    List.of(CampaignStatus.THRESHOLD_MET, CampaignStatus.FULFILLING), pr);
            case "DONE" -> campaignRepository.findByStatusInOrderByCreatedAtDesc(List.of(CampaignStatus.COMPLETED), pr);
            default -> throw new AppException(ErrorCode.INVALID_KEY);
        };
        return PageResponse.from(result, result.getContent().stream().map(this::toCampaignResponse).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CampaignResponse getPublicCampaign(int campaignId) {
        Campaign c = findCampaign(campaignId);
        if (c.getStatus() == CampaignStatus.DRAFT && c.getCallPostedAt() == null) {
            throw new AppException(ErrorCode.CAMPAIGN_NOT_FOUND);
        }
        return toCampaignResponse(c);
    }

    @Override
    @Transactional
    public PreOrderResponse createPreOrder(int campaignId, PreOrderCreateRequest request) {
        User buyer = currentUser();
        if (request == null || request.getCrateSize() == null || request.getCrateQuantity() == null
                || request.getCrateQuantity() < 1 || isBlank(request.getShippingAddress())
                || isBlank(request.getRecipientName()) || isBlank(request.getRecipientPhone())) {
            throw new AppException(ErrorCode.PRE_ORDER_INVALID);
        }
        Campaign c = lockCampaign(campaignId);
        requireStatus(c, CampaignStatus.ACTIVE);
        LocalDateTime now = LocalDateTime.now();
        if (!c.getCutoffAt().isAfter(now)) {
            throw new AppException(ErrorCode.CAMPAIGN_CUTOFF_PASSED);
        }

        BigDecimal totalKg = request.getCrateSize().kgDecimal().multiply(BigDecimal.valueOf(request.getCrateQuantity()));
        if (totalKg.compareTo(remainingVolumeForBooking(c)) > 0) {
            throw new AppException(ErrorCode.CAMPAIGN_VOLUME_EXCEEDED);
        }

        BigDecimal total = c.getRescuePricePerKg().multiply(totalKg).setScale(0, RoundingMode.HALF_UP);
        BigDecimal deposit = total.multiply(c.getDepositPercent()).setScale(0, RoundingMode.HALF_UP);
        PreOrder p = preOrderRepository.save(PreOrder.builder()
                .code(nextCode("PO"))
                .campaign(c)
                .buyer(buyer)
                .crateSize(request.getCrateSize())
                .crateQuantity(request.getCrateQuantity())
                .totalKg(scale2(totalKg))
                .unitPrice(c.getRescuePricePerKg())
                .totalAmount(total)
                .depositAmount(deposit)
                .remainingAmount(total.subtract(deposit))
                .recipientName(request.getRecipientName().trim())
                .recipientPhone(request.getRecipientPhone().trim())
                .shippingAddress(request.getShippingAddress().trim())
                .shippingLatitude(request.getShippingLatitude())
                .shippingLongitude(request.getShippingLongitude())
                .note(request.getNote())
                .status(PreOrderStatus.PENDING)
                .createdAt(now)
                .updatedAt(now)
                .build());

        long orderCode = nextPayosOrderCode();
        CampaignDeposit d = CampaignDeposit.builder()
                .preOrder(p)
                .amount(deposit)
                .payosOrderCode(String.valueOf(orderCode))
                .status(CampaignDepositStatus.PENDING)
                .createdAt(now)
                .updatedAt(now)
                .build();
        try {
            CreatePaymentLinkRequest.CreatePaymentLinkRequestBuilder b = CreatePaymentLinkRequest.builder()
                    .orderCode(orderCode)
                    .amount(deposit.longValueExact())
                    .description(truncate("COC " + p.getCode(), 25))
                    .returnUrl(withQuery(defaultReturnUrl, p.getPreOrderId()))
                    .cancelUrl(withQuery(defaultCancelUrl, p.getPreOrderId()));
            if (preOrderExpireMinutes > 0) {
                b.expiredAt(java.time.Instant.now().plusSeconds(preOrderExpireMinutes * 60L).getEpochSecond());
            }
            CreatePaymentLinkResponse res = payOS.paymentRequests().create(b.build());
            d.setCheckoutUrl(res.getCheckoutUrl());
            d.setQrCodeUrl(res.getQrCode());
        } catch (Exception e) {
            log.error("[Campaign] Cannot create PayOS deposit link for pre-order {}: {}", p.getCode(), e.getMessage(), e);
            throw new AppException(ErrorCode.PAYMENT_FAILED);
        }
        depositRepository.save(d);
        p.setDeposit(d);
        return toPreOrderResponse(p);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PreOrderResponse> getMyPreOrders(int page, int size) {
        Page<PreOrder> result = preOrderRepository.findByBuyer_IdOrderByCreatedAtDesc(currentUser().getId(),
                PageRequest.of(Math.max(page, 0), clampSize(size)));
        return PageResponse.from(result, result.getContent().stream().map(this::toPreOrderResponse).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PreOrderResponse getPreOrder(int preOrderId) {
        PreOrder p = preOrderRepository.findById(preOrderId)
                .orElseThrow(() -> new AppException(ErrorCode.PRE_ORDER_NOT_FOUND));
        User me = currentUser();
        boolean allowed = isAdmin(me) || p.getBuyer().getId() == me.getId()
                || (p.getShipper() != null && p.getShipper().getId() == me.getId())
                || (isRole(me, "SHIPPER") && p.getStatus() == PreOrderStatus.READY && p.getShipper() == null);
        if (!allowed) {
            throw new AppException(ErrorCode.PRE_ORDER_NOT_OWNER);
        }
        return toPreOrderResponse(p);
    }

    @Override
    @Transactional
    public PreOrderResponse confirmDepositPayment(int preOrderId) {
        PreOrder p = preOrderRepository.findById(preOrderId)
                .orElseThrow(() -> new AppException(ErrorCode.PRE_ORDER_NOT_FOUND));
        if (p.getBuyer().getId() != currentUser().getId()) {
            throw new AppException(ErrorCode.PRE_ORDER_NOT_OWNER);
        }
        CampaignDeposit d = depositRepository.findByPreOrder_PreOrderId(preOrderId).orElse(null);
        if (d != null && d.getStatus() == CampaignDepositStatus.PENDING && d.getPayosOrderCode() != null) {
            try {
                PaymentLink link = payOS.paymentRequests().get(Long.parseLong(d.getPayosOrderCode()));
                if (link != null && PaymentLinkStatus.PAID.equals(link.getStatus())) {
                    settleDeposit(d.getPayosOrderCode());
                }
            } catch (Exception e) {
                log.warn("[Campaign] Cannot check PayOS status for {}: {}", d.getPayosOrderCode(), e.getMessage());
            }
        }
        return toPreOrderResponse(preOrderRepository.findById(preOrderId).orElseThrow());
    }

    @Override
    @Transactional
    public PreOrderResponse cancelPreOrder(int preOrderId) {
        PreOrder p = lockPreOrder(preOrderId);
        if (p.getBuyer().getId() != currentUser().getId()) {
            throw new AppException(ErrorCode.PRE_ORDER_NOT_OWNER);
        }
        if (p.getStatus() != PreOrderStatus.PENDING) {
            // Đã cọc thì không tự huỷ được: cọc chỉ hoàn khi chiến dịch bị huỷ
            throw new AppException(ErrorCode.PRE_ORDER_INVALID_STATUS);
        }
        cancelUnpaidPreOrder(p, "Người mua huỷ trước khi cọc");
        return toPreOrderResponse(p);
    }

    // ═════════════════════════════ NHÀ VƯỜN ═════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public List<CampaignResponse> getOpenFarmCalls() {
        LocalDateTime now = LocalDateTime.now();
        return campaignRepository.findOpenCalls().stream()
                .filter(c -> c.getCutoffAt().isAfter(now))
                .map(this::toCampaignResponse).toList();
    }

    @Override
    @Transactional
    public FarmRegistrationResponse registerFarm(int campaignId, FarmRegistrationRequest request) {
        User shop = currentUser();
        if (request == null || request.getSurplusVolumeKg() == null || request.getSurplusVolumeKg().signum() <= 0
                || request.getHarvestDate() == null || request.getHarvestDate().isBefore(LocalDate.now())) {
            throw new AppException(ErrorCode.CAMPAIGN_REGISTRATION_INVALID);
        }
        Campaign c = lockCampaign(campaignId);
        boolean open = (c.getStatus() == CampaignStatus.DRAFT && c.getCallPostedAt() != null)
                || c.getStatus() == CampaignStatus.ACTIVE;
        if (!open) {
            throw new AppException(ErrorCode.CAMPAIGN_CALL_NOT_OPEN);
        }
        if (!c.getCutoffAt().isAfter(LocalDateTime.now())) {
            throw new AppException(ErrorCode.CAMPAIGN_CUTOFF_PASSED);
        }
        if (registrationRepository.findByCampaign_CampaignIdAndShop_Id(campaignId, shop.getId()).isPresent()) {
            throw new AppException(ErrorCode.CAMPAIGN_ALREADY_REGISTERED);
        }
        // Thông tin sản phẩm: tên, ảnh minh hoạ, giá đề xuất, cam kết, thời gian bảo quản
        Product product = null;
        if (request.getProductId() != null) {
            product = productRepository.findById(request.getProductId())
                    .orElseThrow(() -> new AppException(ErrorCode.CAMPAIGN_PRODUCT_NOT_OWNED));
            if (product.getShop() == null || product.getShop().getId() != shop.getId()) {
                throw new AppException(ErrorCode.CAMPAIGN_PRODUCT_NOT_OWNED);
            }
        }
        String productName = !isBlank(request.getProductName()) ? request.getProductName().trim()
                : product != null ? product.getProductName() : null;
        List<String> images = request.getProductImageUrls() == null ? List.of()
                : request.getProductImageUrls().stream().filter(u -> u != null && !u.isBlank()).map(String::trim).toList();
        if (isBlank(productName) || productName.length() > 200 || images.isEmpty() || images.size() > 5
                || request.getProposedPricePerKg() == null || request.getProposedPricePerKg().signum() <= 0
                || request.getShelfLifeTier() == null || !Boolean.TRUE.equals(request.getPriceCommitted())) {
            throw new AppException(ErrorCode.CAMPAIGN_PRODUCT_INVALID);
        }
        // Hàng phải còn tươi tới ngày giao: thu hoạch không sau ngày giao và (ngày giao - ngày thu hoạch) <= số ngày bảo quản
        if (c.getDeliveryDate() != null) {
            long days = java.time.temporal.ChronoUnit.DAYS.between(request.getHarvestDate(), c.getDeliveryDate());
            if (days < 0 || days > request.getShelfLifeTier().getMaxDays()) {
                throw new AppException(ErrorCode.CAMPAIGN_SHELF_LIFE_TOO_SHORT);
            }
        }
        LocalDateTime now = LocalDateTime.now();
        CampaignFarmRegistration r = registrationRepository.save(CampaignFarmRegistration.builder()
                .campaign(c)
                .shop(shop)
                .surplusVolumeKg(scale2(request.getSurplusVolumeKg()))
                .harvestDate(request.getHarvestDate())
                .note(request.getNote())
                .product(product)
                .productName(productName)
                .productImageUrls(String.join(",", images))
                .proposedPricePerKg(request.getProposedPricePerKg().setScale(2, RoundingMode.HALF_UP))
                .priceCommitted(true)
                .shelfLifeTier(request.getShelfLifeTier())
                .status(FarmRegistrationStatus.PENDING)
                .createdAt(now)
                .updatedAt(now)
                .build());
        return toRegistrationResponse(r);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FarmRegistrationResponse> getMyRegistrations() {
        return registrationRepository.findByShop_IdOrderByCreatedAtDesc(currentUser().getId()).stream()
                .map(this::toRegistrationResponse).toList();
    }

    // ═════════════════════════════ SHIPPER ═════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public List<PreOrderResponse> getReadyForPickup() {
        return preOrderRepository.findByStatusAndShipperIsNullOrderByReadyAtAsc(PreOrderStatus.READY).stream()
                .map(this::toPreOrderResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PreOrderResponse> getMyShipperPreOrders() {
        return preOrderRepository.findByShipper_IdOrderByUpdatedAtDesc(currentUser().getId()).stream()
                .map(this::toPreOrderResponse).toList();
    }

    @Override
    @Transactional
    public PreOrderResponse acceptPreOrder(int preOrderId) {
        User shipper = currentUser();
        PreOrder p = lockPreOrder(preOrderId);
        if (p.getStatus() != PreOrderStatus.READY || p.getShipper() != null) {
            throw new AppException(ErrorCode.PRE_ORDER_INVALID_STATUS);
        }
        LocalDateTime now = LocalDateTime.now();
        p.setShipper(shipper);
        p.setPickedUpAt(now);
        p.setUpdatedAt(now);
        preOrderRepository.save(p);
        notify(p.getBuyer(), "Shipper đang giao đơn đặt trước",
                "Đơn " + p.getCode() + " đã được shipper " + nvl(shipper.getFullName(), "") + " nhận tại hub và đang giao tới bạn.");
        return toPreOrderResponse(p);
    }

    @Override
    @Transactional
    public PreOrderResponse deliverPreOrder(int preOrderId, PreOrderDeliveryRequest request) {
        PreOrder p = lockAssignedPreOrder(preOrderId);
        LocalDateTime now = LocalDateTime.now();
        p.setStatus(PreOrderStatus.DELIVERED);
        p.setPodImageUrl(request != null ? request.getPodImageUrl() : null);
        p.setDeliveredAt(now);
        p.setUpdatedAt(now);
        preOrderRepository.save(p);
        escrowService.recordRemainingCollected(p);
        notify(p.getBuyer(), "Đã giao đơn đặt trước", "Đơn " + p.getCode() + " đã giao thành công. Cảm ơn bạn đã cùng giải cứu nông sản!");
        return toPreOrderResponse(p);
    }

    @Override
    @Transactional
    public PreOrderResponse failPreOrder(int preOrderId, PreOrderDeliveryRequest request) {
        PreOrder p = lockAssignedPreOrder(preOrderId);
        LocalDateTime now = LocalDateTime.now();
        p.setStatus(PreOrderStatus.FAILED);
        p.setFailReason(request != null ? request.getReason() : null);
        p.setPodImageUrl(request != null ? request.getPodImageUrl() : null);
        p.setUpdatedAt(now);
        preOrderRepository.save(p);
        notify(p.getBuyer(), "Giao đơn đặt trước thất bại",
                "Shipper không giao được đơn " + p.getCode() + noteSuffix(p.getFailReason()) + " Hàng đã quay về hub.");
        return toPreOrderResponse(p);
    }

    // ═════════════════════════════ NỀN ═════════════════════════════

    @Override
    @Transactional
    public boolean handlePaidOrderCode(String orderCode) {
        if (orderCode == null || !depositRepository.existsByPayosOrderCode(orderCode)) {
            return false;
        }
        settleDeposit(orderCode);
        return true;
    }

    /** Cọc đã trả: giữ tiền vào escrow, PreOrder DEPOSIT_PAID, cộng kg đã đặt. Idempotent nhờ khoá dòng + kiểm tra trạng thái. */
    private void settleDeposit(String orderCode) {
        CampaignDeposit found = depositRepository.findByPayosOrderCode(orderCode).orElse(null);
        if (found == null) return;
        int preOrderId = found.getPreOrder().getPreOrderId();
        // Thứ tự khoá giống evaluateCampaign / cancelCampaign (chiến dịch -> đơn -> cọc) để tránh deadlock
        Campaign c = lockCampaign(found.getPreOrder().getCampaign().getCampaignId());
        PreOrder p = lockPreOrder(preOrderId);
        CampaignDeposit d = depositRepository.findByPreOrderIdForUpdate(preOrderId).orElseThrow();
        if (d.getStatus() != CampaignDepositStatus.PENDING && d.getStatus() != CampaignDepositStatus.CANCELLED) {
            log.info("[Campaign] Deposit {} already {}, skip", orderCode, d.getStatus());
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        escrowService.holdDeposit(d);
        d.setHeldAt(now);
        d.setUpdatedAt(now);

        boolean stillValid = p.getStatus() == PreOrderStatus.PENDING && c.getStatus() == CampaignStatus.ACTIVE;
        if (!stillValid) {
            // Người mua trả sau khi đơn đã huỷ / chiến dịch đã đóng: hoàn ngay vào ví
            escrowService.refundDeposit(d);
            d.setStatus(CampaignDepositStatus.REFUNDED);
            d.setRefundedAt(now);
            depositRepository.save(d);
            notify(p.getBuyer(), "Hoàn tiền cọc", "Đơn " + p.getCode() + " không còn hiệu lực nên " + plain(d.getAmount())
                    + "đ tiền cọc đã được hoàn vào ví CapNong của bạn.");
            log.warn("[Campaign] Late deposit {} for pre-order {} refunded to wallet", orderCode, p.getCode());
            return;
        }

        d.setStatus(CampaignDepositStatus.HELD_IN_ESCROW);
        depositRepository.save(d);
        p.setStatus(PreOrderStatus.DEPOSIT_PAID);
        p.setDepositPaidAt(now);
        p.setUpdatedAt(now);
        preOrderRepository.save(p);
        c.setBookedVolumeKg(scale2(nz(c.getBookedVolumeKg()).add(p.getTotalKg())));
        c.setUpdatedAt(now);
        campaignRepository.save(c);
        notify(p.getBuyer(), "Đặt cọc thành công", "Đơn " + p.getCode() + " đã cọc " + plain(d.getAmount())
                + "đ. Chiến dịch \"" + c.getTitle() + "\" chốt lúc " + c.getCutoffAt() + "; nếu không đủ ngưỡng bạn sẽ được hoàn cọc.");
    }

    @Override
    @Transactional
    public void evaluateCampaign(int campaignId) {
        Campaign c = lockCampaign(campaignId);
        if (c.getStatus() != CampaignStatus.ACTIVE || c.getCutoffAt().isAfter(LocalDateTime.now())) {
            return;
        }
        // Đơn chưa cọc tới lúc chốt thì huỷ luôn
        for (PreOrder p : preOrderRepository.findByCampaign_CampaignIdAndStatusIn(campaignId, List.of(PreOrderStatus.PENDING))) {
            cancelUnpaidPreOrder(p, "Hết hạn chốt chiến dịch khi chưa cọc");
        }
        LocalDateTime now = LocalDateTime.now();
        c.setEvaluatedAt(now);
        if (nz(c.getBookedVolumeKg()).compareTo(c.getMinThresholdKg()) < 0) {
            log.info("[Campaign] {} unmet threshold: booked {} < min {}", c.getCode(), c.getBookedVolumeKg(), c.getMinThresholdKg());
            cancelAndRefund(c, "Không đạt ngưỡng " + plain(c.getMinThresholdKg()) + " kg lúc chốt (đã đặt "
                    + plain(c.getBookedVolumeKg()) + " kg)");
            return;
        }
        allocateToFarms(c);
        c.setStatus(CampaignStatus.THRESHOLD_MET);
        c.setUpdatedAt(now);
        campaignRepository.save(c);
        for (PreOrder p : preOrderRepository.findByCampaign_CampaignIdAndStatusIn(campaignId, List.of(PreOrderStatus.DEPOSIT_PAID))) {
            notify(p.getBuyer(), "Chiến dịch đủ ngưỡng", "Chiến dịch \"" + c.getTitle() + "\" đã đủ sản lượng. Đơn "
                    + p.getCode() + " sẽ được gom và giao tới bạn.");
        }
        log.info("[Campaign] {} THRESHOLD_MET with {} kg", c.getCode(), c.getBookedVolumeKg());
    }

    @Override
    @Transactional
    public void expirePendingPreOrder(int preOrderId) {
        PreOrder p = lockPreOrder(preOrderId);
        if (p.getStatus() != PreOrderStatus.PENDING) return;
        if (p.getCreatedAt().isAfter(LocalDateTime.now().minusMinutes(Math.max(preOrderExpireMinutes, 1)))) return;
        cancelUnpaidPreOrder(p, "Quá hạn thanh toán cọc");
    }

    // ═════════════════════════════ HELPERS ═════════════════════════════

    /** Chia số kg đã đặt cho các nhà vườn được duyệt theo tỷ lệ kg dư đã khai, không vượt kg dư */
    private void allocateToFarms(Campaign c) {
        List<CampaignFarmRegistration> regs = registrationRepository
                .findByCampaign_CampaignIdAndStatusOrderByCreatedAtAsc(c.getCampaignId(), FarmRegistrationStatus.APPROVED);
        BigDecimal totalSurplus = regs.stream().map(CampaignFarmRegistration::getSurplusVolumeKg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalSurplus.signum() <= 0) return;
        BigDecimal toAllocate = nz(c.getBookedVolumeKg()).min(totalSurplus);
        BigDecimal allocated = BigDecimal.ZERO;
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < regs.size(); i++) {
            CampaignFarmRegistration r = regs.get(i);
            BigDecimal share = i == regs.size() - 1
                    ? toAllocate.subtract(allocated)
                    : toAllocate.multiply(r.getSurplusVolumeKg()).divide(totalSurplus, 2, RoundingMode.DOWN);
            share = scale2(share.max(BigDecimal.ZERO).min(r.getSurplusVolumeKg()));
            allocated = allocated.add(share);
            r.setAllocatedVolumeKg(share);
            r.setUpdatedAt(now);
            registrationRepository.save(r);
            notify(r.getShop(), "Chiến dịch đủ ngưỡng - sản lượng của bạn",
                    "Chiến dịch \"" + c.getTitle() + "\" đã đủ ngưỡng. Bạn được phân bổ " + plain(share)
                            + " kg. CapNong sẽ báo lịch mang hàng tới điểm tập kết.");
        }
    }

    /** Huỷ chiến dịch: huỷ đơn, hoàn mọi khoản cọc đang giữ vào ví người mua, báo cho các bên */
    private void cancelAndRefund(Campaign c, String reason) {
        LocalDateTime now = LocalDateTime.now();
        for (PreOrder p : preOrderRepository.findByCampaign_CampaignIdOrderByCreatedAtAsc(c.getCampaignId())) {
            if (p.getStatus() == PreOrderStatus.PENDING) {
                cancelUnpaidPreOrder(p, reason);
                continue;
            }
            if (p.getStatus() == PreOrderStatus.CANCELLED) continue;
            CampaignDeposit d = depositRepository.findByPreOrderIdForUpdate(p.getPreOrderId()).orElse(null);
            if (d != null && d.getStatus() == CampaignDepositStatus.HELD_IN_ESCROW) {
                escrowService.refundDeposit(d);
                d.setStatus(CampaignDepositStatus.REFUNDED);
                d.setRefundedAt(now);
                d.setUpdatedAt(now);
                depositRepository.save(d);
            }
            p.setStatus(PreOrderStatus.CANCELLED);
            p.setCancelReason(reason);
            p.setCancelledAt(now);
            p.setUpdatedAt(now);
            preOrderRepository.save(p);
            notify(p.getBuyer(), "Chiến dịch bị huỷ - đã hoàn cọc", "Chiến dịch \"" + c.getTitle() + "\" bị huỷ (" + reason
                    + "). Tiền cọc " + plain(p.getDepositAmount()) + "đ của đơn " + p.getCode() + " đã được hoàn vào ví CapNong.");
        }
        for (CampaignFarmRegistration r : registrationRepository.findByCampaign_CampaignIdOrderByCreatedAtAsc(c.getCampaignId())) {
            if (r.getStatus() == FarmRegistrationStatus.APPROVED) {
                notify(r.getShop(), "Chiến dịch bị huỷ", "Chiến dịch \"" + c.getTitle() + "\" bị huỷ: " + reason + ".");
            }
        }
        c.setStatus(CampaignStatus.CANCELLED);
        c.setCancelReason(reason);
        c.setCancelledAt(now);
        c.setUpdatedAt(now);
        campaignRepository.save(c);
    }

    private void cancelUnpaidPreOrder(PreOrder p, String reason) {
        LocalDateTime now = LocalDateTime.now();
        CampaignDeposit d = depositRepository.findByPreOrderIdForUpdate(p.getPreOrderId()).orElse(null);
        if (d != null && d.getStatus() == CampaignDepositStatus.PENDING) {
            d.setStatus(CampaignDepositStatus.CANCELLED);
            d.setUpdatedAt(now);
            depositRepository.save(d);
            try {
                payOS.paymentRequests().cancel(Long.parseLong(d.getPayosOrderCode()), truncate(reason, 100));
            } catch (Exception e) {
                // Link có thể đã hết hạn; nếu khách vẫn trả, settleDeposit sẽ hoàn vào ví
                log.warn("[Campaign] Cancel PayOS link {} failed: {}", d.getPayosOrderCode(), e.getMessage());
            }
        }
        p.setStatus(PreOrderStatus.CANCELLED);
        p.setCancelReason(reason);
        p.setCancelledAt(now);
        p.setUpdatedAt(now);
        preOrderRepository.save(p);
    }

    /** Kg còn nhận đặt = min(mục tiêu, tổng kg dư được duyệt) - đã cọc - đang chờ cọc */
    private BigDecimal remainingVolume(Campaign c) {
        BigDecimal cap = c.getTargetVolumeKg().min(nz(registrationRepository.sumApprovedSurplus(c.getCampaignId())));
        BigDecimal used = nz(c.getBookedVolumeKg()).add(nz(preOrderRepository.sumPendingKg(c.getCampaignId())));
        return cap.subtract(used).max(BigDecimal.ZERO);
    }

    /**
     * Giống remainingVolume nhưng đọc bằng locking read. MySQL REPEATABLE READ: SELECT thường đọc snapshot tạo từ lần đọc
     * đầu của giao dịch, nên dù đã khoá dòng chiến dịch vẫn có thể không thấy đơn PENDING người khác vừa commit -> bán vượt.
     */
    private BigDecimal remainingVolumeForBooking(Campaign c) {
        BigDecimal cap = c.getTargetVolumeKg().min(nz(registrationRepository.sumApprovedSurplusForUpdate(c.getCampaignId())));
        BigDecimal used = nz(c.getBookedVolumeKg()).add(nz(preOrderRepository.sumPendingKgForUpdate(c.getCampaignId())));
        return cap.subtract(used).max(BigDecimal.ZERO);
    }

    /** Kg đạt chuẩn của một nhà vườn được tính vào chiến dịch: tối đa bằng phần phân bổ (phần dư không mua) */
    private static BigDecimal countedKg(CampaignFarmRegistration r) {
        if (!Boolean.TRUE.equals(r.getQualityPassed())) return BigDecimal.ZERO;
        BigDecimal kg = r.getAcceptedWeightKg() != null ? r.getAcceptedWeightKg() : nz(r.getReceivedWeightKg());
        if (r.getAllocatedVolumeKg() != null) kg = kg.min(r.getAllocatedVolumeKg());
        return kg.max(BigDecimal.ZERO);
    }

    private BigDecimal acceptedVolume(int campaignId) {
        return registrationRepository.findByCampaign_CampaignIdAndStatusOrderByCreatedAtAsc(campaignId, FarmRegistrationStatus.APPROVED)
                .stream().map(CampaignServiceImpl::countedKg).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private long intakePending(int campaignId) {
        return registrationRepository.findByCampaign_CampaignIdAndStatusOrderByCreatedAtAsc(campaignId, FarmRegistrationStatus.APPROVED)
                .stream().filter(r -> r.getAllocatedVolumeKg() != null && r.getAllocatedVolumeKg().signum() > 0 && r.getIntakeAt() == null)
                .count();
    }

    private void validateUpsert(CampaignUpsertRequest r) {
        if (r == null || isBlank(r.getTitle()) || r.getTargetVolumeKg() == null || r.getTargetVolumeKg().signum() <= 0
                || r.getMinThresholdKg() == null || r.getMinThresholdKg().signum() <= 0
                || r.getMinThresholdKg().compareTo(r.getTargetVolumeKg()) > 0
                || r.getRescuePricePerKg() == null || r.getRescuePricePerKg().signum() <= 0
                || r.getDepositPercent() == null || r.getDepositPercent().signum() <= 0
                || r.getDepositPercent().compareTo(BigDecimal.ONE) > 0
                || r.getCutoffAt() == null || !r.getCutoffAt().isAfter(LocalDateTime.now())
                || (r.getDeliveryDate() != null && r.getDeliveryDate().isBefore(r.getCutoffAt().toLocalDate()))) {
            throw new AppException(ErrorCode.CAMPAIGN_INVALID_DATA);
        }
    }

    private void applyUpsert(Campaign c, CampaignUpsertRequest r) {
        c.setTitle(r.getTitle().trim());
        c.setDescription(r.getDescription());
        c.setCategory(r.getCategoryId() == null ? null : categoryRepository.findById(r.getCategoryId())
                .orElseThrow(() -> new AppException(ErrorCode.CAMPAIGN_INVALID_DATA)));
        c.setSeason(r.getSeason());
        c.setCoverImageUrl(r.getCoverImageUrl());
        c.setTargetVolumeKg(scale2(r.getTargetVolumeKg()));
        c.setMinThresholdKg(scale2(r.getMinThresholdKg()));
        c.setRescuePricePerKg(r.getRescuePricePerKg().setScale(2, RoundingMode.HALF_UP));
        c.setDepositPercent(r.getDepositPercent().setScale(4, RoundingMode.HALF_UP));
        c.setCutoffAt(r.getCutoffAt());
        c.setDeliveryDate(r.getDeliveryDate());
        c.setCollectionSiteName(r.getCollectionSiteName());
        c.setCollectionSiteAddress(r.getCollectionSiteAddress());
        c.setCollectionSiteLatitude(r.getCollectionSiteLatitude());
        c.setCollectionSiteLongitude(r.getCollectionSiteLongitude());
    }

    private Campaign findCampaign(int id) {
        return campaignRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.CAMPAIGN_NOT_FOUND));
    }

    private Campaign lockCampaign(int id) {
        return campaignRepository.findByIdForUpdate(id).orElseThrow(() -> new AppException(ErrorCode.CAMPAIGN_NOT_FOUND));
    }

    private PreOrder lockPreOrder(int id) {
        return preOrderRepository.findByIdForUpdate(id).orElseThrow(() -> new AppException(ErrorCode.PRE_ORDER_NOT_FOUND));
    }

    private PreOrder lockAssignedPreOrder(int id) {
        PreOrder p = lockPreOrder(id);
        if (p.getShipper() == null || p.getShipper().getId() != currentUser().getId()) {
            throw new AppException(ErrorCode.PRE_ORDER_NOT_OWNER);
        }
        if (p.getStatus() != PreOrderStatus.READY) {
            throw new AppException(ErrorCode.PRE_ORDER_INVALID_STATUS);
        }
        return p;
    }

    private CampaignFarmRegistration findRegistration(int id) {
        return registrationRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.CAMPAIGN_REGISTRATION_NOT_FOUND));
    }

    private static void requireStatus(Campaign c, CampaignStatus expected) {
        if (c.getStatus() != expected) {
            throw new AppException(ErrorCode.CAMPAIGN_INVALID_STATUS);
        }
    }

    private static CampaignStatus parseStatus(String s) {
        try {
            return CampaignStatus.valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.CAMPAIGN_INVALID_STATUS);
        }
    }

    private String nextCode(String prefix) {
        for (int i = 0; i < 5; i++) {
            // 4 chữ số ngẫu nhiên + kiểm tra trùng cho cả mã chiến dịch và mã đơn (trước đây mã PO không kiểm tra -> trùng UNIQUE khi 2 đơn cùng giây)
            String code = prefix + LocalDateTime.now().format(CODE_TIME) + ThreadLocalRandom.current().nextInt(1000, 10000);
            boolean taken = "CD".equals(prefix) ? campaignRepository.existsByCode(code) : preOrderRepository.existsByCode(code);
            if (!taken) return code;
        }
        return prefix + System.nanoTime();
    }

    /** Cùng cách sinh mã với PaymentServiceImpl, kiểm tra trùng ở cả payments và campaign_deposits */
    private long nextPayosOrderCode() {
        for (int i = 0; i < 5; i++) {
            long code = System.currentTimeMillis() * 1000L + ThreadLocalRandom.current().nextInt(1000);
            String c = String.valueOf(code);
            if (!depositRepository.existsByPayosOrderCode(c) && paymentRepository.findByPayosOrderCode(c).isEmpty()) {
                return code;
            }
        }
        throw new AppException(ErrorCode.PAYMENT_FAILED);
    }

    private User currentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null || !auth.getName().matches("\\d+")) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        return userRepository.findById(Integer.parseInt(auth.getName()))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    private static boolean isRole(User u, String role) {
        return u.getRole() != null && role.equalsIgnoreCase(u.getRole().getName());
    }

    private static boolean isAdmin(User u) {
        return isRole(u, "ADMIN") || isRole(u, "STAFF");
    }

    /** Thông báo không được làm hỏng nghiệp vụ chính */
    private void notify(User user, String title, String message) {
        if (user == null) return;
        try {
            notificationService.sendNotificationToUser(user, title, message, null);
        } catch (Exception e) {
            log.warn("[Campaign] Notify user #{} failed: {}", user.getId(), e.getMessage());
        }
    }

    private static String withQuery(String url, int preOrderId) {
        return url + (url.contains("?") ? "&" : "?") + "type=campaign&preOrderId=" + preOrderId;
    }

    private static String truncate(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }

    private static int clampSize(int size) {
        return size <= 0 ? 10 : Math.min(size, 100);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String nvl(String s, String def) {
        return s == null || s.isBlank() ? def : s;
    }

    private static String noteSuffix(String note) {
        return note == null || note.isBlank() ? "." : ": " + note + ".";
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static BigDecimal scale2(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }

    private static String plain(BigDecimal v) {
        return v == null ? "0" : v.stripTrailingZeros().toPlainString();
    }

    // ═════════════════════════════ MAPPING ═════════════════════════════

    private CampaignResponse toCampaignResponse(Campaign c) {
        BigDecimal approved = nz(registrationRepository.sumApprovedSurplus(c.getCampaignId()));
        BigDecimal booked = nz(c.getBookedVolumeKg());
        BigDecimal progress = c.getTargetVolumeKg().signum() > 0
                ? booked.multiply(BigDecimal.valueOf(100)).divide(c.getTargetVolumeKg(), 1, RoundingMode.HALF_UP).min(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;
        // Số liệu cung - cầu chỉ có ý nghĩa khi đã chốt đủ ngưỡng
        boolean supply = c.getStatus() == CampaignStatus.FULFILLING || c.getStatus() == CampaignStatus.COMPLETED
                || c.getStatus() == CampaignStatus.THRESHOLD_MET;
        BigDecimal sold = supply ? nz(preOrderRepository.sumKgByStatuses(c.getCampaignId(), PAID_STATUSES)) : BigDecimal.ZERO;
        BigDecimal accepted = supply ? acceptedVolume(c.getCampaignId()) : BigDecimal.ZERO;
        return CampaignResponse.builder()
                .campaignId(c.getCampaignId())
                .code(c.getCode())
                .title(c.getTitle())
                .description(c.getDescription())
                .categoryId(c.getCategory() != null ? c.getCategory().getCategoryId() : null)
                .categoryName(c.getCategory() != null ? c.getCategory().getName() : null)
                .season(c.getSeason())
                .coverImageUrl(c.getCoverImageUrl())
                .targetVolumeKg(c.getTargetVolumeKg())
                .minThresholdKg(c.getMinThresholdKg())
                .rescuePricePerKg(c.getRescuePricePerKg())
                .depositPercent(c.getDepositPercent())
                .cutoffAt(c.getCutoffAt())
                .deliveryDate(c.getDeliveryDate())
                .collectionSiteName(c.getCollectionSiteName())
                .collectionSiteAddress(c.getCollectionSiteAddress())
                .collectionSiteLatitude(c.getCollectionSiteLatitude())
                .collectionSiteLongitude(c.getCollectionSiteLongitude())
                .status(c.getStatus().name())
                .bookedVolumeKg(booked)
                .remainingVolumeKg(c.getStatus() == CampaignStatus.ACTIVE ? remainingVolume(c) : BigDecimal.ZERO)
                .progressPercent(progress)
                .approvedSurplusKg(approved)
                .approvedFarmCount(registrationRepository.countByCampaign_CampaignIdAndStatus(c.getCampaignId(), FarmRegistrationStatus.APPROVED))
                .pendingFarmCount(registrationRepository.countByCampaign_CampaignIdAndStatus(c.getCampaignId(), FarmRegistrationStatus.PENDING))
                .soldVolumeKg(supply ? sold : null)
                .acceptedVolumeKg(supply ? accepted : null)
                .packedVolumeKg(supply ? nz(preOrderRepository.sumKgByStatuses(c.getCampaignId(), PACKED_STATUSES)) : null)
                .shortfallVolumeKg(supply ? sold.subtract(accepted).max(BigDecimal.ZERO) : null)
                .intakePendingCount(supply ? intakePending(c.getCampaignId()) : 0)
                .callPostedAt(c.getCallPostedAt())
                .publishedAt(c.getPublishedAt())
                .evaluatedAt(c.getEvaluatedAt())
                .completedAt(c.getCompletedAt())
                .cancelledAt(c.getCancelledAt())
                .cancelReason(c.getCancelReason())
                .createdAt(c.getCreatedAt())
                .build();
    }

    /** Giá chi trả cho nhà vườn: giá chốt → giá đề xuất → giá giải cứu của chiến dịch (đăng ký cũ chưa có giá) */
    private static BigDecimal effectivePrice(CampaignFarmRegistration r) {
        BigDecimal rescue = r.getCampaign().getRescuePricePerKg();
        if (r.getAgreedPricePerKg() != null) return r.getAgreedPricePerKg().min(rescue);
        if (r.getProposedPricePerKg() != null) return r.getProposedPricePerKg().min(rescue);
        return rescue;
    }

    private FarmRegistrationResponse toRegistrationResponse(CampaignFarmRegistration r) {
        Campaign c = r.getCampaign();
        User s = r.getShop();
        return FarmRegistrationResponse.builder()
                .registrationId(r.getRegistrationId())
                .campaignId(c.getCampaignId())
                .campaignTitle(c.getTitle())
                .campaignStatus(c.getStatus().name())
                .campaignCutoffAt(c.getCutoffAt())
                .collectionSiteName(c.getCollectionSiteName())
                .collectionSiteAddress(c.getCollectionSiteAddress())
                .deliveryDate(c.getDeliveryDate())
                .shopId(s.getId())
                .shopName(nvl(s.getShopName(), s.getFullName()))
                .shopPhone(s.getPhoneNumber())
                .surplusVolumeKg(r.getSurplusVolumeKg())
                .harvestDate(r.getHarvestDate())
                .note(r.getNote())
                .productId(r.getProduct() != null ? r.getProduct().getProductId() : null)
                .productName(r.getProductName())
                .productImageUrls(r.getProductImageUrls() == null || r.getProductImageUrls().isBlank()
                        ? List.of() : Arrays.asList(r.getProductImageUrls().split(",")))
                .proposedPricePerKg(r.getProposedPricePerKg())
                .agreedPricePerKg(r.getAgreedPricePerKg())
                .effectivePricePerKg(effectivePrice(r))
                .campaignPricePerKg(c.getRescuePricePerKg())
                .priceCommitted(r.getPriceCommitted())
                .shelfLifeTier(r.getShelfLifeTier() != null ? r.getShelfLifeTier().name() : null)
                .status(r.getStatus().name())
                .reviewNote(r.getReviewNote())
                .reviewedAt(r.getReviewedAt())
                .allocatedVolumeKg(r.getAllocatedVolumeKg())
                .receivedWeightKg(r.getReceivedWeightKg())
                .rejectedWeightKg(r.getRejectedWeightKg())
                .acceptedWeightKg(r.getAcceptedWeightKg())
                .countedWeightKg(r.getIntakeAt() != null ? countedKg(r) : null)
                .shortfallKg(r.getIntakeAt() != null && r.getAllocatedVolumeKg() != null
                        ? r.getAllocatedVolumeKg().subtract(countedKg(r)).max(BigDecimal.ZERO) : null)
                .qualityPassed(r.getQualityPassed())
                .intakePhotoUrls(r.getIntakePhotoUrls() == null || r.getIntakePhotoUrls().isBlank()
                        ? List.of() : Arrays.asList(r.getIntakePhotoUrls().split(",")))
                .intakeNote(r.getIntakeNote())
                .intakeAt(r.getIntakeAt())
                .payoutAmount(r.getPayoutAmount())
                .paidOutAt(r.getPaidOutAt())
                .createdAt(r.getCreatedAt())
                .build();
    }

    private PreOrderResponse toPreOrderResponse(PreOrder p) {
        CampaignDeposit d = p.getDeposit() != null ? p.getDeposit()
                : depositRepository.findByPreOrder_PreOrderId(p.getPreOrderId()).orElse(null);
        boolean showLink = d != null && d.getStatus() == CampaignDepositStatus.PENDING;
        return PreOrderResponse.builder()
                .preOrderId(p.getPreOrderId())
                .code(p.getCode())
                .campaignId(p.getCampaign().getCampaignId())
                .campaignTitle(p.getCampaign().getTitle())
                .campaignStatus(p.getCampaign().getStatus().name())
                .buyerId(p.getBuyer().getId())
                .buyerName(p.getBuyer().getFullName())
                .crateSize(p.getCrateSize().name())
                .crateKg(p.getCrateSize().getKg())
                .crateQuantity(p.getCrateQuantity())
                .totalKg(p.getTotalKg())
                .unitPrice(p.getUnitPrice())
                .totalAmount(p.getTotalAmount())
                .depositAmount(p.getDepositAmount())
                .remainingAmount(p.getRemainingAmount())
                .recipientName(p.getRecipientName())
                .recipientPhone(p.getRecipientPhone())
                .shippingAddress(p.getShippingAddress())
                .shippingLatitude(p.getShippingLatitude())
                .shippingLongitude(p.getShippingLongitude())
                .note(p.getNote())
                .deliveryZone(p.getDeliveryZone())
                .qrCode(p.getQrCode())
                .status(p.getStatus().name())
                .shipperId(p.getShipper() != null ? p.getShipper().getId() : null)
                .shipperName(p.getShipper() != null ? p.getShipper().getFullName() : null)
                .podImageUrl(p.getPodImageUrl())
                .failReason(p.getFailReason())
                .cancelReason(p.getCancelReason())
                .originalCrateQuantity(p.getOriginalCrateQuantity())
                .shortfallRefundAmount(p.getShortfallRefundAmount())
                .depositStatus(d != null ? d.getStatus().name() : null)
                .payosOrderCode(d != null ? d.getPayosOrderCode() : null)
                .checkoutUrl(showLink ? d.getCheckoutUrl() : null)
                .qrCodeUrl(showLink ? d.getQrCodeUrl() : null)
                .depositPaidAt(p.getDepositPaidAt())
                .packedAt(p.getPackedAt())
                .readyAt(p.getReadyAt())
                .pickedUpAt(p.getPickedUpAt())
                .deliveredAt(p.getDeliveredAt())
                .cancelledAt(p.getCancelledAt())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
