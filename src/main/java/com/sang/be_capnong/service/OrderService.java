package com.sang.be_capnong.service;

import com.sang.be_capnong.dto.request.CreateOrderRequest;
import com.sang.be_capnong.dto.response.OrderResponse;

public interface OrderService {
    // === TẠO ĐƠN HÀNG ===
    OrderResponse createOrder(CreateOrderRequest request);

    // === STATE MACHINE ===
    OrderResponse markOrderAsConfirmed(int orderId);
    OrderResponse markOrderAsReady(int orderId);
    OrderResponse markOrderAsShipping(int orderId);
    OrderResponse markOrderAsDelivered(int orderId);
    OrderResponse markOrderAsCancelled(int orderId);
    OrderResponse markOrderAsFailed(int orderId);
}
