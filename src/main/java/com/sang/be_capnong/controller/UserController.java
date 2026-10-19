package com.sang.be_capnong.controller;

import com.sang.be_capnong.dto.request.BuyerCreationRequest;
import com.sang.be_capnong.dto.request.ChangePasswordRequest;
import com.sang.be_capnong.dto.request.ResendOtpRequest;
import com.sang.be_capnong.dto.request.ShipperCreationRequest;
import com.sang.be_capnong.dto.request.ShopCreationRequest;
import com.sang.be_capnong.dto.request.UserCreationRequest;
import com.sang.be_capnong.dto.request.UserUpdateRequest;
import com.sang.be_capnong.dto.request.VerifyOtpRequest;
import com.sang.be_capnong.dto.response.ApiResponse;
import com.sang.be_capnong.dto.response.PageResponse;
import com.sang.be_capnong.dto.response.UserResponse;
import com.sang.be_capnong.service.UserService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
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
                .result(userService.createBuyer(request))
                .build();
    }

    @PostMapping("/shops")
    public ApiResponse<UserResponse> createShop(@RequestBody @Valid ShopCreationRequest request) {
        log.info("REST request to create Shop: {}, shopName: {}", request.getEmail(), request.getShopName());
        return ApiResponse.<UserResponse>builder()
                .result(userService.createShop(request))
                .build();
    }

    @PostMapping("/shippers")
    public ApiResponse<UserResponse> createShipper(@RequestBody @Valid ShipperCreationRequest request) {
        log.info("REST request to create Shipper: {}, vehicleNumber: {}", request.getEmail(), request.getVehicleNumber());
        return ApiResponse.<UserResponse>builder()
                .result(userService.createShipper(request))
                .build();
    }

    @PostMapping("/verify-otp")
    public ApiResponse<UserResponse> verifyAccount(@RequestBody @Valid VerifyOtpRequest request) {
        log.info("REST request to verify account with OTP for email: {}", request.getEmail());
        return ApiResponse.<UserResponse>builder()
                .result(userService.verifyAccount(request))
                .build();
    }

    @PostMapping("/resend-otp")
    public ApiResponse<Void> resendOtp(@RequestBody @Valid ResendOtpRequest request) {
        log.info("REST request to resend OTP to email: {}", request.getEmail());
        userService.resendVerificationOtp(request);
        return ApiResponse.<Void>builder()
                .message("OTP resent successfully")
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
                .result(userService.updateMyInfo(request))
                .build();
    }

    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(@RequestBody @Valid ChangePasswordRequest request) {
        log.info("REST request to change password for current user");
        userService.changePassword(request);
        return ApiResponse.<Void>builder()
                .message("Đổi mật khẩu thành công")
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
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<UserResponse>> getAllUsers() {
        log.info("REST request to get all users");
        return ApiResponse.<List<UserResponse>>builder()
                .result(userService.getAllUsers())
                .build();
    }

    @GetMapping("/paged")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageResponse<UserResponse>> getUsersByRolePaged(
            @RequestParam(name = "roles", required = false) List<String> roles,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "size", required = false, defaultValue = "10") int size
    ) {
        log.info("REST request to get users paged. roles: {}, status: {}, page: {}, size: {}", roles, status, page, size);
        return ApiResponse.<PageResponse<UserResponse>>builder()
                .result(userService.getUsersByRolePaged(roles, status, page, size))
                .build();
    }

    @PatchMapping("/{userId}/approve-shop-owner")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> approveShopOwner(@PathVariable("userId") Integer userId) {
        log.info("REST request to approve shop owner with userId: {}", userId);
        return ApiResponse.<UserResponse>builder()
                .result(userService.approveShopOwner(userId))
                .build();
    }

    @PatchMapping("/{userId}/approve-shipper")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> approveShipper(@PathVariable("userId") Integer userId) {
        log.info("REST request to approve shipper with userId: {}", userId);
        return ApiResponse.<UserResponse>builder()
                .result(userService.approveShipper(userId))
                .build();
    }

    @PatchMapping("/{userId}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> activateUser(@PathVariable("userId") Integer userId) {
        log.info("REST request to activate user with userId: {}", userId);
        userService.activateUser(userId);
        return ApiResponse.<Void>builder()
                .message("User activated successfully")
                .build();
    }

    @PatchMapping("/{userId}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deactivateUser(@PathVariable("userId") Integer userId) {
        log.info("REST request to deactivate user with userId: {}", userId);
        userService.deactivateUser(userId);
        return ApiResponse.<Void>builder()
                .message("User deactivated successfully")
                .build();
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<UserResponse> updateUser(@PathVariable("userId") Integer userId, @RequestBody @Valid UserUpdateRequest request) {
        log.info("REST request to update user profile by id: {}", userId);
        return ApiResponse.<UserResponse>builder()
                .result(userService.updateUser(userId, request))
                .build();
    }
}