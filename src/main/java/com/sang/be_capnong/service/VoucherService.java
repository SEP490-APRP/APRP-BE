package com.sang.be_capnong.service;

import com.sang.be_capnong.dto.request.VoucherCreateRequest;
import com.sang.be_capnong.dto.response.ApiResponse;
import com.sang.be_capnong.dto.response.VoucherResponse;

import java.util.List;

public interface VoucherService {
    // --- CHO ADMIN VÀ SHOP ---
    VoucherResponse createVoucher(VoucherCreateRequest request, boolean isAdmin);
    List<VoucherResponse> getManagementVouchers(boolean isAdmin);
    VoucherResponse toggleVoucherStatus(int voucherId);

    // --- CHO BUYER ---
    List<VoucherResponse> getAvailablePublicVouchers();
    ApiResponse<Void> collectVoucher(int voucherId);
    ApiResponse<Void> collectVoucherByCode(String voucherCode);
    List<VoucherResponse> getMyVouchers();
}
