package com.sang.be_capnong.service.campaign;

import com.sang.be_capnong.dto.request.campaign.CampaignUpsertRequest;
import com.sang.be_capnong.dto.request.proposal.*;
import com.sang.be_capnong.dto.response.campaign.CampaignResponse;
import com.sang.be_capnong.dto.response.proposal.CampaignProposalResponse;
import com.sang.be_capnong.dto.response.proposal.NearbyShopResponse;
import com.sang.be_capnong.dto.response.proposal.ProposalMemberResponse;
import com.sang.be_capnong.entity.*;
import com.sang.be_capnong.enums.*;
import com.sang.be_capnong.exception.AppException;
import com.sang.be_capnong.exception.ErrorCode;
import com.sang.be_capnong.repository.*;
import com.sang.be_capnong.service.CampaignService;
import com.sang.be_capnong.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Shop đề xuất mở chiến dịch giải cứu, mời shop lân cận, gửi admin duyệt.
 * Admin có thể gộp nhiều đơn gần nhau thành 1 chiến dịch nháp. Shop khởi xướng không có ưu đãi riêng:
 * khi admin đăng lời kêu gọi, mọi shop (kể cả shop không được mời) đăng ký như bình thường.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CampaignProposalService {

    public static final BigDecimal MIN_SUBMIT_KG = BigDecimal.valueOf(200);
    public static final double DEFAULT_RADIUS_KM = 10;
    public static final double MAX_RADIUS_KM = 50;
    static final int MAX_INVITES_PER_PROPOSAL = 20;
    static final int MAX_INVITES_PER_DAY = 30;
    static final int INVITE_TTL_HOURS = 72;
    static final int MIN_HARVEST_LEAD_DAYS = 3;
    static final int COOLDOWN_REJECTS = 3;
    static final int COOLDOWN_WINDOW_DAYS = 30;
    static final int COOLDOWN_DAYS = 7;
    static final int MAX_NEARBY = 50;

    private static final List<CampaignProposalStatus> OPEN = List.of(
            CampaignProposalStatus.DRAFT, CampaignProposalStatus.SUBMITTED, CampaignProposalStatus.NEEDS_CHANGES);

    private final CampaignProposalRepository proposalRepository;
    private final CampaignProposalMemberRepository memberRepository;
    private final CampaignRepository campaignRepository;
    private final CampaignFarmRegistrationRepository registrationRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final CampaignService campaignService;
    private final NotificationService notificationService;

    private final SecureRandom random = new SecureRandom();

    // ═════════════════════════════ SHOP KHỞI XƯỚNG ═════════════════════════════

    @Transactional
    public CampaignProposalResponse create(CampaignProposalRequest req) {
        User me = currentShop();
        requireVerified(me);
        if (proposalRepository.existsByInitiator_IdAndStatusIn(me.getId(), OPEN)) {
            throw new AppException(ErrorCode.PROPOSAL_OPEN_EXISTS);
        }
        checkCooldown(me);
        LocalDateTime now = LocalDateTime.now();
        CampaignProposal p = CampaignProposal.builder()
                .code(nextCode())
                .initiator(me)
                .status(CampaignProposalStatus.DRAFT)
                .createdAt(now)
                .updatedAt(now)
                .build();
        apply(p, req);
        p = proposalRepository.save(p);
        CampaignProposalMember self = CampaignProposalMember.builder()
                .proposal(p)
                .shop(me)
                .role(ProposalMemberRole.INITIATOR)
                .status(ProposalMemberStatus.ACCEPTED)
                .estimatedKg(positiveOrNull(req.getEstimatedKg()))
                .distanceKm(distanceTo(p, me))
                .invitedAt(now)
                .respondedAt(now)
                .build();
        memberRepository.save(self);
        p.getMembers().add(self);
        return toResponse(p, me, false);
    }

    @Transactional
    public CampaignProposalResponse update(int id, CampaignProposalRequest req) {
        User me = currentShop();
        CampaignProposal p = lockOwned(id, me);
        if (!p.getStatus().editable()) throw new AppException(ErrorCode.PROPOSAL_INVALID_STATUS);
        apply(p, req);
        p.setUpdatedAt(LocalDateTime.now());
        memberRepository.findByProposal_ProposalIdAndShop_Id(id, me.getId()).ifPresent(m -> {
            m.setEstimatedKg(positiveOrNull(req.getEstimatedKg()));
            m.setDistanceKm(distanceTo(p, me));
            memberRepository.save(m);
        });
        return toResponse(proposalRepository.save(p), me, false);
    }

    @Transactional(readOnly = true)
    public CampaignProposalResponse get(int id) {
        User me = currentShop();
        CampaignProposal p = find(id);
        boolean owner = p.getInitiator().getId() == me.getId();
        if (!owner && memberRepository.findByProposal_ProposalIdAndShop_Id(id, me.getId()).isEmpty()) {
            throw new AppException(ErrorCode.PROPOSAL_NOT_OWNER);
        }
        return toResponse(p, me, false);
    }

    @Transactional(readOnly = true)
    public List<CampaignProposalResponse> my() {
        User me = currentShop();
        return proposalRepository.findByInitiator_IdOrderByCreatedAtDesc(me.getId()).stream()
                .map(p -> toResponse(p, me, false)).toList();
    }

    /** Lời mời shop nhận được (mới nhất trước) */
    @Transactional(readOnly = true)
    public List<CampaignProposalResponse> myInvites() {
        User me = currentShop();
        return memberRepository.findByShop_IdAndRoleOrderByInvitedAtDesc(me.getId(), ProposalMemberRole.INVITED).stream()
                .filter(m -> m.getStatus() != ProposalMemberStatus.REMOVED)
                .map(m -> toResponse(m.getProposal(), me, false)).toList();
    }

    /** Shop đã xác minh quanh điểm tập kết. Khoảng cách làm tròn 0,5 km, toạ độ làm tròn ~1 km */
    @Transactional(readOnly = true)
    public List<NearbyShopResponse> nearbyShops(int id, Double radiusKm, boolean sameCategoryOnly) {
        User me = currentShop();
        CampaignProposal p = find(id);
        if (p.getInitiator().getId() != me.getId()) throw new AppException(ErrorCode.PROPOSAL_NOT_OWNER);
        if (p.getSiteLatitude() == null || p.getSiteLongitude() == null) throw new AppException(ErrorCode.PROPOSAL_SITE_REQUIRED);
        double r = radiusKm == null ? DEFAULT_RADIUS_KM : Math.max(1, Math.min(MAX_RADIUS_KM, radiusKm));
        double lat = p.getSiteLatitude(), lng = p.getSiteLongitude();
        double dLat = r / 111.0;
        double dLng = r / (111.0 * Math.max(Math.cos(Math.toRadians(lat)), 0.01));

        Map<Integer, CampaignProposalMember> invited = memberRepository.findByProposal_ProposalIdOrderByMemberIdAsc(id).stream()
                .collect(Collectors.toMap(m -> m.getShop().getId(), m -> m, (a, b) -> a));

        record Hit(User shop, double km) {}
        List<Hit> hits = new ArrayList<>();
        for (User s : userRepository.findShopsInBox(lat - dLat, lat + dLat, lng - dLng, lng + dLng)) {
            if (s.getId() == me.getId() || !isVerified(s)) continue;
            double km = haversineKm(lat, lng, s.getShopLatitude(), s.getShopLongitude());
            if (km <= r) hits.add(new Hit(s, km));
        }
        hits.sort(Comparator.comparingDouble(Hit::km));

        Map<Integer, List<Object[]>> cats = hits.isEmpty() ? Map.of()
                : proposalRepository.findShopCategories(hits.stream().map(h -> h.shop().getId()).toList()).stream()
                .collect(Collectors.groupingBy(row -> (Integer) row[0]));
        Integer catId = p.getCategory() == null ? null : p.getCategory().getCategoryId();

        List<NearbyShopResponse> out = new ArrayList<>();
        for (Hit h : hits) {
            List<Object[]> rows = cats.getOrDefault(h.shop().getId(), List.of());
            boolean same = catId != null && rows.stream().anyMatch(row -> catId.equals(row[1]));
            if (sameCategoryOnly && !same) continue;
            CampaignProposalMember m = invited.get(h.shop().getId());
            out.add(NearbyShopResponse.builder()
                    .shopId(h.shop().getId())
                    .shopName(shopName(h.shop()))
                    .logoUrl(h.shop().getLogoUrl())
                    .ratingAverage(h.shop().getRatingAverage())
                    .distanceKm(Math.max(0.5, Math.round(h.km() * 2) / 2.0))
                    .approxLatitude(Math.round(h.shop().getShopLatitude() * 100) / 100.0)
                    .approxLongitude(Math.round(h.shop().getShopLongitude() * 100) / 100.0)
                    .categories(rows.stream().map(row -> (String) row[2]).toList())
                    .sameCategory(same)
                    .inviteStatus(m == null ? null : m.getStatus().name())
                    .build());
            if (out.size() >= MAX_NEARBY) break;
        }
        return out;
    }

    @Transactional
    public CampaignProposalResponse invite(int id, ProposalInviteRequest req) {
        User me = currentShop();
        CampaignProposal p = lockOwned(id, me);
        if (!p.getStatus().editable()) throw new AppException(ErrorCode.PROPOSAL_INVALID_STATUS);
        if (req == null || req.getShopIds() == null || req.getShopIds().isEmpty()) throw new AppException(ErrorCode.PROPOSAL_INVALID);
        String message = trim(req.getMessage(), 300);
        Set<Integer> ids = new LinkedHashSet<>(req.getShopIds());
        ids.remove(me.getId());

        long used = memberRepository.countByProposal_ProposalIdAndRoleAndStatusNot(id, ProposalMemberRole.INVITED, ProposalMemberStatus.REMOVED);
        long today = memberRepository.countInvitesSentSince(me.getId(), LocalDateTime.now().minusHours(24));
        LocalDateTime now = LocalDateTime.now();
        for (Integer shopId : ids) {
            Optional<CampaignProposalMember> existing = memberRepository.findByProposal_ProposalIdAndShop_Id(id, shopId);
            if (existing.isPresent() && existing.get().getStatus() != ProposalMemberStatus.REMOVED) continue;
            if (used >= MAX_INVITES_PER_PROPOSAL) throw new AppException(ErrorCode.PROPOSAL_INVITE_LIMIT);
            if (today >= MAX_INVITES_PER_DAY) throw new AppException(ErrorCode.PROPOSAL_INVITE_DAILY_LIMIT);
            User shop = userRepository.findById(shopId).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
            if (!isVerified(shop)) throw new AppException(ErrorCode.PROPOSAL_SHOP_NOT_VERIFIED);

            CampaignProposalMember m = existing.orElseGet(() -> CampaignProposalMember.builder()
                    .proposal(p).shop(shop).role(ProposalMemberRole.INVITED).build());
            m.setStatus(ProposalMemberStatus.PENDING);
            m.setEstimatedKg(null);
            m.setMessage(message);
            m.setDistanceKm(distanceTo(p, shop));
            m.setInvitedAt(now);
            m.setRespondedAt(null);
            m.setExpiresAt(now.plusHours(INVITE_TTL_HOURS));
            memberRepository.save(m);
            used++;
            today++;
            notify(shop, "Lời mời cùng đề xuất chiến dịch",
                    shopName(me) + " mời bạn cùng đề xuất chiến dịch giải cứu "
                            + (p.getProductName() == null ? "" : "\"" + p.getProductName() + "\" ")
                            + "gần bạn. Trả lời trong " + INVITE_TTL_HOURS + " giờ.");
        }
        p.setUpdatedAt(now);
        proposalRepository.save(p);
        return toResponse(reload(id), me, false);
    }

    @Transactional
    public CampaignProposalResponse removeMember(int id, int shopId) {
        User me = currentShop();
        CampaignProposal p = lockOwned(id, me);
        if (!p.getStatus().editable()) throw new AppException(ErrorCode.PROPOSAL_INVALID_STATUS);
        CampaignProposalMember m = memberRepository.findByProposal_ProposalIdAndShop_Id(id, shopId)
                .orElseThrow(() -> new AppException(ErrorCode.PROPOSAL_MEMBER_NOT_FOUND));
        if (m.getRole() == ProposalMemberRole.INITIATOR) throw new AppException(ErrorCode.PROPOSAL_INVALID);
        m.setStatus(ProposalMemberStatus.REMOVED);
        m.setRespondedAt(LocalDateTime.now());
        memberRepository.save(m);
        return toResponse(reload(id), me, false);
    }

    @Transactional
    public CampaignProposalResponse submit(int id) {
        User me = currentShop();
        requireVerified(me);
        CampaignProposal p = lockOwned(id, me);
        if (!p.getStatus().editable()) throw new AppException(ErrorCode.PROPOSAL_INVALID_STATUS);
        validateForSubmit(p);
        BigDecimal total = totalKg(memberRepository.findByProposal_ProposalIdOrderByMemberIdAsc(id));
        if (total.compareTo(MIN_SUBMIT_KG) < 0) throw new AppException(ErrorCode.PROPOSAL_MIN_KG);
        LocalDateTime now = LocalDateTime.now();
        p.setStatus(CampaignProposalStatus.SUBMITTED);
        p.setSubmittedAt(now);
        p.setUpdatedAt(now);
        proposalRepository.save(p);
        for (User admin : userRepository.findByRole_NameInAndStatus(List.of("ADMIN"), UserStatus.ACTIVE)) {
            notify(admin, "Đề xuất chiến dịch mới " + p.getCode(),
                    shopName(me) + " đề xuất giải cứu " + p.getProductName() + " (dự kiến " + plain(total) + " kg) tại "
                            + p.getSiteName() + ".");
        }
        return toResponse(p, me, false);
    }

    @Transactional
    public CampaignProposalResponse withdraw(int id) {
        User me = currentShop();
        CampaignProposal p = lockOwned(id, me);
        if (!p.getStatus().open()) throw new AppException(ErrorCode.PROPOSAL_INVALID_STATUS);
        p.setStatus(CampaignProposalStatus.WITHDRAWN);
        p.setUpdatedAt(LocalDateTime.now());
        proposalRepository.save(p);
        closeInvites(List.of(p), "Đơn đề xuất " + p.getCode() + " đã được shop khởi xướng rút lại.");
        return toResponse(p, me, false);
    }

    // ═════════════════════════════ SHOP ĐƯỢC MỜI ═════════════════════════════

    @Transactional
    public CampaignProposalResponse acceptInvite(int memberId, ProposalReplyRequest req) {
        User me = currentShop();
        requireVerified(me);
        CampaignProposalMember m = lockMyInvite(memberId, me);
        BigDecimal kg = req == null ? null : positiveOrNull(req.getEstimatedKg());
        if (kg == null) throw new AppException(ErrorCode.PROPOSAL_INVALID);
        m.setStatus(ProposalMemberStatus.ACCEPTED);
        m.setEstimatedKg(kg);
        m.setRespondedAt(LocalDateTime.now());
        memberRepository.save(m);
        CampaignProposal p = m.getProposal();
        notify(p.getInitiator(), "Shop lân cận đã nhận lời",
                shopName(me) + " tham gia đề xuất " + p.getCode() + " với khoảng " + plain(kg) + " kg.");
        return toResponse(p, me, false);
    }

    @Transactional
    public CampaignProposalResponse declineInvite(int memberId) {
        User me = currentShop();
        CampaignProposalMember m = lockMyInvite(memberId, me);
        m.setStatus(ProposalMemberStatus.DECLINED);
        m.setEstimatedKg(null);
        m.setRespondedAt(LocalDateTime.now());
        memberRepository.save(m);
        notify(m.getProposal().getInitiator(), "Shop lân cận đã từ chối",
                shopName(me) + " không tham gia đề xuất " + m.getProposal().getCode() + ".");
        return toResponse(m.getProposal(), me, false);
    }

    // ═════════════════════════════ ADMIN ═════════════════════════════

    @Transactional(readOnly = true)
    public List<CampaignProposalResponse> adminList(String status) {
        User admin = currentUser();
        List<CampaignProposal> list;
        if (status == null || status.isBlank()) {
            list = proposalRepository.findAllByOrderByCreatedAtDesc().stream()
                    .filter(p -> p.getStatus() != CampaignProposalStatus.DRAFT).toList();
        } else {
            CampaignProposalStatus st;
            try {
                st = CampaignProposalStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new AppException(ErrorCode.PROPOSAL_INVALID);
            }
            list = proposalRepository.findByStatusInOrderBySubmittedAtAsc(List.of(st));
        }
        return list.stream().map(p -> toResponse(p, admin, true)).toList();
    }

    @Transactional(readOnly = true)
    public CampaignProposalResponse adminGet(int id) {
        return toResponse(find(id), currentUser(), true);
    }

    /** Duyệt 1 hoặc nhiều đơn đang chờ: tạo 1 chiến dịch nháp, mọi đơn trỏ về chiến dịch đó */
    @Transactional
    public CampaignResponse approve(ProposalApproveRequest req) {
        if (req == null || req.getProposalIds() == null || req.getProposalIds().isEmpty() || req.getCampaign() == null) {
            throw new AppException(ErrorCode.PROPOSAL_INVALID);
        }
        User admin = currentUser();
        List<CampaignProposal> proposals = new ArrayList<>();
        for (Integer pid : new TreeSet<>(req.getProposalIds())) {
            CampaignProposal p = proposalRepository.findByIdForUpdate(pid)
                    .orElseThrow(() -> new AppException(ErrorCode.PROPOSAL_NOT_FOUND));
            if (p.getStatus() != CampaignProposalStatus.SUBMITTED) throw new AppException(ErrorCode.PROPOSAL_INVALID_STATUS);
            proposals.add(p);
        }
        CampaignUpsertRequest cr = req.getCampaign();
        CampaignResponse created = campaignService.createCampaign(cr);
        Campaign campaign = campaignRepository.findById(created.getCampaignId())
                .orElseThrow(() -> new AppException(ErrorCode.PROPOSAL_NOT_FOUND));
        LocalDateTime now = LocalDateTime.now();
        String note = trim(req.getNote(), 500);
        for (CampaignProposal p : proposals) {
            p.setStatus(CampaignProposalStatus.APPROVED);
            p.setCampaign(campaign);
            p.setReviewedBy(admin);
            p.setReviewedAt(now);
            p.setAdminNote(note);
            p.setUpdatedAt(now);
            proposalRepository.save(p);
        }
        closeInvites(proposals, null);
        String merged = proposals.size() > 1 ? " (gộp " + proposals.size() + " đơn gần nhau)" : "";
        for (CampaignProposal p : proposals) {
            // So bài đăng admin soạn với đơn shop gửi: giống thì báo duyệt bình thường, khác thì liệt kê thay đổi
            List<String> changes = diffWithProposal(p, cr);
            String title = changes.isEmpty() ? "Đề xuất chiến dịch đã được duyệt" : "Đề xuất " + p.getCode() + " được duyệt, có điều chỉnh";
            String publish = cr.getPublishAt() != null ? " Bài đăng dự kiến lên lúc " + fmtDateTime(cr.getPublishAt()) + "." : "";
            String body = "Đề xuất " + p.getCode() + " đã được duyệt thành chiến dịch \"" + created.getTitle() + "\"" + merged + "."
                    + (changes.isEmpty() ? ""
                    : " CapNong đã điều chỉnh so với đề xuất của bạn: " + String.join("; ", changes) + ". Có thắc mắc hãy nhắn tin cho CapNong.")
                    + publish
                    + " Khi CapNong đăng lời kêu gọi, hãy vào đăng ký số kg như mọi shop khác.";
            for (CampaignProposalMember m : memberRepository.findByProposal_ProposalIdOrderByMemberIdAsc(p.getProposalId())) {
                if (m.getStatus() != ProposalMemberStatus.ACCEPTED) continue;
                notify(m.getShop(), title, body);
            }
        }
        return created;
    }

    @Transactional
    public CampaignProposalResponse requestChanges(int id, ProposalReplyRequest req) {
        return review(id, req, CampaignProposalStatus.NEEDS_CHANGES);
    }

    @Transactional
    public CampaignProposalResponse reject(int id, ProposalReplyRequest req) {
        return review(id, req, CampaignProposalStatus.REJECTED);
    }

    private CampaignProposalResponse review(int id, ProposalReplyRequest req, CampaignProposalStatus to) {
        String note = req == null ? null : trim(req.getNote(), 500);
        if (note == null) throw new AppException(ErrorCode.PROPOSAL_NOTE_REQUIRED);
        User admin = currentUser();
        CampaignProposal p = proposalRepository.findByIdForUpdate(id).orElseThrow(() -> new AppException(ErrorCode.PROPOSAL_NOT_FOUND));
        if (p.getStatus() != CampaignProposalStatus.SUBMITTED) throw new AppException(ErrorCode.PROPOSAL_INVALID_STATUS);
        LocalDateTime now = LocalDateTime.now();
        p.setStatus(to);
        p.setAdminNote(note);
        p.setReviewedBy(admin);
        p.setReviewedAt(now);
        p.setUpdatedAt(now);
        proposalRepository.save(p);
        if (to == CampaignProposalStatus.REJECTED) {
            closeInvites(List.of(p), null);
            notify(p.getInitiator(), "Đề xuất " + p.getCode() + " bị từ chối", "Lý do: " + note);
        } else {
            notify(p.getInitiator(), "Đề xuất " + p.getCode() + " cần bổ sung", "Admin ghi chú: " + note + ". Sửa xong hãy gửi lại.");
        }
        return toResponse(p, admin, true);
    }

    // ═════════════════════════════ NỀN ═════════════════════════════

    /** Lời mời chờ quá 72 giờ -> hết hạn */
    @Transactional
    public int expireStaleInvites() {
        List<CampaignProposalMember> stale = memberRepository.findTop500ByStatusAndExpiresAtBefore(ProposalMemberStatus.PENDING, LocalDateTime.now());
        for (CampaignProposalMember m : stale) {
            m.setStatus(ProposalMemberStatus.EXPIRED);
            m.setRespondedAt(LocalDateTime.now());
        }
        memberRepository.saveAll(stale);
        return stale.size();
    }

    // ═════════════════════════════ HELPERS ═════════════════════════════

    private void apply(CampaignProposal p, CampaignProposalRequest r) {
        if (r == null) throw new AppException(ErrorCode.PROPOSAL_INVALID);
        Map<String, String> errors = new LinkedHashMap<>();
        if (r.getCategoryId() != null) {
            Category cat = categoryRepository.findById(r.getCategoryId()).orElse(null);
            if (cat == null) errors.put("categoryId", "Danh mục này không còn trên hệ thống, hãy chọn lại");
            p.setCategory(cat);
        } else {
            p.setCategory(null);
        }
        p.setProductName(trim(r.getProductName(), 200));
        List<String> photos = r.getPhotoUrls() == null ? List.of()
                : r.getPhotoUrls().stream().filter(Objects::nonNull).map(String::trim)
                .filter(s -> !s.isEmpty() && !s.contains(",")).distinct().toList();
        if (photos.size() > 5) errors.put("photoUrls", "Tối đa 5 ảnh");
        p.setPhotoUrls(photos.isEmpty() ? null : String.join(",", photos));
        p.setStory(trim(r.getStory(), 3000));
        p.setHarvestFrom(r.getHarvestFrom());
        p.setHarvestTo(r.getHarvestTo());
        p.setProposedOpenDate(r.getProposedOpenDate());
        p.setProposedCloseDate(r.getProposedCloseDate());
        p.setShelfLifeTier(r.getShelfLifeTier());
        p.setDesiredPricePerKg(positiveOrNull(r.getDesiredPricePerKg()));
        p.setMarketPricePerKg(positiveOrNull(r.getMarketPricePerKg()));
        p.setSiteName(trim(r.getSiteName(), 200));
        p.setSiteAddress(trim(r.getSiteAddress(), 500));
        Double lat = r.getSiteLatitude(), lng = r.getSiteLongitude();
        if (lat != null && lng != null && (Math.abs(lat) > 90 || Math.abs(lng) > 180)) errors.put("site", "Vị trí ghim trên bản đồ không hợp lệ");
        p.setSiteLatitude(lat);
        p.setSiteLongitude(lng);
        throwFieldErrors(errors);
    }

    /** Lỗi theo từng ô: message = lỗi đầu tiên, result = {tên ô: lỗi} để FE hiện ngay dưới ô đó */
    private static void throwFieldErrors(Map<String, String> errors) {
        if (errors.isEmpty()) return;
        throw new AppException(ErrorCode.PROPOSAL_INVALID, errors.values().iterator().next(), Map.of("fieldErrors", errors));
    }

    private void validateForSubmit(CampaignProposal p) {
        Map<String, String> e = new LinkedHashMap<>();
        if (p.getCategory() == null) e.put("categoryId", "Chọn danh mục");
        if (p.getProductName() == null) e.put("productName", "Nhập tên nông sản");
        if (p.getStory() == null) e.put("story", "Kể ngắn vì sao cần giải cứu");
        if (p.getPhotoUrls() == null) e.put("photoUrls", "Thêm ít nhất 1 ảnh");
        if (p.getShelfLifeTier() == null) e.put("shelfLifeTier", "Chọn độ bền sau khi kết thúc chiến dịch");
        if (p.getDesiredPricePerKg() == null) e.put("desiredPricePerKg", "Nhập giá mong muốn");
        LocalDate today = LocalDate.now();
        if (p.getHarvestFrom() == null) e.put("harvestFrom", "Chọn ngày bắt đầu thu hoạch");
        else if (p.getHarvestFrom().isBefore(today.plusDays(MIN_HARVEST_LEAD_DAYS))) e.put("harvestFrom", ErrorCode.PROPOSAL_HARVEST_TOO_SOON.getMessage());
        if (p.getHarvestTo() == null) e.put("harvestTo", "Chọn ngày thu hoạch xong");
        else if (p.getHarvestFrom() != null && p.getHarvestTo().isBefore(p.getHarvestFrom())) e.put("harvestTo", "Không được trước ngày bắt đầu thu hoạch");
        if (p.getProposedOpenDate() == null) e.put("proposedOpenDate", "Chọn ngày mở chiến dịch");
        else if (!p.getProposedOpenDate().isAfter(today)) e.put("proposedOpenDate", "Ngày mở phải từ ngày mai trở đi");
        else if (p.getHarvestTo() != null && !p.getProposedOpenDate().isAfter(p.getHarvestTo())) e.put("proposedOpenDate", "Phải sau ngày thu hoạch xong (" + fmtDate(p.getHarvestTo()) + ")");
        if (p.getProposedCloseDate() == null) e.put("proposedCloseDate", "Chọn ngày kết thúc chiến dịch");
        else if (p.getProposedOpenDate() != null && !p.getProposedCloseDate().isAfter(p.getProposedOpenDate())) e.put("proposedCloseDate", "Phải sau ngày mở chiến dịch");
        else if (p.getHarvestTo() != null && p.getHarvestTo().isAfter(p.getProposedCloseDate())) e.put("proposedCloseDate", "Không được trước ngày thu hoạch xong (hàng thu hoạch không sau ngày kết thúc)");
        if (p.getSiteName() == null) e.put("siteName", "Nhập tên điểm tập kết");
        if (p.getSiteAddress() == null || p.getSiteLatitude() == null || p.getSiteLongitude() == null) e.put("site", "Chọn địa chỉ và ghim điểm tập kết trên bản đồ");
        throwFieldErrors(e);
    }

    /** 3 lần bị từ chối trong 30 ngày -> tạm khoá tạo đơn mới 7 ngày kể từ lần từ chối gần nhất */
    private void checkCooldown(User me) {
        List<CampaignProposal> rejected = proposalRepository.findByInitiator_IdAndStatusAndReviewedAtAfter(
                me.getId(), CampaignProposalStatus.REJECTED, LocalDateTime.now().minusDays(COOLDOWN_WINDOW_DAYS));
        if (rejected.size() < COOLDOWN_REJECTS) return;
        LocalDateTime last = rejected.stream().map(CampaignProposal::getReviewedAt).filter(Objects::nonNull)
                .max(Comparator.naturalOrder()).orElse(LocalDateTime.MIN);
        if (last.plusDays(COOLDOWN_DAYS).isAfter(LocalDateTime.now())) throw new AppException(ErrorCode.PROPOSAL_COOLDOWN);
    }

    /** Lời mời đang chờ của các đơn đã đóng -> hết hạn; báo cho shop được mời nếu có lời nhắn */
    private void closeInvites(List<CampaignProposal> proposals, String message) {
        List<CampaignProposalMember> pending = memberRepository.findByProposal_ProposalIdInAndStatusIn(
                proposals.stream().map(CampaignProposal::getProposalId).toList(), List.of(ProposalMemberStatus.PENDING));
        LocalDateTime now = LocalDateTime.now();
        for (CampaignProposalMember m : pending) {
            m.setStatus(ProposalMemberStatus.EXPIRED);
            m.setRespondedAt(now);
            if (message != null) notify(m.getShop(), "Lời mời đã đóng", message);
        }
        memberRepository.saveAll(pending);
    }

    private CampaignProposalMember lockMyInvite(int memberId, User me) {
        CampaignProposalMember m = memberRepository.findByIdForUpdate(memberId)
                .orElseThrow(() -> new AppException(ErrorCode.PROPOSAL_MEMBER_NOT_FOUND));
        if (m.getShop().getId() != me.getId() || m.getRole() != ProposalMemberRole.INVITED) {
            throw new AppException(ErrorCode.PROPOSAL_MEMBER_NOT_FOUND);
        }
        if (m.getStatus() != ProposalMemberStatus.PENDING && m.getStatus() != ProposalMemberStatus.ACCEPTED
                && m.getStatus() != ProposalMemberStatus.DECLINED) {
            throw new AppException(ErrorCode.PROPOSAL_INVITE_EXPIRED);
        }
        if (m.getStatus() == ProposalMemberStatus.PENDING && m.getExpiresAt() != null && m.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new AppException(ErrorCode.PROPOSAL_INVITE_EXPIRED);
        }
        if (!m.getProposal().getStatus().editable()) throw new AppException(ErrorCode.PROPOSAL_INVALID_STATUS);
        return m;
    }

    private CampaignProposal lockOwned(int id, User me) {
        CampaignProposal p = proposalRepository.findByIdForUpdate(id).orElseThrow(() -> new AppException(ErrorCode.PROPOSAL_NOT_FOUND));
        if (p.getInitiator().getId() != me.getId()) throw new AppException(ErrorCode.PROPOSAL_NOT_OWNER);
        return p;
    }

    private CampaignProposal find(int id) {
        return proposalRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.PROPOSAL_NOT_FOUND));
    }

    private CampaignProposal reload(int id) {
        CampaignProposal p = find(id);
        p.setMembers(new ArrayList<>(memberRepository.findByProposal_ProposalIdOrderByMemberIdAsc(id)));
        return p;
    }

    private static BigDecimal totalKg(List<CampaignProposalMember> members) {
        return members.stream()
                .filter(m -> m.getStatus() == ProposalMemberStatus.ACCEPTED && m.getEstimatedKg() != null)
                .map(CampaignProposalMember::getEstimatedKg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private CampaignProposalResponse toResponse(CampaignProposal p, User viewer, boolean admin) {
        List<CampaignProposalMember> members = memberRepository.findByProposal_ProposalIdOrderByMemberIdAsc(p.getProposalId());
        boolean owner = p.getInitiator().getId() == viewer.getId();
        CampaignProposalMember mine = members.stream().filter(m -> m.getShop().getId() == viewer.getId()).findFirst().orElse(null);
        boolean viewerJoined = owner || (mine != null && mine.getStatus() == ProposalMemberStatus.ACCEPTED);

        List<ProposalMemberResponse> memberDtos = members.stream()
                .filter(m -> admin || owner || m.getStatus() == ProposalMemberStatus.ACCEPTED || m == mine)
                .filter(m -> m.getStatus() != ProposalMemberStatus.REMOVED || admin)
                .map(m -> toMember(m, admin || (viewerJoined && m.getStatus() == ProposalMemberStatus.ACCEPTED)))
                .toList();
        Campaign c = p.getCampaign();
        User init = p.getInitiator();
        return CampaignProposalResponse.builder()
                .proposalId(p.getProposalId())
                .code(p.getCode())
                .status(p.getStatus().name())
                .initiatorShopId(init.getId())
                .initiatorShopName(shopName(init))
                .initiatorLogoUrl(init.getLogoUrl())
                .initiatorRating(init.getRatingAverage())
                .initiatorVerified(isVerified(init))
                .categoryId(p.getCategory() == null ? null : p.getCategory().getCategoryId())
                .categoryName(p.getCategory() == null ? null : p.getCategory().getName())
                .productName(p.getProductName())
                .photoUrls(p.getPhotoUrls() == null ? List.of() : Arrays.asList(p.getPhotoUrls().split(",")))
                .story(p.getStory())
                .harvestFrom(p.getHarvestFrom())
                .harvestTo(p.getHarvestTo())
                .proposedOpenDate(p.getProposedOpenDate())
                .proposedCloseDate(p.getProposedCloseDate())
                .shelfLifeTier(p.getShelfLifeTier() == null ? null : p.getShelfLifeTier().name())
                .desiredPricePerKg(p.getDesiredPricePerKg())
                .marketPricePerKg(p.getMarketPricePerKg())
                .siteName(p.getSiteName())
                .siteAddress(p.getSiteAddress())
                .siteLatitude(p.getSiteLatitude())
                .siteLongitude(p.getSiteLongitude())
                .totalEstimatedKg(totalKg(members))
                .acceptedShopCount((int) members.stream().filter(m -> m.getStatus() == ProposalMemberStatus.ACCEPTED).count())
                .pendingInviteCount((int) members.stream().filter(m -> m.getStatus() == ProposalMemberStatus.PENDING).count())
                .minSubmitKg(MIN_SUBMIT_KG)
                .adminNote(owner || admin ? p.getAdminNote() : null)
                .reviewedAt(p.getReviewedAt())
                .campaignId(c == null ? null : c.getCampaignId())
                .campaignCode(c == null ? null : c.getCode())
                .campaignStatus(c == null ? null : c.getStatus().name())
                .submittedAt(p.getSubmittedAt())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .myInvite(mine == null || mine.getRole() == ProposalMemberRole.INITIATOR ? null : toMember(mine, false))
                .members(memberDtos)
                .initiatorPastRegistrations(admin ? (long) registrationRepository.findByShop_IdOrderByCreatedAtDesc(init.getId()).size() : null)
                .build();
    }

    private ProposalMemberResponse toMember(CampaignProposalMember m, boolean showPhone) {
        User s = m.getShop();
        return ProposalMemberResponse.builder()
                .memberId(m.getMemberId())
                .shopId(s.getId())
                .shopName(shopName(s))
                .logoUrl(s.getLogoUrl())
                .ratingAverage(s.getRatingAverage())
                .phone(showPhone ? s.getPhoneNumber() : null)
                .role(m.getRole().name())
                .status(m.getStatus().name())
                .estimatedKg(m.getEstimatedKg())
                .message(m.getMessage())
                .distanceKm(m.getDistanceKm() == null ? null : Math.round(m.getDistanceKm() * 2) / 2.0)
                .verified(isVerified(s))
                .invitedAt(m.getInvitedAt())
                .respondedAt(m.getRespondedAt())
                .expiresAt(m.getExpiresAt())
                .build();
    }

    private static Double distanceTo(CampaignProposal p, User shop) {
        if (p.getSiteLatitude() == null || p.getSiteLongitude() == null || shop.getShopLatitude() == null || shop.getShopLongitude() == null) {
            return null;
        }
        return haversineKm(p.getSiteLatitude(), p.getSiteLongitude(), shop.getShopLatitude(), shop.getShopLongitude());
    }

    static double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1), dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 6371.0 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    static boolean isVerified(User u) {
        return u != null && u.getRole() != null && "SHOP".equals(u.getRole().getName())
                && u.getStatus() == UserStatus.ACTIVE && !Boolean.FALSE.equals(u.getShopActive());
    }

    private static void requireVerified(User u) {
        if (!isVerified(u)) throw new AppException(ErrorCode.PROPOSAL_SHOP_NOT_VERIFIED);
    }

    private String nextCode() {
        for (int i = 0; i < 20; i++) {
            String code = "DX" + LocalDate.now().toString().replace("-", "").substring(2) + (100000 + random.nextInt(900000));
            if (!proposalRepository.existsByCode(code)) return code;
        }
        throw new AppException(ErrorCode.PROPOSAL_INVALID);
    }

    private User currentShop() {
        User u = currentUser();
        if (u.getRole() == null || !"SHOP".equals(u.getRole().getName())) throw new AppException(ErrorCode.PROPOSAL_SHOP_NOT_VERIFIED);
        return u;
    }

    private User currentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null || !auth.getName().matches("\\d+")) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        return userRepository.findById(Integer.parseInt(auth.getName()))
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    private static String shopName(User u) {
        return u.getShopName() != null && !u.getShopName().isBlank() ? u.getShopName() : u.getFullName();
    }

    private static String trim(String s, int max) {
        if (s == null) return null;
        String t = s.trim();
        if (t.isEmpty()) return null;
        return t.length() > max ? t.substring(0, max) : t;
    }

    private static BigDecimal positiveOrNull(BigDecimal v) {
        return v == null || v.signum() <= 0 ? null : v;
    }

    private static String plain(BigDecimal v) {
        return v == null ? "0" : v.stripTrailingZeros().toPlainString();
    }

    /** Các điểm bài đăng khác đơn đề xuất (giá, ngày mở/kết thúc, điểm tập kết) */
    private List<String> diffWithProposal(CampaignProposal p, CampaignUpsertRequest cr) {
        List<String> out = new ArrayList<>();
        if (p.getDesiredPricePerKg() != null && cr.getRescuePricePerKg() != null
                && p.getDesiredPricePerKg().compareTo(cr.getRescuePricePerKg()) != 0) {
            out.add("giá " + fmtVnd(p.getDesiredPricePerKg()) + " → " + fmtVnd(cr.getRescuePricePerKg()) + "/kg");
        }
        if (p.getProposedOpenDate() != null && cr.getStartDate() != null && !p.getProposedOpenDate().equals(cr.getStartDate())) {
            out.add("ngày bắt đầu " + fmtDate(p.getProposedOpenDate()) + " → " + fmtDate(cr.getStartDate()));
        }
        if (p.getProposedCloseDate() != null && cr.getEndDate() != null && !p.getProposedCloseDate().equals(cr.getEndDate())) {
            out.add("ngày kết thúc " + fmtDate(p.getProposedCloseDate()) + " → " + fmtDate(cr.getEndDate()));
        }
        if (!sameText(p.getSiteName(), cr.getCollectionSiteName())) {
            out.add("điểm tập kết \"" + Objects.toString(p.getSiteName(), "") + "\" → \"" + Objects.toString(cr.getCollectionSiteName(), "") + "\"");
        } else if (!sameText(p.getSiteAddress(), cr.getCollectionSiteAddress())) {
            out.add("địa chỉ điểm tập kết → " + Objects.toString(cr.getCollectionSiteAddress(), ""));
        }
        return out;
    }

    private static boolean sameText(String a, String b) {
        String x = a == null ? "" : a.trim().replaceAll("\\s+", " ");
        String y = b == null ? "" : b.trim().replaceAll("\\s+", " ");
        return x.equalsIgnoreCase(y);
    }

    private static String fmtVnd(BigDecimal v) {
        return String.format("%,.0f", v).replace(',', '.') + "đ";
    }

    private static String fmtDate(LocalDate d) {
        return d.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private static String fmtDateTime(LocalDateTime d) {
        return d.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"));
    }

    private void notify(User user, String title, String message) {
        if (user == null) return;
        try {
            notificationService.sendNotificationToUser(user, title, message, null);
        } catch (Exception e) {
            log.warn("[Proposal] Notify user #{} failed: {}", user.getId(), e.getMessage());
        }
    }
}
