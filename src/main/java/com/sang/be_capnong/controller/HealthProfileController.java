package com.sang.be_capnong.controller;

import com.sang.be_capnong.dto.request.ManualHealthProfileUpdateRequest;
import com.sang.be_capnong.dto.response.ApiResponse;
import com.sang.be_capnong.dto.response.UserHealthProfileResponse;
import com.sang.be_capnong.service.HealthProfileService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/health-profile")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class HealthProfileController {

    HealthProfileService healthProfileService;
    com.sang.be_capnong.service.HealthProfileExtractService healthProfileExtractService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('BUYER')")
    public ApiResponse<UserHealthProfileResponse> getMyHealthProfile() {
        log.info("REST request to get current buyer health profile");
        return ApiResponse.<UserHealthProfileResponse>builder()
                .result(healthProfileService.getMyHealthProfile())
                .build();
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('BUYER')")
    public ApiResponse<UserHealthProfileResponse> updateMyHealthProfileManual(
            @RequestBody @Valid ManualHealthProfileUpdateRequest request
    ) {
        log.info("REST request to manually update buyer health profile");
        return ApiResponse.<UserHealthProfileResponse>builder()
                .result(healthProfileService.updateMyHealthProfileManual(request))
                .build();
    }

    @PostMapping("/extract")
    @PreAuthorize("hasRole('BUYER')")
    public ApiResponse<com.sang.be_capnong.dto.response.HealthProfileExtractResponse> extractHealthProfile(
            @RequestBody @Valid com.sang.be_capnong.dto.request.HealthProfileExtractRequest request
    ) {
        log.info("REST request to extract health profile from text");
        return ApiResponse.<com.sang.be_capnong.dto.response.HealthProfileExtractResponse>builder()
                .result(healthProfileExtractService.extractProfile(request))
                .build();
    }

    @PostMapping("/requests/{id}/confirm")
    @PreAuthorize("hasRole('BUYER')")
    public ApiResponse<UserHealthProfileResponse> confirmProfileUpdate(
            @PathVariable("id") Integer id,
            @RequestBody(required = false) com.sang.be_capnong.dto.request.ConfirmHealthProfileRequest request
    ) {
        log.info("REST request to confirm profile update request id: {}", id);
        com.sang.be_capnong.dto.request.ConfirmHealthProfileRequest body =
                request != null ? request : new com.sang.be_capnong.dto.request.ConfirmHealthProfileRequest();
        return ApiResponse.<UserHealthProfileResponse>builder()
                .result(healthProfileExtractService.confirmProfileUpdate(id, body))
                .build();
    }
}
