package com.sang.be_capnong.service;

import com.sang.be_capnong.dto.request.wallet.WithdrawCreateRequest;
import com.sang.be_capnong.dto.response.wallet.*;
import com.sang.be_capnong.entity.ReturnRequest;
import org.springframework.data.domain.Page;

import java.util.List;

public interface WalletService {

    /** Chia tiền đơn COMPLETED vào ví Shop / Shipper / Sàn. Gọi lại nhiều lần vẫn chỉ chia 1 lần. */
    OrderSettlementResponse settleOrder(int orderId);

    /** Tính bảng chia tiền (dự kiến nếu chưa quyết toán) - không ghi gì vào DB */
    OrderSettlementResponse getOrderSettlement(int orderId);

    /** Tạm giữ phần của Shop và Sàn khi tiền đã vào sàn (PayOS PAID, hoặc COD vừa thu tiền mặt) */
    void holdShopAndPlatformIncome(int orderId);

    /** Tạm giữ cước giao cho Shipper ngay khi Shipper nhận đơn */
    void holdShipperIncome(int orderId);

    /** Đơn huỷ / giao thất bại: gỡ toàn bộ tiền tạm giữ của đơn */
    void cancelHolds(int orderId, String reason);

    /** Xử lý hoàn tiền cho người mua và khấu trừ tiền tạm giữ của shop khi yêu cầu hoàn hàng hoàn tất */
    void processReturnRefund(ReturnRequest returnRequest);

    /** Ghi nợ Shipper số tiền mặt COD vừa thu của khách (gọi khi DELIVERED) */
    void recordCodCashCollected(int orderId);

    /** Admin xác nhận Shipper đã nộp tiền mặt COD về sàn */
    OrderSettlementResponse settleCodCash(int orderId);

    WalletResponse getMyWallet();

    Page<WalletTransactionResponse> getMyTransactions(int page, int size);

    WalletResponse getPlatformWallet();

    Page<WalletTransactionResponse> getPlatformTransactions(int page, int size);

    List<OrderSettlementResponse> getPendingCodOrders();

    List<RefundDueResponse> getRefundsDue();

    // ── Rút tiền ──

    /** Shop / Shipper / Buyer tạo lệnh rút: trừ số dư ngay, chờ Admin chuyển khoản */
    WithdrawResponse requestWithdraw(WithdrawCreateRequest request);

    List<WithdrawResponse> getMyWithdrawals();

    /** Admin: status = PENDING | COMPLETED | REJECTED, null = tất cả */
    List<WithdrawResponse> getWithdrawals(String status);

    /** Admin đã chuyển khoản xong */
    WithdrawResponse approveWithdraw(int requestId, String note);

    /** Admin từ chối: trả lại tiền vào số dư */
    WithdrawResponse rejectWithdraw(int requestId, String note);

    /** Số liệu tổng hợp màn Ví Sàn. period: TODAY | 7D | 30D | ALL */
    PlatformOverviewResponse getPlatformOverview(String period);
}
