package com.sang.be_capnong.controller;

import com.sang.be_capnong.dto.request.campaign.*;
import com.sang.be_capnong.dto.response.ApiResponse;
import com.sang.be_capnong.dto.response.PageResponse;
import com.sang.be_capnong.dto.response.campaign.CampaignResponse;
import com.sang.be_capnong.dto.response.campaign.PreOrderResponse;
import com.sang.be_capnong.service.CampaignService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Campaign - Buyer", description = "Xem chiến dịch đang mở bán, đặt trước thùng 10/20/50 kg và cọc PayOS")
public class CampaignController {

    CampaignService campaignService;
    com.sang.be_capnong.service.campaign.DamageClaimService damageClaimService;

    @Operation(summary = "Danh sách chiến dịch công khai theo giai đoạn: UPCOMING | ONGOING (mặc định) | DELIVERING | DONE")
    @GetMapping("/campaigns")
    public ApiResponse<PageResponse<CampaignResponse>> publicCampaigns(
            @RequestParam(defaultValue = "ONGOING") String phase,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.<PageResponse<CampaignResponse>>builder().result(campaignService.getPublicCampaigns(phase, page, size)).build();
    }

    @Operation(summary = "Banner chiến dịch ở đầu trang chủ (công khai): số giây tự chuyển + chiến dịch theo thứ tự")
    /** Công khai: ai quét tem QR trên thùng cũng xem được */
    @GetMapping("/trace/{token}")
    public ApiResponse<com.sang.be_capnong.dto.response.campaign.PackageTraceResponse> tracePackage(@PathVariable String token) {
        return ApiResponse.<com.sang.be_capnong.dto.response.campaign.PackageTraceResponse>builder()
                .result(campaignService.tracePackage(token)).build();
    }

    @GetMapping("/campaigns/home-banners")
    public ApiResponse<com.sang.be_capnong.dto.response.campaign.HomeBannerConfigResponse> homeBanners() {
        return ApiResponse.<com.sang.be_capnong.dto.response.campaign.HomeBannerConfigResponse>builder()
                .result(campaignService.getHomeBanners(true)).build();
    }

    @Operation(summary = "Chi tiết chiến dịch (công khai)")
    @GetMapping("/campaigns/{id}")
    public ApiResponse<CampaignResponse> campaign(@PathVariable int id) {
        return ApiResponse.<CampaignResponse>builder().result(campaignService.getPublicCampaign(id)).build();
    }

    @Operation(summary = "Báo giá phí giao hàng (công khai): method = DELIVERY | PICKUP")
    @GetMapping("/campaigns/{id}/shipping-quote")
    public ApiResponse<com.sang.be_capnong.dto.response.campaign.CampaignShippingQuoteResponse> shippingQuote(
            @PathVariable int id,
            @RequestParam(defaultValue = "0") java.math.BigDecimal totalKg,
            @RequestParam(defaultValue = "DELIVERY") String method,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) String address) {
        return ApiResponse.<com.sang.be_capnong.dto.response.campaign.CampaignShippingQuoteResponse>builder()
                .result(campaignService.quoteShipping(id, totalKg, method, lat, lng, address)).build();
    }

    @Operation(summary = "Đặt trước + tạo link cọc PayOS")
    @PostMapping("/campaigns/{id}/pre-orders")
    @PreAuthorize("hasRole('BUYER')")
    public ApiResponse<PreOrderResponse> createPreOrder(@PathVariable int id, @RequestBody PreOrderCreateRequest request) {
        return ApiResponse.<PreOrderResponse>builder().result(campaignService.createPreOrder(id, request)).build();
    }

    @Operation(summary = "Báo hàng hư (cân + ảnh), không cần hoàn hàng")
    @PostMapping("/pre-orders/{id}/damage-claims")
    @PreAuthorize("hasRole('BUYER')")
    public ApiResponse<com.sang.be_capnong.dto.response.campaign.DamageClaimResponse> reportDamage(
            @PathVariable int id, @RequestBody com.sang.be_capnong.dto.request.campaign.DamageClaimRequest request) {
        return ApiResponse.<com.sang.be_capnong.dto.response.campaign.DamageClaimResponse>builder().result(damageClaimService.submit(id, request)).build();
    }

    @Operation(summary = "Các lần báo hư của đơn")
    @GetMapping("/pre-orders/{id}/damage-claims")
    @PreAuthorize("hasRole('BUYER')")
    public ApiResponse<List<com.sang.be_capnong.dto.response.campaign.DamageClaimResponse>> damageClaims(@PathVariable int id) {
        return ApiResponse.<List<com.sang.be_capnong.dto.response.campaign.DamageClaimResponse>>builder().result(damageClaimService.myClaims(id)).build();
    }

    @Operation(summary = "Đơn đặt trước của tôi")
    @GetMapping("/pre-orders/my")
    @PreAuthorize("hasRole('BUYER')")
    public ApiResponse<PageResponse<PreOrderResponse>> myPreOrders(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.<PageResponse<PreOrderResponse>>builder().result(campaignService.getMyPreOrders(page, size)).build();
    }

    @Operation(summary = "Chi tiết đơn đặt trước")
    @GetMapping("/pre-orders/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<PreOrderResponse> preOrder(@PathVariable int id) {
        return ApiResponse.<PreOrderResponse>builder().result(campaignService.getPreOrder(id)).build();
    }

    @Operation(summary = "Kiểm tra lại trạng thái cọc với PayOS")
    @PostMapping("/pre-orders/{id}/confirm-payment")
    @PreAuthorize("hasRole('BUYER')")
    public ApiResponse<PreOrderResponse> confirmPayment(@PathVariable int id) {
        return ApiResponse.<PreOrderResponse>builder().result(campaignService.confirmDepositPayment(id)).build();
    }

    @Operation(summary = "Huỷ đơn khi chưa cọc")
    @PostMapping("/pre-orders/{id}/cancel")
    @PreAuthorize("hasRole('BUYER')")
    public ApiResponse<PreOrderResponse> cancelPreOrder(@PathVariable int id) {
        return ApiResponse.<PreOrderResponse>builder().result(campaignService.cancelPreOrder(id)).build();
    }
}
