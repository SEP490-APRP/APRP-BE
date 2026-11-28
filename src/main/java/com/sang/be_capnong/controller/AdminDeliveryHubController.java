package com.sang.be_capnong.controller;

import com.sang.be_capnong.dto.request.hub.DeliveryHubRequest;
import com.sang.be_capnong.dto.response.ApiResponse;
import com.sang.be_capnong.dto.response.hub.DeliveryHubResponse;
import com.sang.be_capnong.service.campaign.DeliveryHubService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/hubs")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Delivery Hub - Admin", description = "Điểm trung chuyển: xe CapNong chở thùng đã đóng gói tới đây, shipper lấy hàng tại hub")
public class AdminDeliveryHubController {

    DeliveryHubService hubService;
    com.sang.be_capnong.service.CampaignService campaignService;

    @Operation(summary = "Danh sách hub (kể cả hub đã ngưng)")
    @GetMapping("")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<DeliveryHubResponse>> list(@RequestParam(defaultValue = "false") boolean activeOnly) {
        return ApiResponse.<List<DeliveryHubResponse>>builder().result(hubService.list(activeOnly)).build();
    }

    @Operation(summary = "Hàng chiến dịch ở một hub kèm mã QR thùng")
    @GetMapping("/{id}/orders")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<com.sang.be_capnong.dto.response.campaign.PreOrderResponse>> orders(@PathVariable int id) {
        return ApiResponse.<List<com.sang.be_capnong.dto.response.campaign.PreOrderResponse>>builder().result(campaignService.getHubOrders(id)).build();
    }

    @Operation(summary = "Hub xác nhận đã nhận hàng")
    @PutMapping("/{id}/orders/receive")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Integer> receive(@PathVariable int id, @RequestBody com.sang.be_capnong.dto.request.campaign.PreOrderBatchRequest request) {
        return ApiResponse.<Integer>builder().result(campaignService.receiveAtHub(id, request)).build();
    }

    @Operation(summary = "Hub báo đơn thiếu / hư")
    @PutMapping("/{id}/orders/{preOrderId}/issue")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<com.sang.be_capnong.dto.response.campaign.PreOrderResponse> issue(@PathVariable int id, @PathVariable int preOrderId,
            @RequestBody com.sang.be_capnong.dto.request.hub.HubIssueRequest request) {
        return ApiResponse.<com.sang.be_capnong.dto.response.campaign.PreOrderResponse>builder()
                .result(campaignService.reportHubIssue(id, preOrderId, request)).build();
    }

    @Operation(summary = "Thêm hub")
    @PostMapping("")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<DeliveryHubResponse> create(@RequestBody DeliveryHubRequest request) {
        return ApiResponse.<DeliveryHubResponse>builder().result(hubService.create(request)).build();
    }

    @Operation(summary = "Sửa hub")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<DeliveryHubResponse> update(@PathVariable int id, @RequestBody DeliveryHubRequest request) {
        return ApiResponse.<DeliveryHubResponse>builder().result(hubService.update(id, request)).build();
    }

    @Operation(summary = "Ngưng dùng hub")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<DeliveryHubResponse> deactivate(@PathVariable int id) {
        return ApiResponse.<DeliveryHubResponse>builder().result(hubService.deactivate(id)).build();
    }
}
