package com.sang.be_capnong.controller;

import com.nimbusds.jose.JOSEException;
import com.sang.be_capnong.dto.request.AuthenticationRequest;
import com.sang.be_capnong.dto.request.ForgotPasswordRequest;
import com.sang.be_capnong.dto.request.IntrospectRequest;
import com.sang.be_capnong.dto.request.LogoutRequest;
import com.sang.be_capnong.dto.request.RefreshRequest;
import com.sang.be_capnong.dto.request.ResetPasswordRequest;
import com.sang.be_capnong.dto.request.SocialLoginRequest;
import com.sang.be_capnong.dto.response.ApiResponse;
import com.sang.be_capnong.dto.response.AuthenticationResponse;
import com.sang.be_capnong.dto.response.IntrospectResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import com.sang.be_capnong.service.AuthenticationService;

import java.text.ParseException;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AuthenticationController {

    AuthenticationService authenticationService;

    /**
     * LOGIN
     */
    @PostMapping("/login")
    public ApiResponse<AuthenticationResponse> login(
            @RequestBody @Valid AuthenticationRequest request
    ) {
        log.info("Login request for email: {}", request.getEmail());

        return ApiResponse.<AuthenticationResponse>builder()
                .result(authenticationService.authenticated(request))
                .build();
    }

    /**
     * INTROSPECT TOKEN
     */
    @PostMapping("/introspect")
    public ApiResponse<IntrospectResponse> introspect(
            @RequestBody IntrospectRequest request
    ) throws ParseException, JOSEException {

        return ApiResponse.<IntrospectResponse>builder()
                .result(authenticationService.introspect(request))
                .build();
    }

    /**
     * LOGOUT
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @RequestBody LogoutRequest request
    ) throws ParseException, JOSEException {

        authenticationService.logout(request);
        return ApiResponse.<Void>builder().build();
    }

    /**
     * REFRESH TOKEN
     */
    @PostMapping("/refresh")
    public ApiResponse<AuthenticationResponse> refresh(
            @RequestBody RefreshRequest request
    ) throws ParseException, JOSEException {

        return ApiResponse.<AuthenticationResponse>builder()
                .result(authenticationService.refreshToken(request))
                .build();
    }

    /**
     * FORGOT PASSWORD
     */
    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(
            @RequestBody @Valid ForgotPasswordRequest request
    ) {
        log.info("REST request to request password reset OTP for email: {}", request.getEmail());
        authenticationService.forgotPassword(request);
        return ApiResponse.<Void>builder()
                .message("Mã xác thực OTP đặt lại mật khẩu đã được gửi đến email của bạn")
                .build();
    }

    /**
     * RESET PASSWORD
     */
    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(
            @RequestBody @Valid ResetPasswordRequest request
    ) {
        log.info("REST request to reset password for email: {}", request.getEmail());
        authenticationService.resetPassword(request);
        return ApiResponse.<Void>builder()
                .message("Đặt lại mật khẩu thành công. Bạn có thể đăng nhập bằng mật khẩu mới")
                .build();
    }

    /**
     * GOOGLE SOCIAL LOGIN (Client-side token exchange)
     */
    @PostMapping("/google")
    public ApiResponse<AuthenticationResponse> googleLogin(
            @RequestBody @Valid SocialLoginRequest request
    ) {
        log.info("REST request to login with Google");
        return ApiResponse.<AuthenticationResponse>builder()
                .result(authenticationService.socialLoginGoogle(request))
                .build();
    }
}
