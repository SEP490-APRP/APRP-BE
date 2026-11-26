package com.sang.be_capnong.controller;

import com.sang.be_capnong.dto.response.ApiResponse;
import com.sang.be_capnong.dto.response.hub.HubTripResponse;
import com.sang.be_capnong.service.campaign.HubTripService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/shipper/hub-trips")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Campaign - Shipper (xe ra hub)", description = "Tài xế nhận chuyến chở hàng chiến dịch từ điểm tập kết ra hub")
public class ShipperHubTripController {

    HubTripService hubTripService;

    @Operation(summary = "Chuyến chưa có tài xế (đang kêu gọi)")
    @GetMapping("/open")
    @PreAuthorize("hasRole('SHIPPER')")
    public ApiResponse<List<HubTripResponse>> open() {
        return ApiResponse.<List<HubTripResponse>>builder().result(hubTripService.openTrips()).build();
    }

    @Operation(summary = "Chuyến của tôi")
    @GetMapping("/my")
    @PreAuthorize("hasRole('SHIPPER')")
    public ApiResponse<List<HubTripResponse>> mine() {
        return ApiResponse.<List<HubTripResponse>>builder().result(hubTripService.myTrips()).build();
    }

    @Operation(summary = "Nhận chuyến")
    @PutMapping("/{id}/accept")
    @PreAuthorize("hasRole('SHIPPER')")
    public ApiResponse<HubTripResponse> accept(@PathVariable int id) {
        return ApiResponse.<HubTripResponse>builder().result(hubTripService.accept(id)).build();
    }

    @Operation(summary = "Không nhận chuyến đang chờ")
    @PutMapping("/{id}/decline")
    @PreAuthorize("hasRole('SHIPPER')")
    public ApiResponse<HubTripResponse> decline(@PathVariable int id) {
        return ApiResponse.<HubTripResponse>builder().result(hubTripService.decline(id)).build();
    }

    @Operation(summary = "Trả chuyến (chưa lấy hàng)")
    @PutMapping("/{id}/release")
    @PreAuthorize("hasRole('SHIPPER')")
    public ApiResponse<HubTripResponse> release(@PathVariable int id) {
        return ApiResponse.<HubTripResponse>builder().result(hubTripService.release(id)).build();
    }

    @Operation(summary = "Đánh dấu thùng đã nhận lên xe")
    @PutMapping("/{id}/boxes")
    @PreAuthorize("hasRole('SHIPPER')")
    public ApiResponse<HubTripResponse> boxes(@PathVariable int id, @RequestBody com.sang.be_capnong.dto.request.hub.HubTripBoxRequest request) {
        return ApiResponse.<HubTripResponse>builder().result(hubTripService.loadBoxes(id, request)).build();
    }

    @Operation(summary = "Đã lấy hàng tại điểm tập kết, bắt đầu chở ra hub")
    @PutMapping("/{id}/start")
    @PreAuthorize("hasRole('SHIPPER')")
    public ApiResponse<HubTripResponse> start(@PathVariable int id, @RequestBody com.sang.be_capnong.dto.request.hub.HubPhotoRequest request) {
        return ApiResponse.<HubTripResponse>builder().result(hubTripService.start(id, request.getPhotoUrl())).build();
    }

    @Operation(summary = "Đã giao hàng tới một hub trên lộ trình, kèm ảnh")
    @PutMapping("/{id}/stops/{hubId}/arrive")
    @PreAuthorize("hasRole('SHIPPER')")
    public ApiResponse<HubTripResponse> arriveStop(@PathVariable int id, @PathVariable int hubId,
            @RequestBody com.sang.be_capnong.dto.request.hub.HubPhotoRequest request) {
        return ApiResponse.<HubTripResponse>builder().result(hubTripService.arriveStop(id, hubId, request.getPhotoUrl())).build();
    }

}
