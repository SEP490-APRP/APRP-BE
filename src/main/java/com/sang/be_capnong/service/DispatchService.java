package com.sang.be_capnong.service;

/**
 * Điều phối đơn READY cho Shipper (Bước 1: Assign delivery task):
 * quét shipper đang trực tuyến quanh nhà vườn theo bán kính tăng dần, mời qua thông báo + STOMP,
 * ai bấm nhận trước được đơn. Không ai nhận sau một thời gian thì báo Shop và Admin.
 */
public interface DispatchService {

    /** Gọi khi đơn vừa chuyển READY: chạy đợt 1 sau khi transaction hiện tại commit */
    void dispatchAfterCommit(int orderId);

    /** Scheduler: mở rộng bán kính / quét lại / cảnh báo cho các đơn READY chưa có người nhận */
    void runDispatchCycle();

    /** Gọi khi có shipper nhận đơn: báo các shipper đã được mời gỡ đơn khỏi danh sách (sau commit) */
    void notifyOrderTakenAfterCommit(int orderId, int takenByShipperId);

    /** Báo Buyer / Shop khi đơn đổi trạng thái giao hàng (sau commit) */
    void notifyStatusChangedAfterCommit(int orderId);
}
