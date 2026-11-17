package com.sang.be_capnong.controller;

import com.sang.be_capnong.dto.request.campaign.*;
import com.sang.be_capnong.dto.response.ApiResponse;
import com.sang.be_capnong.dto.response.campaign.CampaignResponse;
import com.sang.be_capnong.dto.response.campaign.FarmRegistrationResponse;
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
@RequestMapping("/shop/campaigns")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Campaign - Farmer", description = "Nhà vườn tham gia chiến dịch trong 2 bước (GAP-04)")
public class ShopCampaignController {

    CampaignService campaignService;

    @Operation(summary = "Lời kêu gọi đang mở")
    @GetMapping("/calls")
    @PreAuthorize("hasRole('SHOP')")
    public ApiResponse<List<CampaignResponse>> openCalls() {
        return ApiResponse.<List<CampaignResponse>>builder().result(campaignService.getOpenFarmCalls()).build();
    }

    @Operation(summary = "Tham gia: khai kg dư + ngày thu hoạch")
    @PostMapping("/{id}/register")
    @PreAuthorize("hasRole('SHOP')")
    public ApiResponse<FarmRegistrationResponse> register(@PathVariable int id, @RequestBody FarmRegistrationRequest request) {
        return ApiResponse.<FarmRegistrationResponse>builder().result(campaignService.registerFarm(id, request)).build();
    }

    @Operation(summary = "Đăng ký của tôi")
    @GetMapping("/my")
    @PreAuthorize("hasRole('SHOP')")
    public ApiResponse<List<FarmRegistrationResponse>> myRegistrations() {
        return ApiResponse.<List<FarmRegistrationResponse>>builder().result(campaignService.getMyRegistrations()).build();
    }
}
