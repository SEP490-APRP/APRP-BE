package com.sang.be_capnong.controller;

import com.sang.be_capnong.dto.request.proposal.CampaignProposalRequest;
import com.sang.be_capnong.dto.request.proposal.ProposalInviteRequest;
import com.sang.be_capnong.dto.request.proposal.ProposalReplyRequest;
import com.sang.be_capnong.dto.response.ApiResponse;
import com.sang.be_capnong.dto.response.proposal.CampaignProposalResponse;
import com.sang.be_capnong.dto.response.proposal.NearbyShopResponse;
import com.sang.be_capnong.service.campaign.CampaignProposalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/shop/campaign-proposals")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@PreAuthorize("hasRole('SHOP')")
@Tag(name = "Campaign proposal - Shop", description = "Shop đề xuất mở chiến dịch, mời shop lân cận, gửi admin duyệt")
public class ShopCampaignProposalController {

    CampaignProposalService service;

    @Operation(summary = "Tạo đơn đề xuất (nháp)")
    @PostMapping
    public ApiResponse<CampaignProposalResponse> create(@RequestBody CampaignProposalRequest request) {
        return ApiResponse.<CampaignProposalResponse>builder().result(service.create(request)).build();
    }

    @Operation(summary = "Sửa đơn nháp / đơn bị trả về")
    @PutMapping("/{id}")
    public ApiResponse<CampaignProposalResponse> update(@PathVariable int id, @RequestBody CampaignProposalRequest request) {
        return ApiResponse.<CampaignProposalResponse>builder().result(service.update(id, request)).build();
    }

    @GetMapping("/{id}")
    public ApiResponse<CampaignProposalResponse> get(@PathVariable int id) {
        return ApiResponse.<CampaignProposalResponse>builder().result(service.get(id)).build();
    }

    @Operation(summary = "Đơn tôi khởi xướng")
    @GetMapping("/my")
    public ApiResponse<List<CampaignProposalResponse>> my() {
        return ApiResponse.<List<CampaignProposalResponse>>builder().result(service.my()).build();
    }

    @Operation(summary = "Lời mời tôi nhận được")
    @GetMapping("/invites")
    public ApiResponse<List<CampaignProposalResponse>> invites() {
        return ApiResponse.<List<CampaignProposalResponse>>builder().result(service.myInvites()).build();
    }

    @Operation(summary = "Shop đã xác minh gần điểm tập kết")
    @GetMapping("/{id}/nearby-shops")
    public ApiResponse<List<NearbyShopResponse>> nearby(@PathVariable int id,
                                                        @RequestParam(required = false) Double radiusKm,
                                                        @RequestParam(defaultValue = "false") boolean sameCategory) {
        return ApiResponse.<List<NearbyShopResponse>>builder().result(service.nearbyShops(id, radiusKm, sameCategory)).build();
    }

    @Operation(summary = "Mời shop lân cận")
    @PostMapping("/{id}/invites")
    public ApiResponse<CampaignProposalResponse> invite(@PathVariable int id, @RequestBody ProposalInviteRequest request) {
        return ApiResponse.<CampaignProposalResponse>builder().result(service.invite(id, request)).build();
    }

    @Operation(summary = "Bỏ shop khỏi đơn")
    @DeleteMapping("/{id}/members/{shopId}")
    public ApiResponse<CampaignProposalResponse> removeMember(@PathVariable int id, @PathVariable int shopId) {
        return ApiResponse.<CampaignProposalResponse>builder().result(service.removeMember(id, shopId)).build();
    }

    @Operation(summary = "Gửi admin duyệt")
    @PostMapping("/{id}/submit")
    public ApiResponse<CampaignProposalResponse> submit(@PathVariable int id) {
        return ApiResponse.<CampaignProposalResponse>builder().result(service.submit(id)).build();
    }

    @Operation(summary = "Rút đơn")
    @PostMapping("/{id}/withdraw")
    public ApiResponse<CampaignProposalResponse> withdraw(@PathVariable int id) {
        return ApiResponse.<CampaignProposalResponse>builder().result(service.withdraw(id)).build();
    }

    @Operation(summary = "Nhận lời mời, khai kg dự kiến")
    @PostMapping("/invites/{memberId}/accept")
    public ApiResponse<CampaignProposalResponse> accept(@PathVariable int memberId, @RequestBody ProposalReplyRequest request) {
        return ApiResponse.<CampaignProposalResponse>builder().result(service.acceptInvite(memberId, request)).build();
    }

    @Operation(summary = "Từ chối lời mời")
    @PostMapping("/invites/{memberId}/decline")
    public ApiResponse<CampaignProposalResponse> decline(@PathVariable int memberId) {
        return ApiResponse.<CampaignProposalResponse>builder().result(service.declineInvite(memberId)).build();
    }
}
