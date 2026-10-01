package com.sang.be_capnong.controller;

import com.sang.be_capnong.dto.request.CheckoutPreviewRequest;
import com.sang.be_capnong.dto.request.CreateOrderRequest;
import com.sang.be_capnong.dto.response.ApiResponse;
import com.sang.be_capnong.dto.response.CheckoutPreviewResponse;
import com.sang.be_capnong.dto.response.OrderResponse;
import com.sang.be_capnong.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Order State Machine", description = "APIs for creating orders and transitioning order states")
public class OrderController {

    OrderService orderService;

    // ── XEM TRƯỚC VÀ TẠO ĐƠN HÀNG ──────────────────────────────────────────
    @Operation(summary = "Xem trước đơn hàng", description = "Tính toán chi phí, phí ship, voucher cho đơn hàng.")
    @PostMapping("/preview")
    public ApiResponse<CheckoutPreviewResponse> previewCheckout(@RequestBody @Valid CheckoutPreviewRequest request) {
        return ApiResponse.<CheckoutPreviewResponse>builder()
                .result(orderService.previewCheckout(request))
                .message("Checkout preview.")
                .build();
    }

    @Operation(summary = "Buyer places an order",
            description = "Creates a new order. Status = PENDING (COD) or PAID (TRANSFER). Splits into multiple orders if items belong to multiple shops.")
    @PostMapping
    public ApiResponse<java.util.List<OrderResponse>> createOrder(@RequestBody @Valid CreateOrderRequest request) {
        return ApiResponse.<java.util.List<OrderResponse>>builder()
                .result(orderService.createOrder(request))
                .message("Order placed successfully.")
                .build();
    }

    @Operation(summary = "Mark Order as Confirmed", description = "Shop confirms the incoming order. Transitions state from PENDING/PAID to CONFIRMED.")
    @PutMapping("/{id}/confirm")
    public ApiResponse<OrderResponse> markOrderAsConfirmed(@PathVariable("id") int orderId) {
        return ApiResponse.<OrderResponse>builder()
                .result(orderService.markOrderAsConfirmed(orderId))
                .message("Order is confirmed.")
                .build();
    }

    @Operation(summary = "Mark Order as Ready", description = "Shop finished packing. Transitions state from CONFIRMED to READY.")
    @PutMapping("/{id}/ready")
    public ApiResponse<OrderResponse> markOrderAsReady(@PathVariable("id") int orderId) {
        return ApiResponse.<OrderResponse>builder()
                .result(orderService.markOrderAsReady(orderId))
                .message("Order is ready for pickup.")
                .build();
    }

    @Operation(summary = "Mark Order as Shipping", description = "Shipper picks up the order. Transitions state from READY to SHIPPING.")
    @PutMapping("/{id}/ship")
    public ApiResponse<OrderResponse> markOrderAsShipping(@PathVariable("id") int orderId) {
        return ApiResponse.<OrderResponse>builder()
                .result(orderService.markOrderAsShipping(orderId))
                .message("Order is now shipping.")
                .build();
    }

    @Operation(summary = "Mark Order as Delivered", description = "Shipper delivers the order. Transitions state from SHIPPING to DELIVERED.")
    @PutMapping("/{id}/deliver")
    public ApiResponse<OrderResponse> markOrderAsDelivered(@PathVariable("id") int orderId) {
        return ApiResponse.<OrderResponse>builder()
                .result(orderService.markOrderAsDelivered(orderId))
                .message("Order has been delivered.")
                .build();
    }

    @Operation(summary = "Cancel Order", description = "Cancel the order before it ships.")
    @PutMapping("/{id}/cancel")
    public ApiResponse<OrderResponse> markOrderAsCancelled(@PathVariable("id") int orderId) {
        return ApiResponse.<OrderResponse>builder()
                .result(orderService.markOrderAsCancelled(orderId))
                .message("Order has been cancelled.")
                .build();
    }

    @Operation(summary = "Fail Order Delivery", description = "Fail the order delivery due to some reason.")
    @PutMapping("/{id}/fail")
    public ApiResponse<OrderResponse> markOrderAsFailed(@PathVariable("id") int orderId) {
        return ApiResponse.<OrderResponse>builder()
                .result(orderService.markOrderAsFailed(orderId))
                .message("Order delivery failed.")
                .build();
    }
}
