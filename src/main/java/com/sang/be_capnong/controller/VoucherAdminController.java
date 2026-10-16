package com.sang.be_capnong.controller;

import com.sang.be_capnong.dto.request.VoucherCreateRequest;
import com.sang.be_capnong.dto.response.ApiResponse;
import com.sang.be_capnong.dto.response.PageResponse;
import com.sang.be_capnong.dto.response.VoucherResponse;
import com.sang.be_capnong.service.VoucherService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/admin/vouchers", "/api/v1/admin/vouchers"})
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VoucherAdminController {

    VoucherService voucherService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<VoucherResponse> createAdminVoucher(@Valid @RequestBody VoucherCreateRequest request) {
        return ApiResponse.<VoucherResponse>builder()
                .result(voucherService.createVoucher(request, true))
                .build();
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageResponse<VoucherResponse>> getManagementVouchers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.<PageResponse<VoucherResponse>>builder()
                .result(voucherService.getManagementVouchers(true, page, size))
                .build();
    }

    @PutMapping("/{voucherId}/toggle-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<VoucherResponse> toggleStatus(@PathVariable int voucherId) {
        return ApiResponse.<VoucherResponse>builder()
                .result(voucherService.toggleVoucherStatus(voucherId))
                .build();
    }

    @DeleteMapping("/{voucherId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteVoucher(@PathVariable int voucherId) {
        return voucherService.deleteVoucher(voucherId);
    }
}
