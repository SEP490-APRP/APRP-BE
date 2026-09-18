package com.sang.be_capnong.controller;

import com.sang.be_capnong.dto.request.BuyerCreationRequest;
import com.sang.be_capnong.dto.request.ResendOtpRequest;
import com.sang.be_capnong.dto.request.ShipperCreationRequest;
import com.sang.be_capnong.dto.request.ShopCreationRequest;
import com.sang.be_capnong.dto.request.UserUpdateRequest;
import com.sang.be_capnong.dto.request.VerifyOtpRequest;
import com.sang.be_capnong.dto.response.ApiResponse;
import com.sang.be_capnong.dto.response.UserResponse;
import com.sang.be_capnong.service.UserService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserController {

    UserService userService;

    @PostMapping
    public ApiResponse<UserResponse> createUser(@RequestBody @Valid BuyerCreationRequest request) {
        log.info("REST request to create user (Buyer): {}", request.getEmail());
        return ApiResponse.<UserResponse>builder()
                .message("Đăng ký thành công. Vui lòng kiểm tra email để nhận mã xác thực OTP.")
                .result(userService.createBuyer(request))
                .build();
    }

    @PostMapping("/shops")
    public ApiResponse<UserResponse> createShop(@RequestBody @Valid ShopCreationRequest request) {
        log.info("REST request to create Shop: {}, shopName: {}", request.getEmail(), request.getShopName());
        return ApiResponse.<UserResponse>builder()
                .message("Đăng ký Shop thành công. Vui lòng kiểm tra email để nhận mã xác thực OTP.")
                .result(userService.createShop(request))
                .build();
    }

    @PostMapping("/shippers")
    public ApiResponse<UserResponse> createShipper(@RequestBody @Valid ShipperCreationRequest request) {
        log.info("REST request to create Shipper: {}, vehicleNumber: {}", request.getEmail(), request.getVehicleNumber());
        return ApiResponse.<UserResponse>builder()
                .message("Đăng ký Shipper thành công. Vui lòng kiểm tra email để nhận mã xác thực OTP.")
                .result(userService.createShipper(request))
                .build();
    }

    @PostMapping("/verify-otp")
    public ApiResponse<UserResponse> verifyAccount(@RequestBody @Valid VerifyOtpRequest request) {
        log.info("REST request to verify account with OTP for email: {}", request.getEmail());
        return ApiResponse.<UserResponse>builder()
                .message("Tài khoản đã được xác thực thành công")
                .result(userService.verifyAccount(request))
                .build();
    }

    @PostMapping("/resend-otp")
    public ApiResponse<Void> resendOtp(@RequestBody @Valid ResendOtpRequest request) {
        log.info("REST request to resend OTP to email: {}", request.getEmail());
        userService.resendVerificationOtp(request);
        return ApiResponse.<Void>builder()
                .message("Mã OTP mới đã được gửi đến email của bạn")
                .build();
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> getMyInfo() {
        log.info("REST request to get current user info");
        return ApiResponse.<UserResponse>builder()
                .result(userService.getMyInfo())
                .build();
    }

    @PutMapping("/me")
    public ApiResponse<UserResponse> updateMyInfo(@RequestBody @Valid UserUpdateRequest request) {
        log.info("REST request to update current user info");
        return ApiResponse.<UserResponse>builder()
                .message("Cập nhật thông tin thành công")
                .result(userService.updateMyInfo(request))
                .build();
    }

    @PatchMapping(value = "/me/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UserResponse> updateAvatar(
            @RequestParam(value = "logoUrl", required = false) MultipartFile logoUrl,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "file", required = false) MultipartFile file
    ) {
        log.info("REST request to update avatar for current user");
        MultipartFile targetFile = logoUrl != null ? logoUrl : (image != null ? image : file);
        return ApiResponse.<UserResponse>builder()
                .message("Cập nhật ảnh đại diện thành công")
                .result(userService.updateAvatar(targetFile))
                .build();
    }

    @GetMapping("/{userId}")
    public ApiResponse<UserResponse> getUserById(@PathVariable("userId") Integer userId) {
        log.info("REST request to get user by id: {}", userId);
        return ApiResponse.<UserResponse>builder()
                .result(userService.getUserById(userId))
                .build();
    }

    @GetMapping
    public ApiResponse<List<UserResponse>> getAllUsers() {
        log.info("REST request to get all users");
        return ApiResponse.<List<UserResponse>>builder()
                .result(userService.getAllUsers())
                .build();
    }
}