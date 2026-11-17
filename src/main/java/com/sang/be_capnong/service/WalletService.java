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

    /** Trừ cước vận chuyển hoàn trả hàng từ ví khả dụng của Shop (kiểm tra số dư trước khi trừ) */
    void deductShopReturnShippingFee(ReturnRequest returnRequest);

    /** Hoàn trả lại cước vận chuyển hoàn trả hàng vào ví Shop khi yêu cầu hoàn hàng bị huỷ/từ chối */
    void refundShopReturnShippingFee(ReturnRequest returnRequest);

    /** Ghi nợ Shipper số tiền mặt COD vừa thu của khách (gọi khi DELIVERED) */
    void recordCodCashCollected(int orderId);

    /**
     * Shipper vừa được gán vào đơn: chặn nếu ví âm (nợ COD cũ); đơn COD thì giữ chỗ đủ totalAmount
     * trong ví (ném SHIPPER_COD_BALANCE_INSUFFICIENT nếu thiếu, kèm required/available/missing).
     */
    void reserveForShipperAccept(int orderId);

    /** Đơn COD thất bại / huỷ: gỡ tiền ký quỹ. Gọi nhiều lần vẫn chỉ gỡ 1 lần */
    void releaseCodReserve(int orderId, String reason);

    /** Số dư khả dụng của Shipper (balance - codReserved), 0 nếu chưa có ví */
    java.math.BigDecimal getShipperAvailableBalance(int shipperId);

    /** Tạo link PayOS nạp tiền vào ví của tôi (Shipper / Shop) */
    com.sang.be_capnong.dto.response.wallet.WalletTopupResponse createTopup(com.sang.be_capnong.dto.request.wallet.WalletTopupCreateRequest request);

    List<com.sang.be_capnong.dto.response.wallet.WalletTopupResponse> getMyTopups();

    /** Client quay lại từ PayOS: hỏi PayOS rồi cộng ví nếu đã trả */
    com.sang.be_capnong.dto.response.wallet.WalletTopupResponse confirmTopup(String orderCode);

    /** Webhook PayOS: nếu orderCode là lệnh nạp ví thì cộng ví (idempotent) và trả true */
    boolean settleTopupIfExists(String orderCode);

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

    /**
     * Quyết toán cộng hoa hồng tiếp thị liên kết vào ví TYPE_BUYER của người giới thiệu.
     * Idempotent dựa trên referral_commission_id (1-1 không thể ghi trùng).
     */
    void creditReferralCommission(com.sang.be_capnong.entity.User referrer, com.sang.be_capnong.entity.Order order, Long referralCommissionId, java.math.BigDecimal amount, String description);

    /**
     * Giải phóng cước giao hàng vào số dư khả dụng của Shipper ngay khi đơn chuyển sang DELIVERED.
     * Idempotent dựa trên order.shipperPaidAt (chỉ thanh toán 1 lần duy nhất).
     */
    void releaseShipperFeeOnDelivered(int orderId);
}
