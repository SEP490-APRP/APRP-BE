package com.sang.be_capnong.service;

public interface ReferralSettlementService {

    /**
     * Quyết toán hoa hồng cho toàn bộ các dòng hàng trong đơn hàng đã settle.
     */
    void settleOrderCommissions(int orderId);

    /**
     * Quyết toán cho 1 bản ghi hoa hồng cụ thể (dùng bởi scheduler retry).
     */
    void settleSingleCommission(Long commissionId);
}

