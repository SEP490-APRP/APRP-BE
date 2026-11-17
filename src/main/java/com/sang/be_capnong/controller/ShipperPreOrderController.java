package com.sang.be_capnong.controller;

import com.sang.be_capnong.dto.request.campaign.*;
import com.sang.be_capnong.dto.response.ApiResponse;
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
@RequestMapping("/shipper/pre-orders")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Campaign - Shipper", description = "Shipper nhận đơn đặt trước tại hub và giao (WF-04)")
public class ShipperPreOrderController {

    CampaignService campaignService;

    @Operation(summary = "Đơn đặt trước đang chờ tại hub")
    @GetMapping("/ready")
    @PreAuthorize("hasRole('SHIPPER')")
    public ApiResponse<List<PreOrderResponse>> ready() {
        return ApiResponse.<List<PreOrderResponse>>builder().result(campaignService.getReadyForPickup()).build();
    }

    @Operation(summary = "Đơn đặt trước tôi đang / đã giao")
    @GetMapping("/my")
    @PreAuthorize("hasRole('SHIPPER')")
    public ApiResponse<List<PreOrderResponse>> mine() {
        return ApiResponse.<List<PreOrderResponse>>builder().result(campaignService.getMyShipperPreOrders()).build();
    }

    @Operation(summary = "Nhận đơn tại hub")
    @PutMapping("/{id}/accept")
    @PreAuthorize("hasRole('SHIPPER')")
    public ApiResponse<PreOrderResponse> accept(@PathVariable int id) {
        return ApiResponse.<PreOrderResponse>builder().result(campaignService.acceptPreOrder(id)).build();
    }

    @Operation(summary = "Giao thành công (ảnh POD), thu phần còn lại")
    @PutMapping("/{id}/deliver")
    @PreAuthorize("hasRole('SHIPPER')")
    public ApiResponse<PreOrderResponse> deliver(@PathVariable int id, @RequestBody(required = false) PreOrderDeliveryRequest request) {
        return ApiResponse.<PreOrderResponse>builder().result(campaignService.deliverPreOrder(id, request)).build();
    }

    @Operation(summary = "Giao thất bại, hàng về hub")
    @PutMapping("/{id}/fail")
    @PreAuthorize("hasRole('SHIPPER')")
    public ApiResponse<PreOrderResponse> fail(@PathVariable int id, @RequestBody(required = false) PreOrderDeliveryRequest request) {
        return ApiResponse.<PreOrderResponse>builder().result(campaignService.failPreOrder(id, request)).build();
    }
}
