package com.sang.be_capnong.controller;

import com.sang.be_capnong.dto.response.ApiResponse;
import com.sang.be_capnong.dto.response.wallet.OrderSettlementResponse;
import com.sang.be_capnong.dto.response.wallet.RefundDueResponse;
import com.sang.be_capnong.dto.response.wallet.WalletResponse;
import com.sang.be_capnong.dto.response.wallet.WalletTransactionResponse;
import com.sang.be_capnong.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/wallets")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Wallet & Settlement", description = "Ví Shop / Shipper / Sàn và bảng chia tiền từng đơn hàng")
public class WalletController {

    WalletService walletService;

    @Operation(summary = "Ví của tôi", description = "[SHOP, SHIPPER, ADMIN] Shop/Shipper xem ví của mình, Admin xem ví sàn.")
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('SHOP', 'SHIPPER', 'BUYER', 'ADMIN')")
    public ApiResponse<WalletResponse> getMyWallet() {
        return ApiResponse.<WalletResponse>builder().result(walletService.getMyWallet()).build();
    }

    @Operation(summary = "Lịch sử biến động ví của tôi")
    @GetMapping("/me/transactions")
    @PreAuthorize("hasAnyRole('SHOP', 'SHIPPER', 'BUYER', 'ADMIN')")
    public ApiResponse<Page<WalletTransactionResponse>> getMyTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.<Page<WalletTransactionResponse>>builder()
                .result(walletService.getMyTransactions(page, size)).build();
    }

    @Operation(summary = "Bảng chia tiền của 1 đơn",
            description = "[SHOP/SHIPPER của đơn, ADMIN] Tiền hàng, voucher, phí dịch vụ và số tiền Shop / Shipper / Sàn nhận. "
                    + "Đơn chưa COMPLETED thì trả số dự kiến (settled=false).")
    @GetMapping("/orders/{orderId}/settlement")
    public ApiResponse<OrderSettlementResponse> getOrderSettlement(@PathVariable int orderId) {
        return ApiResponse.<OrderSettlementResponse>builder()
                .result(walletService.getOrderSettlement(orderId)).build();
    }

    @Operation(summary = "Tạo lệnh rút tiền về ngân hàng", description = "Shop / Shipper / Buyer. Tối thiểu capnong.wallet.min-withdraw (mặc định 50.000đ). Số dư bị trừ ngay, Admin chuyển khoản rồi xác nhận.")
    @PostMapping("/withdrawals")
    @PreAuthorize("hasAnyRole('SHOP', 'SHIPPER', 'BUYER')")
    public ApiResponse<com.sang.be_capnong.dto.response.wallet.WithdrawResponse> requestWithdraw(
            @RequestBody com.sang.be_capnong.dto.request.wallet.WithdrawCreateRequest request) {
        return ApiResponse.<com.sang.be_capnong.dto.response.wallet.WithdrawResponse>builder()
                .result(walletService.requestWithdraw(request)).build();
    }

    @Operation(summary = "Lệnh rút tiền của tôi")
    @GetMapping("/withdrawals/me")
    @PreAuthorize("hasAnyRole('SHOP', 'SHIPPER', 'BUYER')")
    public ApiResponse<List<com.sang.be_capnong.dto.response.wallet.WithdrawResponse>> getMyWithdrawals() {
        return ApiResponse.<List<com.sang.be_capnong.dto.response.wallet.WithdrawResponse>>builder()
                .result(walletService.getMyWithdrawals()).build();
    }

    // ── ADMIN ──────────────────────────────────────────

    @Operation(summary = "[ADMIN] Danh sách lệnh rút tiền", description = "status = PENDING | COMPLETED | REJECTED, bỏ trống = tất cả")
    @GetMapping("/withdrawals")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<com.sang.be_capnong.dto.response.wallet.WithdrawResponse>> getWithdrawals(
            @RequestParam(required = false) String status) {
        return ApiResponse.<List<com.sang.be_capnong.dto.response.wallet.WithdrawResponse>>builder()
                .result(walletService.getWithdrawals(status)).build();
    }

    @Operation(summary = "[ADMIN] Xác nhận đã chuyển khoản cho lệnh rút")
    @PostMapping("/withdrawals/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<com.sang.be_capnong.dto.response.wallet.WithdrawResponse> approveWithdraw(
            @PathVariable int id,
            @RequestBody(required = false) com.sang.be_capnong.dto.request.wallet.WithdrawProcessRequest body) {
        return ApiResponse.<com.sang.be_capnong.dto.response.wallet.WithdrawResponse>builder()
                .result(walletService.approveWithdraw(id, body != null ? body.getNote() : null)).build();
    }

    @Operation(summary = "[ADMIN] Từ chối lệnh rút, hoàn tiền vào ví")
    @PostMapping("/withdrawals/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<com.sang.be_capnong.dto.response.wallet.WithdrawResponse> rejectWithdraw(
            @PathVariable int id,
            @RequestBody(required = false) com.sang.be_capnong.dto.request.wallet.WithdrawProcessRequest body) {
        return ApiResponse.<com.sang.be_capnong.dto.response.wallet.WithdrawResponse>builder()
                .result(walletService.rejectWithdraw(id, body != null ? body.getNote() : null)).build();
    }

    @Operation(summary = "[ADMIN] Ví sàn")
    @GetMapping("/platform")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<WalletResponse> getPlatformWallet() {
        return ApiResponse.<WalletResponse>builder().result(walletService.getPlatformWallet()).build();
    }

    @Operation(summary = "[ADMIN] Tổng hợp ví sàn", description = "Tiền đang giữ hộ Shop/Shipper, hoa hồng, tiền sàn bù, COD chưa nộp, cần hoàn tiền, bảng theo đối tác. period: TODAY | 7D | 30D | ALL")
    @GetMapping("/platform/overview")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<com.sang.be_capnong.dto.response.wallet.PlatformOverviewResponse> getPlatformOverview(
            @RequestParam(defaultValue = "30D") String period) {
        return ApiResponse.<com.sang.be_capnong.dto.response.wallet.PlatformOverviewResponse>builder()
                .result(walletService.getPlatformOverview(period)).build();
    }

    @Operation(summary = "[ADMIN] Lịch sử biến động ví sàn")
    @GetMapping("/platform/transactions")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Page<WalletTransactionResponse>> getPlatformTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.<Page<WalletTransactionResponse>>builder()
                .result(walletService.getPlatformTransactions(page, size)).build();
    }

    @Operation(summary = "[ADMIN] Chạy quyết toán lại cho 1 đơn COMPLETED", description = "An toàn khi gọi nhiều lần: đơn đã chia rồi sẽ không chia lại.")
    @PostMapping("/orders/{orderId}/settle")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<OrderSettlementResponse> settleOrder(@PathVariable int orderId) {
        return ApiResponse.<OrderSettlementResponse>builder()
                .result(walletService.settleOrder(orderId)).build();
    }

    @Operation(summary = "[ADMIN] Đơn COD Shipper đang giữ tiền mặt chưa nộp")
    @GetMapping("/cod/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<OrderSettlementResponse>> getPendingCod() {
        return ApiResponse.<List<OrderSettlementResponse>>builder()
                .result(walletService.getPendingCodOrders()).build();
    }

    @Operation(summary = "[ADMIN] Xác nhận Shipper đã nộp tiền mặt COD của đơn")
    @PostMapping("/cod/{orderId}/settle")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<OrderSettlementResponse> settleCod(@PathVariable int orderId) {
        return ApiResponse.<OrderSettlementResponse>builder()
                .result(walletService.settleCodCash(orderId))
                .message("COD cash settled.").build();
    }

    @Operation(summary = "[ADMIN] Đơn cần hoàn tiền cho Buyer", description = "Đơn đã thanh toán online rồi bị huỷ, giao thất bại, hoặc bị thanh toán trùng.")
    @GetMapping("/refunds")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<RefundDueResponse>> getRefundsDue() {
        return ApiResponse.<List<RefundDueResponse>>builder()
                .result(walletService.getRefundsDue()).build();
    }
}
