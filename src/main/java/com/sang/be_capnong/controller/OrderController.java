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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Order State Machine", description = "APIs for creating orders and transitioning order states")
public class OrderController {

    OrderService orderService;

    // ── XEM TRƯỚC VÀ TẠO ĐƠN HÀNG (BUYER) ──────────────────────────────────────────
    @Operation(summary = "Xem trước đơn hàng", description = "Tính toán chi phí, phí ship, voucher cho đơn hàng.")
    @PostMapping("/preview")
    @PreAuthorize("hasRole('BUYER')")
    public ApiResponse<CheckoutPreviewResponse> previewCheckout(@RequestBody @Valid CheckoutPreviewRequest request) {
        return ApiResponse.<CheckoutPreviewResponse>builder()
                .result(orderService.previewCheckout(request))
                .message("Checkout preview.")
                .build();
    }

    @Operation(summary = "Buyer places an order",
            description = "Creates a new order. Status = PENDING (COD) or PAID (TRANSFER). " +
                    "Splits into multiple orders if items belong to multiple shops. Clears the cart after success.")
    @PostMapping
    @PreAuthorize("hasRole('BUYER')")
    public ApiResponse<List<OrderResponse>> createOrder(@RequestBody @Valid CreateOrderRequest request) {
        return ApiResponse.<List<OrderResponse>>builder()
                .result(orderService.createOrder(request))
                .message("Order placed successfully.")
                .build();
    }

    // ── SHOP: Xác nhận & Đóng gói ──────────────────────────────────────
    @Operation(summary = "Shop confirms order",
            description = "[SHOP only] Confirms the incoming order. Transitions: PENDING/PAID → CONFIRMED.")
    @PutMapping("/{id}/confirm")
    @PreAuthorize("hasRole('SHOP')")
    public ApiResponse<OrderResponse> markOrderAsConfirmed(@PathVariable("id") int orderId) {
        return ApiResponse.<OrderResponse>builder()
                .result(orderService.markOrderAsConfirmed(orderId))
                .message("Order is confirmed.")
                .build();
    }

    @Operation(summary = "Shop marks order as ready",
            description = "[SHOP only] Shop finished packing the order. Transitions: CONFIRMED → READY.")
    @PutMapping("/{id}/ready")
    @PreAuthorize("hasRole('SHOP')")
    public ApiResponse<OrderResponse> markOrderAsReady(@PathVariable("id") int orderId) {
        return ApiResponse.<OrderResponse>builder()
                .result(orderService.markOrderAsReady(orderId))
                .message("Order is ready for pickup.")
                .build();
    }

    // ── SHIPPER: Vận chuyển & Giao hàng ──────────────────────────────────
    @Operation(summary = "Shipper starts delivery",
            description = "[SHIPPER only] Shipper picks up the order. Transitions: READY → SHIPPING.")
    @PutMapping("/{id}/ship")
    @PreAuthorize("hasRole('SHIPPER')")
    public ApiResponse<OrderResponse> markOrderAsShipping(@PathVariable("id") int orderId) {
        return ApiResponse.<OrderResponse>builder()
                .result(orderService.markOrderAsShipping(orderId))
                .message("Order is now shipping.")
                .build();
    }

    @Operation(summary = "Shipper confirms delivery",
            description = "[SHIPPER only] Order delivered successfully. Transitions: SHIPPING → DELIVERED.")
    @PutMapping("/{id}/deliver")
    @PreAuthorize("hasRole('SHIPPER')")
    public ApiResponse<OrderResponse> markOrderAsDelivered(@PathVariable("id") int orderId) {
        return ApiResponse.<OrderResponse>builder()
                .result(orderService.markOrderAsDelivered(orderId))
                .message("Order has been delivered.")
                .build();
    }

    @Operation(summary = "Shipper reports failed delivery",
            description = "[SHIPPER only] Delivery failed (no recipient, wrong address, etc.). Transitions: SHIPPING → FAILED. Stock is restored.")
    @PutMapping("/{id}/fail")
    @PreAuthorize("hasRole('SHIPPER')")
    public ApiResponse<OrderResponse> markOrderAsFailed(@PathVariable("id") int orderId) {
        return ApiResponse.<OrderResponse>builder()
                .result(orderService.markOrderAsFailed(orderId))
                .message("Order delivery failed.")
                .build();
    }

    // ── BUYER/SHOP: Hủy đơn ──────────────────────────────────────────
    @Operation(summary = "Cancel order",
            description = "[BUYER or SHOP] Cancel the order before it ships. Transitions: PENDING/PAID/CONFIRMED/READY → CANCELLED. Stock is restored.")
    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasRole('BUYER') or hasRole('SHOP')")
    public ApiResponse<OrderResponse> markOrderAsCancelled(@PathVariable("id") int orderId) {
        return ApiResponse.<OrderResponse>builder()
                .result(orderService.markOrderAsCancelled(orderId))
                .message("Order has been cancelled.")
                .build();
    }
}
