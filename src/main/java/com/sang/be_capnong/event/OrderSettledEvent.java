package com.sang.be_capnong.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Sự kiện phát ra khi một đơn hàng hoàn tất quyết toán thành công giữa Shop, Shipper và Sàn.
 * Lắng nghe bởi ReferralSettlementListener để quyết toán hoa hồng tiếp thị liên kết (AFTER_COMMIT).
 */
@Getter
public class OrderSettledEvent extends ApplicationEvent {

    private final int orderId;

    public OrderSettledEvent(int orderId) {
        super(orderId);
        this.orderId = orderId;
    }

    public OrderSettledEvent(Object source, int orderId) {
        super(source);
        this.orderId = orderId;
    }
}

