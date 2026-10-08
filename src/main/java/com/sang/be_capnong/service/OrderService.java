package com.sang.be_capnong.service;

import com.sang.be_capnong.dto.request.CreateOrderRequest;
import com.sang.be_capnong.dto.request.CheckoutPreviewRequest;
import com.sang.be_capnong.dto.response.CheckoutPreviewResponse;
import com.sang.be_capnong.dto.response.OrderResponse;

public interface OrderService {
    CheckoutPreviewResponse previewCheckout(CheckoutPreviewRequest request);
    
    // === TẠO ĐƠN HÀNG ===
    java.util.List<OrderResponse> createOrder(CreateOrderRequest request);

    // === XEM ĐƠN HÀNG ===
    OrderResponse getOrderById(int orderId);
    org.springframework.data.domain.Page<OrderResponse> getMyOrders(int page, int size, java.util.List<com.sang.be_capnong.enums.OrderStatus> statuses);
    org.springframework.data.domain.Page<OrderResponse> getShopOrders(int page, int size, java.util.List<com.sang.be_capnong.enums.OrderStatus> statuses);

    // === STATE MACHINE ===
    OrderResponse markOrderAsConfirmed(int orderId);
    OrderResponse markOrderAsReady(int orderId);
    OrderResponse markOrderAsShipping(int orderId);
    OrderResponse markOrderAsDelivered(int orderId);
    OrderResponse markOrderAsCancelled(int orderId);
    OrderResponse markOrderAsFailed(int orderId);

    // === SHIPPER ===
    org.springframework.data.domain.Page<OrderResponse> getReadyOrders(int page, int size);
    org.springframework.data.domain.Page<OrderResponse> getShipperOrders(int page, int size, java.util.List<com.sang.be_capnong.enums.OrderStatus> statuses);
}
