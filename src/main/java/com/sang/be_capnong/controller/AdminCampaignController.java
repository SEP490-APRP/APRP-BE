package com.sang.be_capnong.controller;

import com.sang.be_capnong.dto.request.campaign.*;
import com.sang.be_capnong.dto.response.ApiResponse;
import com.sang.be_capnong.dto.response.PageResponse;
import com.sang.be_capnong.dto.response.campaign.CampaignResponse;
import com.sang.be_capnong.dto.response.campaign.FarmRegistrationResponse;
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
@RequestMapping("/admin/campaigns")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Campaign - Admin", description = "Admin tạo, duyệt nhà vườn, mở bán, gom hàng và hoàn tất chiến dịch giải cứu (WF-01)")
public class AdminCampaignController {

    CampaignService campaignService;
    com.sang.be_capnong.service.campaign.DamageClaimService damageClaimService;

    @Operation(summary = "Tạo chiến dịch (DRAFT)")
    @PostMapping("")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CampaignResponse> create(@RequestBody CampaignUpsertRequest request) {
        return ApiResponse.<CampaignResponse>builder().result(campaignService.createCampaign(request)).build();
    }

    @Operation(summary = "Sửa chiến dịch khi còn DRAFT")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CampaignResponse> update(@PathVariable int id, @RequestBody CampaignUpsertRequest request) {
        return ApiResponse.<CampaignResponse>builder().result(campaignService.updateCampaign(id, request)).build();
    }

    @Operation(summary = "Tuỳ chỉnh banner trang chủ của chiến dịch")
    @PutMapping("/{id}/banner")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CampaignResponse> updateBanner(@PathVariable int id, @RequestBody CampaignBannerRequest request) {
        return ApiResponse.<CampaignResponse>builder().result(campaignService.updateBanner(id, request)).build();
    }

    @Operation(summary = "Danh sách chiến dịch (lọc theo status)")
    @GetMapping("")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageResponse<CampaignResponse>> list(@RequestParam(required = false) String status, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.<PageResponse<CampaignResponse>>builder().result(campaignService.getAdminCampaigns(status, page, size)).build();
    }

    @Operation(summary = "Chi tiết chiến dịch")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CampaignResponse> detail(@PathVariable int id) {
        return ApiResponse.<CampaignResponse>builder().result(campaignService.getAdminCampaign(id)).build();
    }

    @Operation(summary = "Đăng lời kêu gọi nhà vườn tham gia")
    @PostMapping("/{id}/call")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CampaignResponse> postCall(@PathVariable int id) {
        return ApiResponse.<CampaignResponse>builder().result(campaignService.postFarmCall(id)).build();
    }

    @Operation(summary = "Danh sách nhà vườn đăng ký")
    @GetMapping("/{id}/registrations")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<FarmRegistrationResponse>> registrations(@PathVariable int id) {
        return ApiResponse.<List<FarmRegistrationResponse>>builder().result(campaignService.getRegistrations(id)).build();
    }

    @Operation(summary = "Duyệt đăng ký của nhà vườn")
    @PostMapping("/registrations/{registrationId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<FarmRegistrationResponse> approveRegistration(@PathVariable int registrationId, @RequestBody(required = false) CampaignNoteRequest request) {
        return ApiResponse.<FarmRegistrationResponse>builder().result(campaignService.approveRegistration(registrationId, request)).build();
    }

    @Operation(summary = "Từ chối đăng ký của nhà vườn")
    @PostMapping("/registrations/{registrationId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<FarmRegistrationResponse> rejectRegistration(@PathVariable int registrationId, @RequestBody(required = false) CampaignNoteRequest request) {
        return ApiResponse.<FarmRegistrationResponse>builder().result(campaignService.rejectRegistration(registrationId, request)).build();
    }

    @Operation(summary = "Mở bán cho người mua (DRAFT -> ACTIVE)")
    @PostMapping("/{id}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CampaignResponse> publish(@PathVariable int id) {
        return ApiResponse.<CampaignResponse>builder().result(campaignService.publishCampaign(id)).build();
    }

    @Operation(summary = "Huỷ chiến dịch và hoàn cọc")
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CampaignResponse> cancel(@PathVariable int id, @RequestBody(required = false) CampaignNoteRequest request) {
        return ApiResponse.<CampaignResponse>builder().result(campaignService.cancelCampaign(id, request)).build();
    }

    @Operation(summary = "Bắt đầu gom hàng (THRESHOLD_MET -> FULFILLING)")
    @PostMapping("/{id}/start-fulfilling")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CampaignResponse> startFulfilling(@PathVariable int id) {
        return ApiResponse.<CampaignResponse>builder().result(campaignService.startFulfilling(id)).build();
    }

    @Operation(summary = "Nhập cân / kiểm tại điểm tập kết")
    @PostMapping("/registrations/{registrationId}/intake")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<FarmRegistrationResponse> intake(@PathVariable int registrationId, @RequestBody CampaignIntakeRequest request) {
        return ApiResponse.<FarmRegistrationResponse>builder().result(campaignService.recordIntake(registrationId, request)).build();
    }

    @Operation(summary = "Đơn đặt trước của chiến dịch")
    @GetMapping("/{id}/pre-orders")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<PreOrderResponse>> preOrders(@PathVariable int id) {
        return ApiResponse.<List<PreOrderResponse>>builder().result(campaignService.getCampaignPreOrders(id)).build();
    }

    @Operation(summary = "Kêu gọi nhà vườn bổ sung hàng khi mọi nhà vườn đã giao mà vẫn thiếu")
    @PostMapping("/{id}/top-up-call")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CampaignResponse> openTopUp(@PathVariable int id) {
        return ApiResponse.<CampaignResponse>builder().result(campaignService.openTopUpCall(id)).build();
    }

    @Operation(summary = "Đóng kêu gọi bổ sung")
    @PostMapping("/{id}/top-up-call/close")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CampaignResponse> closeTopUp(@PathVariable int id) {
        return ApiResponse.<CampaignResponse>builder().result(campaignService.closeTopUpCall(id)).build();
    }

    @Operation(summary = "Danh sách báo hàng hư của chiến dịch")
    @GetMapping("/{id}/damage-claims")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<com.sang.be_capnong.dto.response.campaign.DamageClaimResponse>> damageClaims(@PathVariable int id) {
        return ApiResponse.<List<com.sang.be_capnong.dto.response.campaign.DamageClaimResponse>>builder().result(damageClaimService.campaignClaims(id)).build();
    }

    @Operation(summary = "Duyệt báo hư: hoàn tiền phần hư từ ví sàn vào ví người mua")
    @PostMapping("/damage-claims/{claimId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<com.sang.be_capnong.dto.response.campaign.DamageClaimResponse> approveDamage(
            @PathVariable int claimId, @RequestBody(required = false) com.sang.be_capnong.dto.request.campaign.DamageClaimReviewRequest request) {
        return ApiResponse.<com.sang.be_capnong.dto.response.campaign.DamageClaimResponse>builder().result(damageClaimService.approve(claimId, request)).build();
    }

    @Operation(summary = "Từ chối báo hư")
    @PostMapping("/damage-claims/{claimId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<com.sang.be_capnong.dto.response.campaign.DamageClaimResponse> rejectDamage(
            @PathVariable int claimId, @RequestBody com.sang.be_capnong.dto.request.campaign.DamageClaimReviewRequest request) {
        return ApiResponse.<com.sang.be_capnong.dto.response.campaign.DamageClaimResponse>builder().result(damageClaimService.reject(claimId, request)).build();
    }

    @Operation(summary = "Xử lý thiếu hàng: giữ hàng cho đơn cọc trước, giảm thùng hoặc huỷ đơn sau và hoàn cọc")
    @PostMapping("/{id}/resolve-shortfall")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CampaignResponse> resolveShortfall(@PathVariable int id) {
        return ApiResponse.<CampaignResponse>builder().result(campaignService.resolveShortfall(id)).build();
    }

    @Operation(summary = "Đóng gói, dán QR, gán khu giao (DEPOSIT_PAID -> PROCESSING)")
    @PostMapping("/pre-orders/{preOrderId}/pack")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PreOrderResponse> pack(@PathVariable int preOrderId, @RequestBody(required = false) PreOrderPackRequest request) {
        return ApiResponse.<PreOrderResponse>builder().result(campaignService.packPreOrder(preOrderId, request)).build();
    }

    @Operation(summary = "Khách tự đến điểm tập kết đã nhận hàng (PICKUP): PROCESSING -> DELIVERED")
    @PostMapping("/pre-orders/{preOrderId}/picked-up")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PreOrderResponse> pickedUp(@PathVariable int preOrderId) {
        return ApiResponse.<PreOrderResponse>builder().result(campaignService.confirmPickup(preOrderId)).build();
    }

    @Operation(summary = "Nhắc khách tự đến điểm tập kết nhận đơn đã đóng gói")
    @PostMapping("/pre-orders/{preOrderId}/remind-pickup")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PreOrderResponse> remindPickup(@PathVariable int preOrderId) {
        return ApiResponse.<PreOrderResponse>builder().result(campaignService.remindPickup(preOrderId)).build();
    }

    @Operation(summary = "Đã nhắc mà khách không tới nhận: đóng đơn, cọc không hoàn")
    @PostMapping("/pre-orders/{preOrderId}/pickup-no-show")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PreOrderResponse> pickupNoShow(@PathVariable int preOrderId, @RequestBody(required = false) CampaignNoteRequest request) {
        return ApiResponse.<PreOrderResponse>builder().result(campaignService.markPickupNoShow(preOrderId, request)).build();
    }

    @Operation(summary = "Bảng chia đơn giao theo phường, xếp theo độ gấp")
    @GetMapping("/{id}/dispatch-board")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<com.sang.be_capnong.dto.response.campaign.DispatchBoardResponse> dispatchBoard(@PathVariable int id) {
        return ApiResponse.<com.sang.be_capnong.dto.response.campaign.DispatchBoardResponse>builder()
                .result(campaignService.getDispatchBoard(id)).build();
    }

    @Operation(summary = "Chia đơn: đưa đơn đã đóng gói ra hub, giao cho shipper (tuỳ chọn)")
    @PostMapping("/{id}/dispatch")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<com.sang.be_capnong.dto.response.campaign.DispatchBoardResponse> dispatch(
            @PathVariable int id, @RequestBody com.sang.be_capnong.dto.request.campaign.PreOrderDispatchRequest request) {
        return ApiResponse.<com.sang.be_capnong.dto.response.campaign.DispatchBoardResponse>builder()
                .result(campaignService.dispatchPreOrders(id, request)).build();
    }

    @Operation(summary = "Giao lại đơn thất bại: FAILED -> PROCESSING (chờ chia lại)")
    @PostMapping("/pre-orders/{preOrderId}/retry")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PreOrderResponse> retry(@PathVariable int preOrderId) {
        return ApiResponse.<PreOrderResponse>builder().result(campaignService.retryPreOrder(preOrderId)).build();
    }

    @Operation(summary = "Xe tới hub: PROCESSING -> READY")
    @PostMapping("/{id}/dispatch-to-hub")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CampaignResponse> dispatchToHub(@PathVariable int id) {
        return ApiResponse.<CampaignResponse>builder().result(campaignService.dispatchToHub(id)).build();
    }

    @Operation(summary = "Hoàn tất: giải ngân cọc, chi trả nhà vườn")
    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<CampaignResponse> complete(@PathVariable int id) {
        return ApiResponse.<CampaignResponse>builder().result(campaignService.completeCampaign(id)).build();
    }
}
