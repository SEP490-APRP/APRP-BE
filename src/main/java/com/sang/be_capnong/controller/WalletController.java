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
    @PreAuthorize("hasAnyRole('SHOP', 'SHIPPER', 'ADMIN')")
    public ApiResponse<WalletResponse> getMyWallet() {
        return ApiResponse.<WalletResponse>builder().result(walletService.getMyWallet()).build();
    }

    @Operation(summary = "Lịch sử biến động ví của tôi")
    @GetMapping("/me/transactions")
    @PreAuthorize("hasAnyRole('SHOP', 'SHIPPER', 'ADMIN')")
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

    // ── ADMIN ──────────────────────────────────────────

    @Operation(summary = "[ADMIN] Ví sàn")
    @GetMapping("/platform")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<WalletResponse> getPlatformWallet() {
        return ApiResponse.<WalletResponse>builder().result(walletService.getPlatformWallet()).build();
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
