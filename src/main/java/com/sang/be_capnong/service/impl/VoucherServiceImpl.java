package com.sang.be_capnong.service.impl;

import com.sang.be_capnong.dto.request.VoucherCreateRequest;
import com.sang.be_capnong.dto.response.ApiResponse;
import com.sang.be_capnong.dto.response.VoucherResponse;
import com.sang.be_capnong.entity.User;
import com.sang.be_capnong.entity.Voucher;
import com.sang.be_capnong.entity.VoucherUser;
import com.sang.be_capnong.exception.AppException;
import com.sang.be_capnong.exception.ErrorCode;
import com.sang.be_capnong.repository.UserRepository;
import com.sang.be_capnong.repository.VoucherRepository;
import com.sang.be_capnong.repository.VoucherUserRepository;
import com.sang.be_capnong.service.VoucherService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
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

        Voucher voucher = Voucher.builder()
                .shop(currentUser) // Admin hay Shop thì đều gán ID của người tạo vào đây
                .voucherCode(request.getVoucherCode().toUpperCase())
                .voucherType(request.getVoucherType())
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
    public List<VoucherResponse> getManagementVouchers(boolean isAdmin) {
        User currentUser = getCurrentUser();
        List<Voucher> vouchers;
        if (isAdmin) {
            // Lấy tất cả voucher do Admin này tạo
            vouchers = voucherRepository.findByShop_Id(currentUser.getId());
        } else {
            // Lấy tất cả voucher do Shop này tạo
            vouchers = voucherRepository.findByShop_Id(currentUser.getId());
        }
        return vouchers.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional
    @Override
    public VoucherResponse toggleVoucherStatus(int voucherId) {
        Voucher voucher = voucherRepository.findById(voucherId)
                .orElseThrow(() -> new AppException(ErrorCode.VOUCHER_NOT_FOUND));

        User currentUser = getCurrentUser();
        if (voucher.getShop().getId() != currentUser.getId()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED); // Chỉ người tạo mới được sửa
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
                .filter(v -> v.getExpiryDate().isAfter(now))
                .filter(v -> v.getClaimLimit() == null || v.getClaimedCount() < v.getClaimLimit())
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
                .map(vu -> mapToResponse(vu.getVoucher()))
                .collect(Collectors.toList());
    }

    // --- HELPER METHODS ---

    private ApiResponse<Void> doCollectVoucher(User buyer, Voucher voucher) {
        if (!Boolean.TRUE.equals(voucher.getActive()) || voucher.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new AppException(ErrorCode.VOUCHER_INVALID);
        }
        if (voucher.getClaimLimit() != null && voucher.getClaimedCount() >= voucher.getClaimLimit()) {
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
        String role = v.getShop() != null && v.getShop().getRole() != null ? v.getShop().getRole().getName() : "UNKNOWN";
        boolean isPublic = !"HIDDEN".equalsIgnoreCase(v.getAutoCreatedReason());
        
        return VoucherResponse.builder()
                .voucherId(v.getVoucherId())
                .voucherCode(v.getVoucherCode())
                .voucherType(v.getVoucherType())
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
