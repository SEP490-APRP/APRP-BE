package com.sang.be_capnong.controller;

import com.sang.be_capnong.dto.request.ShipperStatusRequest;
import com.sang.be_capnong.dto.response.ApiResponse;
import com.sang.be_capnong.dto.response.ShipperStatusResponse;
import com.sang.be_capnong.service.ShipperService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/shippers")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ShipperController {

    ShipperService shipperService;

    @Operation(summary = "Shipper bật trực tuyến / heartbeat",
            description = "[SHIPPER only] Bật Online kèm tọa độ GPS. App gọi lại mỗi ~60s khi đang online (heartbeat); "
                    + "quá capnong.dispatch.heartbeat-timeout-seconds không gọi thì tự chuyển offline.")
    @PostMapping("/status/online")
    @PreAuthorize("hasRole('SHIPPER')")
    public ApiResponse<Void> goOnlineSelf(@RequestBody @Valid ShipperStatusRequest request) {
        shipperService.goOnlineCurrentShipper(request);
        return ApiResponse.<Void>builder().message("Shipper went online successfully").build();
    }

    @Operation(summary = "Shipper tắt trực tuyến (nghỉ ca)",
            description = "[SHIPPER only] Shipper tắt trạng thái Online, tạm ngừng nhận đơn.")
    @PostMapping("/status/offline")
    @PreAuthorize("hasRole('SHIPPER')")
    public ApiResponse<Void> goOfflineSelf() {
        shipperService.goOfflineCurrentShipper();
        return ApiResponse.<Void>builder().message("Shipper went offline successfully").build();
    }

    @Operation(summary = "Lấy trạng thái trực tuyến hiện tại của Shipper",
            description = "[SHIPPER only] Kiểm tra shipper hiện đang Online hay Offline kèm vị trí cập nhật gần nhất.")
    @GetMapping("/status")
    @PreAuthorize("hasRole('SHIPPER')")
    public ApiResponse<ShipperStatusResponse> getShipperStatus() {
        return ApiResponse.<ShipperStatusResponse>builder()
                .result(shipperService.getCurrentShipperStatus())
                .message("Shipper status retrieved.")
                .build();
    }

    @Operation(summary = "Lấy vị trí thời gian thực của Shipper theo đơn hàng",
            description = "[BUYER, SHOP, SHIPPER, ADMIN] Lấy tọa độ và thông tin lộ trình hiện tại của đơn hàng.")
    @GetMapping("/location/order/{orderId}")
    public ApiResponse<com.sang.be_capnong.dto.response.ShipperLocationResponse> getShipperLocation(@PathVariable Integer orderId) {
        return ApiResponse.<com.sang.be_capnong.dto.response.ShipperLocationResponse>builder()
                .result(shipperService.getShipperLocationByOrder(orderId))
                .message("Shipper location retrieved successfully")
                .build();
    }

    // Đã bỏ POST /shippers/{shipperId}/status/online|offline: không phân quyền, ai cũng bật/tắt hộ shipper khác được.
}
