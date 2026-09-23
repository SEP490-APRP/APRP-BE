package com.sang.be_capnong.controller;

import com.sang.be_capnong.dto.request.ShipperStatusRequest;
import com.sang.be_capnong.dto.response.ApiResponse;
import com.sang.be_capnong.service.ShipperService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/shippers")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ShipperController {

    ShipperService shipperService;

    // Ideally, the shipperId should come from the SecurityContext
    // For now, we will pass it as a path variable to keep it unblocked if auth is not fully configured
    @PostMapping("/{shipperId}/status/online")
    public ApiResponse<Void> goOnline(@PathVariable Integer shipperId, @RequestBody @Valid ShipperStatusRequest request) {
        shipperService.goOnline(shipperId, request);
        return ApiResponse.<Void>builder().message("Shipper went online successfully").build();
    }

    @PostMapping("/{shipperId}/status/offline")
    public ApiResponse<Void> goOffline(@PathVariable Integer shipperId) {
        shipperService.goOffline(shipperId);
        return ApiResponse.<Void>builder().message("Shipper went offline successfully").build();
    }
}
