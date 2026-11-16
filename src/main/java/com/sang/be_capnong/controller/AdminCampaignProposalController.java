package com.sang.be_capnong.controller;

import com.sang.be_capnong.dto.request.proposal.ProposalApproveRequest;
import com.sang.be_capnong.dto.request.proposal.ProposalReplyRequest;
import com.sang.be_capnong.dto.response.ApiResponse;
import com.sang.be_capnong.dto.response.campaign.CampaignResponse;
import com.sang.be_capnong.dto.response.proposal.CampaignProposalResponse;
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
@RequestMapping("/admin/campaign-proposals")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Campaign proposal - Admin", description = "Duyệt / gộp đơn đề xuất chiến dịch từ shop")
public class AdminCampaignProposalController {

    CampaignProposalService service;

    @Operation(summary = "Danh sách đơn đề xuất (không gồm nháp)")
    @GetMapping
    public ApiResponse<List<CampaignProposalResponse>> list(@RequestParam(required = false) String status) {
        return ApiResponse.<List<CampaignProposalResponse>>builder().result(service.adminList(status)).build();
    }

    @GetMapping("/{id}")
    public ApiResponse<CampaignProposalResponse> get(@PathVariable int id) {
        return ApiResponse.<CampaignProposalResponse>builder().result(service.adminGet(id)).build();
    }

    @Operation(summary = "Duyệt 1 hoặc gộp nhiều đơn thành 1 chiến dịch nháp")
    @PostMapping("/approve")
    public ApiResponse<CampaignResponse> approve(@RequestBody ProposalApproveRequest request) {
        return ApiResponse.<CampaignResponse>builder().result(service.approve(request)).build();
    }

    @Operation(summary = "Trả về cho shop sửa")
    @PostMapping("/{id}/request-changes")
    public ApiResponse<CampaignProposalResponse> requestChanges(@PathVariable int id, @RequestBody ProposalReplyRequest request) {
        return ApiResponse.<CampaignProposalResponse>builder().result(service.requestChanges(id, request)).build();
    }

    @Operation(summary = "Từ chối")
    @PostMapping("/{id}/reject")
    public ApiResponse<CampaignProposalResponse> reject(@PathVariable int id, @RequestBody ProposalReplyRequest request) {
        return ApiResponse.<CampaignProposalResponse>builder().result(service.reject(id, request)).build();
    }
}
