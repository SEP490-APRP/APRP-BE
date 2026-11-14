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
    OrderResponse markOrderAsPickedUp(int orderId);
    /** Shipper giao thành công, bắt buộc ảnh bằng chứng */
    OrderResponse markOrderAsDelivered(int orderId, com.sang.be_capnong.dto.request.DeliverOrderRequest request);
    OrderResponse markOrderAsCancelled(int orderId);
    /** Shipper báo giao thất bại: lý do, số cuộc gọi, ảnh bằng chứng */
    OrderResponse markOrderAsFailed(int orderId, com.sang.be_capnong.dto.request.FailOrderRequest request);
    /** BUYER xác nhận đã nhận hàng: DELIVERED -> COMPLETED, sau đó chia tiền vào ví */
    OrderResponse markOrderAsCompleted(int orderId);
    /** Hệ thống tự hoàn tất đơn DELIVERED quá hạn (không kiểm tra người gọi) */
    OrderResponse autoCompleteOrder(int orderId);

    // === SHIPPER ===
    /** Đơn READY chưa có người nhận trong bán kính quanh shipper, gần nhất trước. Bắt buộc đang trực tuyến */
    org.springframework.data.domain.Page<OrderResponse> getReadyOrders(int page, int size, Double latitude, Double longitude);
    org.springframework.data.domain.Page<OrderResponse> getShipperOrders(int page, int size, java.util.List<com.sang.be_capnong.enums.OrderStatus> statuses);
}
