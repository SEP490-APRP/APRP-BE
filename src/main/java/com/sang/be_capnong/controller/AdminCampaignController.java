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
