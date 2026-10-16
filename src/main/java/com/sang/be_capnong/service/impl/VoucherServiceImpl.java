package com.sang.be_capnong.service.impl;

import com.sang.be_capnong.dto.request.VoucherCreateRequest;
import com.sang.be_capnong.dto.response.ApiResponse;
import com.sang.be_capnong.dto.response.PageResponse;
import com.sang.be_capnong.dto.response.VoucherResponse;
import com.sang.be_capnong.entity.User;
import com.sang.be_capnong.entity.Voucher;
import com.sang.be_capnong.entity.VoucherUser;
import com.sang.be_capnong.enums.VoucherScope;
import com.sang.be_capnong.enums.VoucherTarget;
import com.sang.be_capnong.enums.VoucherType;
import com.sang.be_capnong.exception.AppException;
import com.sang.be_capnong.exception.ErrorCode;
import com.sang.be_capnong.repository.UserRepository;
import com.sang.be_capnong.repository.VoucherRepository;
import com.sang.be_capnong.repository.VoucherUserRepository;
import com.sang.be_capnong.service.VoucherService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VoucherServiceImpl implements VoucherService {

    VoucherRepository voucherRepository;
    VoucherUserRepository voucherUserRepository;
    UserRepository userRepository;

    @Transactional
    @Override
    public VoucherResponse createVoucher(VoucherCreateRequest request, boolean isAdmin) {
        User currentUser = getCurrentUser();

        // Kiểm tra xem mã voucher đã tồn tại chưa
        if (voucherRepository.findByVoucherCode(request.getVoucherCode()).isPresent()) {
            throw new AppException(ErrorCode.VOUCHER_CODE_EXISTED);
        }

        if (request.getVoucherType() == VoucherType.PERCENTAGE && request.getDiscountValue() != null && request.getDiscountValue().compareTo(java.math.BigDecimal.valueOf(100)) > 0) {
            throw new AppException(ErrorCode.DISCOUNT_PERCENT_INVALID);
        }

        VoucherScope scope = request.getVoucherScope();
        if (!isAdmin) {
            scope = VoucherScope.SHOP;
        } else if (scope == null) {
            scope = VoucherScope.PLATFORM;
        }

        Voucher voucher = Voucher.builder()
                .shop(currentUser) // Admin hay Shop thì đều gán ID của người tạo vào đây
                .voucherCode(request.getVoucherCode().toUpperCase())
                .voucherType(request.getVoucherType())
                .voucherScope(scope)
                .voucherTarget(request.getVoucherTarget())
                .discountValue(request.getDiscountValue())
                .maxDiscount(request.getMaxDiscount())
                .minOrderValue(request.getMinOrderValue())
                .claimLimit(request.getClaimLimit())
                .claimedCount(0)
                .usedCount(0)
                .perUserLimit(request.getPerUserLimit())
                .expiryDate(request.getExpiryDate())
                .createAt(LocalDateTime.now())
                .active(true)
                // Sử dụng autoCreatedReason để lưu PUBLIC hoặc HIDDEN
                .autoCreatedReason(Boolean.TRUE.equals(request.getIsPublic()) ? null : "HIDDEN")
                .build();

        voucher = voucherRepository.save(voucher);
        return mapToResponse(voucher);
    }

    @Override
    public PageResponse<VoucherResponse> getManagementVouchers(boolean isAdmin, int page, int size) {
        User currentUser = getCurrentUser();
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(Sort.Direction.DESC, "createAt", "voucherId"));
        Page<Voucher> voucherPage;
        if (isAdmin) {
            // Lấy tất cả voucher do Admin tạo hoặc toàn sàn
            List<VoucherScope> adminScopes = List.of(VoucherScope.PLATFORM, VoucherScope.SYSTEM, VoucherScope.WELCOME);
            voucherPage = voucherRepository.findAllAdminVouchers(adminScopes, pageable);
        } else {
            // Lấy tất cả voucher do Shop này tạo
            voucherPage = voucherRepository.findByShop_Id(currentUser.getId(), pageable);
        }
        List<VoucherResponse> content = voucherPage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return PageResponse.from(voucherPage, content);
    }

    @Transactional
    @Override
    public VoucherResponse toggleVoucherStatus(int voucherId) {
        Voucher voucher = voucherRepository.findById(voucherId)
                .orElseThrow(() -> new AppException(ErrorCode.VOUCHER_NOT_FOUND));

        User currentUser = getCurrentUser();
        boolean isCurrentUserAdmin = currentUser.getRole() != null && "ADMIN".equalsIgnoreCase(currentUser.getRole().getName());
        if (!isCurrentUserAdmin) {
            if (voucher.getShop() == null || !Objects.equals(voucher.getShop().getId(), currentUser.getId())) {
                throw new AppException(ErrorCode.UNAUTHORIZED);
            }
        }

        voucher.setActive(!Boolean.TRUE.equals(voucher.getActive()));
        return mapToResponse(voucherRepository.save(voucher));
    }

    @Override
    public List<VoucherResponse> getAvailablePublicVouchers() {
        // Lấy các mã active, chưa hết hạn, và không có autoCreatedReason = "HIDDEN"
        LocalDateTime now = LocalDateTime.now();
        List<Voucher> vouchers = voucherRepository.findByActiveTrueAndAutoCreatedReasonIsNull();
        
        return vouchers.stream()
                .filter(v -> v.getExpiryDate() == null || v.getExpiryDate().isAfter(now))
                .filter(v -> v.getClaimLimit() == null || (v.getClaimedCount() != null ? v.getClaimedCount() : 0) < v.getClaimLimit())
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    @Override
    public ApiResponse<Void> collectVoucher(int voucherId) {
        User buyer = getCurrentUser();
        Voucher voucher = voucherRepository.findById(voucherId)
                .orElseThrow(() -> new AppException(ErrorCode.VOUCHER_NOT_FOUND));

        return doCollectVoucher(buyer, voucher);
    }

    @Transactional
    @Override
    public ApiResponse<Void> collectVoucherByCode(String voucherCode) {
        User buyer = getCurrentUser();
        Voucher voucher = voucherRepository.findByVoucherCode(voucherCode.toUpperCase())
                .orElseThrow(() -> new AppException(ErrorCode.VOUCHER_NOT_FOUND));

        return doCollectVoucher(buyer, voucher);
    }

    @Override
    public List<VoucherResponse> getMyVouchers() {
        User buyer = getCurrentUser();
        List<VoucherUser> myVouchers = voucherUserRepository.findByUser_Id(buyer.getId());
        
        return myVouchers.stream()
                .filter(vu -> {
                    int qty = vu.getQuantity() == null ? 0 : vu.getQuantity();
                    int used = vu.getQuantityUsed() == null ? 0 : vu.getQuantityUsed();
                    return qty > used;
                })
                .map(vu -> mapToResponse(vu.getVoucher()))
                .filter(resp -> resp != null && Boolean.TRUE.equals(resp.getActive()) && (resp.getExpiryDate() == null || resp.getExpiryDate().isAfter(LocalDateTime.now())))
                .collect(Collectors.toList());
    }

    // --- HELPER METHODS ---

    private ApiResponse<Void> doCollectVoucher(User buyer, Voucher voucher) {
        if (!Boolean.TRUE.equals(voucher.getActive()) || (voucher.getExpiryDate() != null && voucher.getExpiryDate().isBefore(LocalDateTime.now()))) {
            throw new AppException(ErrorCode.VOUCHER_INVALID);
        }
        if (voucher.getClaimLimit() != null && (voucher.getClaimedCount() != null ? voucher.getClaimedCount() : 0) >= voucher.getClaimLimit()) {
            throw new AppException(ErrorCode.VOUCHER_OUT_OF_STOCK);
        }

        Optional<VoucherUser> existingVuOpt = voucherUserRepository.findByUser_IdAndVoucher_VoucherId(buyer.getId(), voucher.getVoucherId());
        if (existingVuOpt.isPresent()) {
            VoucherUser existingVu = existingVuOpt.get();
            if (voucher.getPerUserLimit() != null && existingVu.getQuantity() >= voucher.getPerUserLimit()) {
                throw new AppException(ErrorCode.VOUCHER_LIMIT_REACHED);
            }
            existingVu.setQuantity(existingVu.getQuantity() + 1);
            voucherUserRepository.save(existingVu);
        } else {
            VoucherUser newVu = VoucherUser.builder()
                    .user(buyer)
                    .voucher(voucher)
                    .quantity(1)
                    .quantityUsed(0)
                    .claimAt(LocalDateTime.now())
                    .build();
            voucherUserRepository.save(newVu);
        }

        voucher.setClaimedCount((voucher.getClaimedCount() == null ? 0 : voucher.getClaimedCount()) + 1);
        voucherRepository.save(voucher);

        return ApiResponse.<Void>builder().message("Lưu voucher thành công").build();
    }

    private VoucherResponse mapToResponse(Voucher v) {
        String role = v.getShop() != null && v.getShop().getRole() != null ? v.getShop().getRole().getName() : "SHOP";
        boolean isCreatorAdmin = "ADMIN".equalsIgnoreCase(role);
        boolean isPublic = !"HIDDEN".equalsIgnoreCase(v.getAutoCreatedReason());
        
        VoucherScope scope = v.getVoucherScope();
        if (!isCreatorAdmin) {
            scope = VoucherScope.SHOP;
        } else if (scope == null) {
            scope = VoucherScope.PLATFORM;
        }

        VoucherTarget target = v.getVoucherTarget();
        if (target == null) {
            boolean isShipping = v.getVoucherType() == VoucherType.FREE_SHIPPING
                    || (v.getVoucherCode() != null && v.getVoucherCode().toUpperCase().contains("FREESHIP"));
            target = isShipping ? VoucherTarget.SHIPPING : VoucherTarget.ORDER;
        }

        return VoucherResponse.builder()
                .voucherId(v.getVoucherId())
                .voucherCode(v.getVoucherCode())
                .voucherType(v.getVoucherType())
                .voucherScope(scope)
                .voucherTarget(target)
                .discountValue(v.getDiscountValue())
                .maxDiscount(v.getMaxDiscount())
                .minOrderValue(v.getMinOrderValue())
                .claimLimit(v.getClaimLimit())
                .claimedCount(v.getClaimedCount())
                .usedCount(v.getUsedCount())
                .perUserLimit(v.getPerUserLimit())
                .active(v.getActive())
                .expiryDate(v.getExpiryDate())
                .createAt(v.getCreateAt())
                .creatorRole(role)
                .shopId(v.getShop() != null ? v.getShop().getId() : null)
                .isPublic(isPublic)
                .build();
    }

    private User getCurrentUser() {
        String subject = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            int userId = Integer.parseInt(subject);
            return userRepository.findById(userId)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        } catch (NumberFormatException e) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
    }
}
