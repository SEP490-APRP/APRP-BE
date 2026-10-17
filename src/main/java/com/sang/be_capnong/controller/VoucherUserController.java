package com.sang.be_capnong.controller;

import com.sang.be_capnong.dto.response.ApiResponse;
import com.sang.be_capnong.dto.response.VoucherResponse;
import com.sang.be_capnong.service.VoucherService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"", "/v1", "/api/v1"})
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VoucherUserController {

    VoucherService voucherService;

    // Lấy danh sách các voucher đang public để hiển thị
    @GetMapping("/vouchers")
    public ApiResponse<List<VoucherResponse>> getAvailablePublicVouchers() {
        return ApiResponse.<List<VoucherResponse>>builder()
                .result(voucherService.getAvailablePublicVouchers())
                .build();
    }

    // Lưu mã hiển thị sẵn
    @PostMapping("/vouchers/{voucherId}/collect")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> collectVoucher(@PathVariable int voucherId) {
        return voucherService.collectVoucher(voucherId);
    }

    // Lưu mã ẩn bằng code
    @PostMapping("/vouchers/collect-by-code")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> collectVoucherByCode(@RequestParam String voucherCode) {
        return voucherService.collectVoucherByCode(voucherCode);
    }

    // Lấy danh sách ví voucher của người dùng
    @GetMapping("/users/me/vouchers")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<VoucherResponse>> getMyVouchers() {
        return ApiResponse.<List<VoucherResponse>>builder()
                .result(voucherService.getMyVouchers())
                .build();
    }
}
