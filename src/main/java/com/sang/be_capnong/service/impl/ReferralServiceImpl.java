package com.sang.be_capnong.service.impl;

import com.sang.be_capnong.config.ReferralProperties;
import com.sang.be_capnong.dto.request.referral.ReferralClaimRequest;
import com.sang.be_capnong.dto.request.referral.ReferralClickTrackRequest;
import com.sang.be_capnong.dto.request.referral.ReferralLinkCreateRequest;
import com.sang.be_capnong.dto.response.PageResponse;
import com.sang.be_capnong.dto.response.referral.ReferralCommissionResponse;
import com.sang.be_capnong.dto.response.referral.ReferralLinkResponse;
import com.sang.be_capnong.dto.response.referral.ReferralResolveResponse;
import com.sang.be_capnong.dto.response.referral.ReferralStatsResponse;
import com.sang.be_capnong.entity.*;
import com.sang.be_capnong.enums.AttributionStatus;
import com.sang.be_capnong.enums.CommissionStatus;
import com.sang.be_capnong.enums.ReferralTargetType;
import com.sang.be_capnong.exception.AppException;
import com.sang.be_capnong.exception.ErrorCode;
import com.sang.be_capnong.mapper.ReferralMapper;
import com.sang.be_capnong.repository.*;
import com.sang.be_capnong.service.ReferralService;
import com.sang.be_capnong.util.SecurityUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ReferralServiceImpl implements ReferralService {

    static final String ALPHANUMERIC = "23456789abcdefghjkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ";
    static final SecureRandom RANDOM = new SecureRandom();

    ReferralLinkRepository referralLinkRepository;
    ReferralClickRepository referralClickRepository;
    ReferralAttributionRepository referralAttributionRepository;
    ReferralCommissionRepository referralCommissionRepository;
    ProductRepository productRepository;
    UserRepository userRepository;
    ReferralMapper referralMapper;
    ReferralProperties referralProperties;

    @Override
    @Transactional
    public ReferralLinkResponse getOrCreateLink(ReferralLinkCreateRequest request) {
        Integer userId = SecurityUtils.getCurrentUserId();
        User referrer = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Kiểm tra đối tượng giới thiệu và chặn tự giới thiệu (Self-referral)
        if (request.getTargetType() == ReferralTargetType.PRODUCT) {
            Product product = productRepository.findById(request.getTargetId())
                    .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

            if (Objects.equals(product.getShop().getId(), userId)) {
                throw new AppException(ErrorCode.SELF_REFERRAL_NOT_ALLOWED);
            }
        } else if (request.getTargetType() == ReferralTargetType.SHOP) {
            User shop = userRepository.findById(request.getTargetId())
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

            if (Objects.equals(shop.getId(), userId)) {
                throw new AppException(ErrorCode.SELF_REFERRAL_NOT_ALLOWED);
            }
        }

        // Tái sử dụng link cũ nếu đã tạo trước đó
        Optional<ReferralLink> existingLink = referralLinkRepository
                .findByReferrer_IdAndTargetTypeAndTargetId(userId, request.getTargetType(), request.getTargetId());

        ReferralLink link = existingLink.orElseGet(() -> {
            String code = generateUniqueReferralCode();
            ReferralLink newLink = ReferralLink.builder()
                    .referralCode(code)
                    .referrer(referrer)
                    .targetType(request.getTargetType())
                    .targetId(request.getTargetId())
                    .commissionRate(null) // Dùng mặc định hệ thống (2.0%)
                    .clicksCount(0)
                    .isActive(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            return referralLinkRepository.save(newLink);
        });

        ReferralLinkResponse response = referralMapper.toReferralLinkResponse(link);
        enrichLinkResponse(response, link);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReferralLinkResponse> getMyLinks(Pageable pageable) {
        Integer userId = SecurityUtils.getCurrentUserId();
        Page<ReferralLink> page = referralLinkRepository.findByReferrer_Id(userId, pageable);

        List<ReferralLinkResponse> content = page.getContent().stream()
                .map(link -> {
                    ReferralLinkResponse resp = referralMapper.toReferralLinkResponse(link);
                    enrichLinkResponse(resp, link);
                    return resp;
                })
                .toList();

        return PageResponse.from(page, content);
    }

    @Override
    @Transactional(readOnly = true)
    public ReferralResolveResponse resolveLink(String referralCode) {
        ReferralLink link = referralLinkRepository.findByReferralCode(referralCode)
                .orElseThrow(() -> new AppException(ErrorCode.REFERRAL_LINK_NOT_FOUND));

        if (!Boolean.TRUE.equals(link.getIsActive())) {
            throw new AppException(ErrorCode.REFERRAL_LINK_INACTIVE);
        }

        ReferralResolveResponse.ReferralResolveResponseBuilder builder = ReferralResolveResponse.builder()
                .referralCode(link.getReferralCode())
                .targetType(link.getTargetType())
                .targetId(link.getTargetId())
                .referrerName("Thành viên CapNong"); // Bảo mật danh tính người giới thiệu

        if (link.getTargetType() == ReferralTargetType.PRODUCT) {
            Product product = productRepository.findById(link.getTargetId()).orElse(null);
            if (product != null) {
                builder.targetName(product.getProductName())
                        .targetUrl("/san-pham/" + product.getProductId())
                        .price(product.getSellingPrice())
                        .shopName(product.getShop() != null ? product.getShop().getShopName() : null);

                if (product.getImages() != null && !product.getImages().isEmpty()) {
                    builder.imageUrl(product.getImages().get(0).getImageUrl());
                }
            }
        } else if (link.getTargetType() == ReferralTargetType.SHOP) {
            User shop = userRepository.findById(link.getTargetId()).orElse(null);
            if (shop != null) {
                builder.targetName(shop.getShopName())
                        .targetUrl("/shop/" + shop.getId())
                        .shopName(shop.getShopName())
                        .imageUrl(shop.getLogoUrl());
            }
        }

        return builder.build();
    }

    @Override
    @Transactional
    public void trackClick(ReferralClickTrackRequest request, String ipAddress, String userAgent) {
        // Rate-limiting: Tối đa số click / IP / phút
        if (ipAddress != null) {
            long recentClicks = referralClickRepository.countByIpAddressAndCreatedAtAfter(
                    ipAddress, LocalDateTime.now().minusMinutes(1));
            if (recentClicks >= referralProperties.getClickRateLimitPerIp()) {
                throw new AppException(ErrorCode.REFERRAL_RATE_LIMIT_EXCEEDED);
            }
        }

        ReferralLink link = referralLinkRepository.findByReferralCode(request.getReferralCode())
                .orElseThrow(() -> new AppException(ErrorCode.REFERRAL_LINK_NOT_FOUND));

        if (!Boolean.TRUE.equals(link.getIsActive())) {
            return;
        }

        Integer buyerId = SecurityUtils.getCurrentUserIdOrNull();
        User buyer = buyerId != null ? userRepository.findById(buyerId).orElse(null) : null;

        // Nếu người tự click là chính người giới thiệu thì không tăng lượt thống kê
        if (buyer != null && Objects.equals(buyer.getId(), link.getReferrer().getId())) {
            return;
        }

        // Cập nhật lượt click trước để tránh deadlock giữa khóa ngoại (FK S-lock) và câu lệnh UPDATE (X-lock)
        referralLinkRepository.incrementClicksCount(link.getId());

        ReferralClick click = ReferralClick.builder()
                .referralLink(link)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .buyer(buyer)
                .createdAt(LocalDateTime.now())
                .build();

        referralClickRepository.save(click);
    }

    @Override
    @Transactional
    public void claimReferral(ReferralClaimRequest request) {
        Integer buyerId = SecurityUtils.getCurrentUserId();
        ReferralLink link = referralLinkRepository.findByReferralCode(request.getReferralCode())
                .orElseThrow(() -> new AppException(ErrorCode.REFERRAL_LINK_NOT_FOUND));

        if (!Boolean.TRUE.equals(link.getIsActive())) {
            throw new AppException(ErrorCode.REFERRAL_LINK_INACTIVE);
        }

        // Chặn tự mua qua link của chính mình
        if (Objects.equals(buyerId, link.getReferrer().getId())) {
            throw new AppException(ErrorCode.SELF_REFERRAL_NOT_ALLOWED);
        }

        // Chặn chủ shop claim link của chính sản phẩm/shop mình
        if (link.getTargetType() == ReferralTargetType.PRODUCT) {
            Product product = productRepository.findById(link.getTargetId()).orElse(null);
            if (product != null && Objects.equals(buyerId, product.getShop().getId())) {
                throw new AppException(ErrorCode.SELF_REFERRAL_NOT_ALLOWED);
            }
        } else if (link.getTargetType() == ReferralTargetType.SHOP) {
            if (Objects.equals(buyerId, link.getTargetId())) {
                throw new AppException(ErrorCode.SELF_REFERRAL_NOT_ALLOWED);
            }
        }

        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // Tìm hoặc tạo mới Attribution (Multi-Conversion trong 7 ngày)
        Optional<ReferralAttribution> existing = referralAttributionRepository
                .findByBuyer_IdAndReferralLink_Id(buyerId, link.getId());

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusDays(referralProperties.getWindowDays());

        ReferralAttribution attribution = existing.orElseGet(() -> ReferralAttribution.builder()
                .buyer(buyer)
                .referralLink(link)
                .referrer(link.getReferrer())
                .targetType(link.getTargetType())
                .targetId(link.getTargetId())
                .maxConversions(referralProperties.getMaxConversionsPerAttribution())
                .build());

        attribution.setAttributedAt(now);
        attribution.setExpiresAt(expiresAt);
        attribution.setConvertedOrderCount(0); // Làm mới chu kỳ chuyển đổi cho đợt click mới
        attribution.setStatus(AttributionStatus.ACTIVE);

        referralAttributionRepository.save(attribution);
        log.info("Claimed referral attribution: buyerId={}, linkCode={}, expiresAt={}",
                buyerId, link.getReferralCode(), expiresAt);
    }

    @Override
    @Transactional(readOnly = true)
    public ReferralStatsResponse getDashboard() {
        Integer userId = SecurityUtils.getCurrentUserId();

        BigDecimal totalEarned = referralCommissionRepository.sumEarnedByReferrer(userId);
        BigDecimal pendingCommission = referralCommissionRepository.sumPendingByReferrer(userId);
        long totalOrders = referralCommissionRepository.countOrdersByReferrer(userId);

        long totalClicks = referralLinkRepository.findByReferrer_Id(userId, Pageable.unpaged())
                .stream()
                .mapToLong(ReferralLink::getClicksCount)
                .sum();

        return ReferralStatsResponse.builder()
                .totalEarned(totalEarned)
                .pendingCommission(pendingCommission)
                .totalClicks(totalClicks)
                .totalOrders(totalOrders)
                .defaultCommissionRate(referralProperties.getDefaultCommissionRate())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReferralCommissionResponse> getMyCommissions(Pageable pageable) {
        Integer userId = SecurityUtils.getCurrentUserId();
        Page<ReferralCommission> page = referralCommissionRepository.findByReferrer_Id(userId, pageable);

        List<ReferralCommissionResponse> content = page.getContent().stream()
                .map(comm -> {
                    ReferralCommissionResponse resp = referralMapper.toReferralCommissionResponse(comm);
                    if (comm.getOrderDetail() != null) {
                        OrderDetail od = comm.getOrderDetail();
                        if (od.getProduct() != null) {
                            Product p = od.getProduct();
                            resp.setProductName(p.getProductName());
                            if (p.getImages() != null && !p.getImages().isEmpty()) {
                                resp.setProductImageUrl(p.getImages().get(0).getImageUrl());
                            }
                        } else if (od.getBuildCombo() != null) {
                            BuildCombo c = od.getBuildCombo();
                            resp.setProductName(c.getComboName());
                            resp.setProductImageUrl(c.getImageUrl());
                        } else if (od.getMysteryBox() != null) {
                            MysteryBox m = od.getMysteryBox();
                            resp.setProductName(m.getBoxType() != null ? m.getBoxType() : "Túi mù");
                            resp.setProductImageUrl(m.getImageUrl());
                        }
                    }
                    return resp;
                })
                .toList();

        return PageResponse.from(page, content);
    }

    /**
     * Kiểm tra dòng hàng có hợp lệ để tính hoa hồng tiếp thị liên kết:
     * - Nông sản đơn lẻ (PRODUCT)
     * - Combo rau củ (BUILD_COMBO)
     * - Túi mù nông sản (MYSTERY_BOX)
     * Loại trừ tuyệt đối: Thực đơn bữa ăn / kế hoạch AI (BUILD_PLAN / PLAN).
     */
    private boolean isEligibleAffiliateItem(OrderDetail detail) {
        if (detail == null || detail.getUnitPrice() == null) {
            return false;
        }
        String type = detail.getItemType();
        if ("BUILD_PLAN".equalsIgnoreCase(type) || "PLAN".equalsIgnoreCase(type) || "MEAL_PLAN".equalsIgnoreCase(type)) {
            return false;
        }
        if ("PRODUCT".equalsIgnoreCase(type) && detail.getProduct() != null) {
            return true;
        }
        if ("BUILD_COMBO".equalsIgnoreCase(type) && detail.getBuildCombo() != null) {
            return true;
        }
        if ("MYSTERY_BOX".equalsIgnoreCase(type) && detail.getMysteryBox() != null) {
            return true;
        }
        if (type == null || type.isBlank()) {
            return detail.getProduct() != null || detail.getBuildCombo() != null || detail.getMysteryBox() != null;
        }
        return false;
    }

    @Override
    @Transactional
    public void processOrderReferral(Order order, List<OrderDetail> details, BigDecimal shopVoucherTotal) {
        if (order == null || details == null || details.isEmpty() || order.getBuyer() == null) {
            return;
        }

        Integer buyerId = order.getBuyer().getId();
        LocalDateTime now = LocalDateTime.now();

        // 1. Quét các attribution còn hiệu lực của Buyer
        List<ReferralAttribution> activeAttrs = referralAttributionRepository
                .findActiveAttributionsForBuyer(buyerId, now);

        if (activeAttrs.isEmpty()) {
            return;
        }

        // Tính tổng tiền hàng của các OrderDetail hợp lệ trong đơn này (để phân bổ voucher shop)
        BigDecimal sumItemGross = BigDecimal.ZERO;
        for (OrderDetail d : details) {
            if (isEligibleAffiliateItem(d)) {
                sumItemGross = sumItemGross.add(d.getUnitPrice().multiply(BigDecimal.valueOf(d.getQuantity())));
            }
        }

        BigDecimal voucherTotal = shopVoucherTotal != null ? shopVoucherTotal : BigDecimal.ZERO;
        BigDecimal farmerFeeRate = new BigDecimal("0.10"); // Phí sàn mặc định 10%

        for (OrderDetail detail : details) {
            // Chỉ áp dụng cho PRODUCT, BUILD_COMBO, MYSTERY_BOX (loại trừ BUILD_PLAN)
            if (!isEligibleAffiliateItem(detail)) {
                continue;
            }

            Integer productId = detail.getProduct() != null ? detail.getProduct().getProductId() : null;
            Integer itemShopId = null;
            if (detail.getProduct() != null && detail.getProduct().getShop() != null) {
                itemShopId = detail.getProduct().getShop().getId();
            } else if (detail.getBuildCombo() != null && detail.getBuildCombo().getShop() != null) {
                itemShopId = detail.getBuildCombo().getShop().getId();
            } else if (detail.getMysteryBox() != null && detail.getMysteryBox().getShop() != null) {
                itemShopId = detail.getMysteryBox().getShop().getId();
            }
            if (itemShopId == null && order.getShop() != null) {
                itemShopId = order.getShop().getId();
            }

            // 2. Tìm attribution khớp theo Strict Last-Touch với Specificity Tie-Breaker
            // activeAttrs đã được sắp xếp ORDER BY attributedAt DESC
            ReferralAttribution winningAttr = null;

            for (ReferralAttribution attr : activeAttrs) {
                // Khớp chính xác sản phẩm nếu link chia sẻ là PRODUCT
                boolean matchDirectProduct = attr.getTargetType() == ReferralTargetType.PRODUCT
                        && productId != null && Objects.equals(attr.getTargetId(), productId);

                // Khớp theo gian hàng (SHOP): link là SHOP của cùng gian hàng
                boolean matchShopLink = attr.getTargetType() == ReferralTargetType.SHOP
                        && itemShopId != null && Objects.equals(attr.getTargetId(), itemShopId);

                // Fallback: nếu link là PRODUCT của cùng gian hàng (khách click link sản phẩm nhưng mua combo/túi mù/món khác của shop)
                boolean matchSameShopProductLink = false;
                if (!matchDirectProduct && !matchShopLink && attr.getTargetType() == ReferralTargetType.PRODUCT && itemShopId != null) {
                    Product refProduct = productRepository.findById(attr.getTargetId()).orElse(null);
                    if (refProduct != null && refProduct.getShop() != null
                            && Objects.equals(refProduct.getShop().getId(), itemShopId)) {
                        matchSameShopProductLink = true;
                    }
                }

                if (matchDirectProduct || matchShopLink || matchSameShopProductLink) {
                    // Chặn tự mua qua link của chính mình hoặc shop của chính mình
                    if (Objects.equals(attr.getReferrer().getId(), buyerId)) {
                        continue;
                    }
                    if (itemShopId != null && Objects.equals(attr.getReferrer().getId(), itemShopId)) {
                        continue;
                    }

                    if (winningAttr == null) {
                        winningAttr = attr;
                    } else {
                        // Tie-breaker:
                        // 1. Ưu tiên khớp chính xác sản phẩm (direct product)
                        // 2. Nếu cùng mức độ ưu tiên và cùng thời điểm: PRODUCT > SHOP
                        boolean currentIsDirect = (winningAttr.getTargetType() == ReferralTargetType.PRODUCT
                                && productId != null && Objects.equals(winningAttr.getTargetId(), productId));
                        if (!currentIsDirect && matchDirectProduct) {
                            winningAttr = attr;
                        } else if (matchDirectProduct == currentIsDirect && attr.getAttributedAt().isEqual(winningAttr.getAttributedAt())) {
                            if (attr.getTargetType() == ReferralTargetType.PRODUCT
                                    && winningAttr.getTargetType() == ReferralTargetType.SHOP) {
                                winningAttr = attr;
                            }
                        }
                    }
                }
            }

            if (winningAttr == null) {
                continue;
            }

            // 3. Tính toán cơ sở và hoa hồng
            BigDecimal itemGross = detail.getUnitPrice().multiply(BigDecimal.valueOf(detail.getQuantity()));
            BigDecimal itemVoucherShare = BigDecimal.ZERO;
            if (sumItemGross.signum() > 0 && voucherTotal.signum() > 0) {
                itemVoucherShare = voucherTotal.multiply(itemGross).divide(sumItemGross, 2, java.math.RoundingMode.HALF_UP);
            }

            BigDecimal commissionBase = itemGross.subtract(itemVoucherShare).max(BigDecimal.ZERO);

            // Tỷ lệ hoa hồng snapshot
            BigDecimal commissionRate = winningAttr.getReferralLink().getCommissionRate() != null
                    ? winningAttr.getReferralLink().getCommissionRate()
                    : referralProperties.getDefaultCommissionRate();

            // Tính hoa hồng thô
            BigDecimal rawCommission = commissionBase.multiply(commissionRate)
                    .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);

            // Safety Cap: Chặn trần theo phí sàn thực thu từ món hàng đó
            BigDecimal maxAllowedPlatformFee = commissionBase.multiply(farmerFeeRate);
            BigDecimal finalCommission = rawCommission.min(maxAllowedPlatformFee).setScale(0, java.math.RoundingMode.HALF_UP);

            // 4. Tạo bản ghi ReferralCommission (PENDING)
            ReferralCommission commission = ReferralCommission.builder()
                    .order(order)
                    .orderDetail(detail)
                    .referralLink(winningAttr.getReferralLink())
                    .referralAttribution(winningAttr)
                    .referrer(winningAttr.getReferrer())
                    .buyer(order.getBuyer())
                    .commissionRate(commissionRate)
                    .baseAmount(commissionBase)
                    .commissionAmount(finalCommission)
                    .status(CommissionStatus.PENDING)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            referralCommissionRepository.save(commission);

            // 5. Tăng bộ đếm đơn chuyển đổi của attribution (nếu maxConversions > 0 thì mới xét trần)
            winningAttr.setConvertedOrderCount(winningAttr.getConvertedOrderCount() + 1);
            if (winningAttr.getMaxConversions() != null && winningAttr.getMaxConversions() > 0
                    && winningAttr.getConvertedOrderCount() >= winningAttr.getMaxConversions()) {
                winningAttr.setStatus(AttributionStatus.EXHAUSTED);
            }
            referralAttributionRepository.save(winningAttr);

            log.info("[ReferralOrder] Created pending commission #{} for orderDetail #{}: rate={}%, base={}, amount={}đ, referrer=#{}",
                    commission.getId(), detail.getOrderDetailId(), commissionRate, commissionBase, finalCommission, winningAttr.getReferrer().getId());
        }
    }

    @Override
    @Transactional
    public void handleOrderCancelled(int orderId) {
        List<ReferralCommission> commissions = referralCommissionRepository.findByOrder_OrderId(orderId);
        for (ReferralCommission comm : commissions) {
            if (comm.getStatus() == CommissionStatus.PENDING) {
                comm.setStatus(CommissionStatus.CANCELLED);
                comm.setUpdatedAt(LocalDateTime.now());
                referralCommissionRepository.save(comm);

                // Hoàn trả bộ đếm đơn chuyển đổi cho attribution
                ReferralAttribution attr = comm.getReferralAttribution();
                if (attr != null && attr.getConvertedOrderCount() > 0) {
                    attr.setConvertedOrderCount(attr.getConvertedOrderCount() - 1);
                    if (attr.getStatus() == AttributionStatus.EXHAUSTED
                            && attr.getExpiresAt().isAfter(LocalDateTime.now())) {
                        attr.setStatus(AttributionStatus.ACTIVE);
                    }
                    referralAttributionRepository.save(attr);
                    log.info("Decremented convertedOrderCount for attribution #{} due to order #{} cancellation",
                            attr.getId(), orderId);
                }
            }
        }
    }

    private void enrichLinkResponse(ReferralLinkResponse response, ReferralLink link) {
        String base = referralProperties.getBaseUrl();
        if (base == null || base.isBlank()) {
            base = "http://localhost:5173/ref/";
        }
        if (!base.endsWith("/")) {
            base += "/";
        }
        response.setShareUrl(base + link.getReferralCode());
        if (response.getCommissionRate() == null) {
            response.setCommissionRate(referralProperties.getDefaultCommissionRate());
        }

        if (link.getTargetType() == ReferralTargetType.PRODUCT) {
            productRepository.findById(link.getTargetId()).ifPresent(product -> {
                response.setTargetName(product.getProductName());
                if (product.getImages() != null && !product.getImages().isEmpty()) {
                    response.setTargetImageUrl(product.getImages().get(0).getImageUrl());
                }
            });
        } else if (link.getTargetType() == ReferralTargetType.SHOP) {
            userRepository.findById(link.getTargetId()).ifPresent(shop -> {
                response.setTargetName(shop.getShopName());
                response.setTargetImageUrl(shop.getLogoUrl());
            });
        }
    }

    private String generateUniqueReferralCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder(8);
            for (int i = 0; i < 8; i++) {
                sb.append(ALPHANUMERIC.charAt(RANDOM.nextInt(ALPHANUMERIC.length())));
            }
            code = sb.toString();
        } while (referralLinkRepository.existsByReferralCode(code));
        return code;
    }
}
