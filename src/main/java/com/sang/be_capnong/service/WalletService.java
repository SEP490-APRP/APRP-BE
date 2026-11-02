package com.sang.be_capnong.service;

import com.sang.be_capnong.dto.response.wallet.OrderSettlementResponse;
import com.sang.be_capnong.dto.response.wallet.RefundDueResponse;
import com.sang.be_capnong.dto.response.wallet.WalletResponse;
import com.sang.be_capnong.dto.response.wallet.WalletTransactionResponse;
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
}
