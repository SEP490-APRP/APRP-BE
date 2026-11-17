package com.sang.be_capnong.controller;

import com.sang.be_capnong.dto.request.DeviceTokenRegisterRequest;
import com.sang.be_capnong.dto.request.DeviceTokenUnregisterRequest;
import com.sang.be_capnong.dto.response.ApiResponse;
import com.sang.be_capnong.service.DeviceTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/devices", "/api/v1/devices", "/notifications/devices"})
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Device Token APIs", description = "Đăng ký và quản lý token thiết bị phục vụ Push Notification ra điện thoại")
public class DeviceTokenController {

    DeviceTokenService deviceTokenService;

    @PostMapping("/token")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Đăng ký thiết bị nhận push notification", description = "Đăng ký hoặc cập nhật ExpoPushToken khi người dùng đăng nhập vào ứng dụng di động.")
    public ApiResponse<Void> registerToken(@Valid @RequestBody DeviceTokenRegisterRequest request) {
        deviceTokenService.registerDeviceToken(request);
        return ApiResponse.<Void>builder()
                .message("Đăng ký token thiết bị thành công")
                .build();
    }

    @DeleteMapping("/token")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Hủy token thiết bị", description = "Hủy kích hoạt token thiết bị khi người dùng đăng xuất để tránh nhận thông báo sai người dùng.")
    public ApiResponse<Void> unregisterToken(@Valid @RequestBody DeviceTokenUnregisterRequest request) {
        deviceTokenService.unregisterDeviceToken(request);
        return ApiResponse.<Void>builder()
                .message("Hủy token thiết bị thành công")
                .build();
    }
}

