package com.sang.be_capnong.controller;

import com.sang.be_capnong.dto.request.referral.ReferralClaimRequest;
import com.sang.be_capnong.dto.request.referral.ReferralClickTrackRequest;
import com.sang.be_capnong.dto.request.referral.ReferralLinkCreateRequest;
import com.sang.be_capnong.dto.response.ApiResponse;
import com.sang.be_capnong.dto.response.PageResponse;
import com.sang.be_capnong.dto.response.referral.ReferralCommissionResponse;
import com.sang.be_capnong.dto.response.referral.ReferralLinkResponse;
import com.sang.be_capnong.dto.response.referral.ReferralResolveResponse;
import com.sang.be_capnong.dto.response.referral.ReferralStatsResponse;
import com.sang.be_capnong.service.ReferralService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/referrals")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Referrals", description = "Quản lý Tiếp thị liên kết (Referral / Affiliate)")
public class ReferralController {

    ReferralService referralService;

    @PostMapping("/links")
    @PreAuthorize("hasRole('BUYER')")
    @Operation(summary = "Tạo mới hoặc lấy liên kết giới thiệu sản phẩm / gian hàng")
    public ApiResponse<ReferralLinkResponse> getOrCreateLink(@RequestBody @Valid ReferralLinkCreateRequest request) {
        return ApiResponse.<ReferralLinkResponse>builder()
                .result(referralService.getOrCreateLink(request))
                .build();
    }

    @GetMapping("/links/my")
    @PreAuthorize("hasRole('BUYER')")
    @Operation(summary = "Lấy danh sách các liên kết giới thiệu đã tạo của người mua (0-based page)")
    public ApiResponse<PageResponse<ReferralLinkResponse>> getMyLinks(
            @PageableDefault(page = 0, size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.<PageResponse<ReferralLinkResponse>>builder()
                .result(referralService.getMyLinks(pageable))
                .build();
    }

    @GetMapping("/resolve/{code}")
    @Operation(summary = "Phân giải mã referral: chỉ đọc metadata, ẩn danh người giới thiệu (Public)")
    public ApiResponse<ReferralResolveResponse> resolveLink(@PathVariable String code) {
        return ApiResponse.<ReferralResolveResponse>builder()
                .result(referralService.resolveLink(code))
                .build();
    }

    @PostMapping("/clicks")
    @Operation(summary = "Ghi nhận lượt nhấp thật từ client kèm IP và User-Agent (Public)")
    public ApiResponse<Void> trackClick(
            @RequestBody @Valid ReferralClickTrackRequest request,
            HttpServletRequest httpServletRequest) {
        String ip = extractClientIp(httpServletRequest);
        String userAgent = httpServletRequest.getHeader("User-Agent");
        referralService.trackClick(request, ip, userAgent);
        return ApiResponse.<Void>builder()
                .message("Đã ghi nhận click thành công")
                .build();
    }

    @PostMapping("/claim")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Gắn mã referral lưu tạm của khách sau khi đăng nhập / đăng ký")
    public ApiResponse<Void> claimReferral(@RequestBody @Valid ReferralClaimRequest request) {
        referralService.claimReferral(request);
        return ApiResponse.<Void>builder()
                .message("Đã ghi nhận liên kết giới thiệu thành công")
                .build();
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('BUYER')")
    @Operation(summary = "Xem tổng quan thống kê hoa hồng, số click, số đơn của người giới thiệu")
    public ApiResponse<ReferralStatsResponse> getDashboard() {
        return ApiResponse.<ReferralStatsResponse>builder()
                .result(referralService.getDashboard())
                .build();
    }

    @GetMapping("/commissions")
    @PreAuthorize("hasRole('BUYER')")
    @Operation(summary = "Lịch sử hoa hồng chi tiết theo từng đơn hàng / món hàng (0-based page)")
    public ApiResponse<PageResponse<ReferralCommissionResponse>> getMyCommissions(
            @PageableDefault(page = 0, size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.<PageResponse<ReferralCommissionResponse>>builder()
                .result(referralService.getMyCommissions(pageable))
                .build();
    }

    private String extractClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
