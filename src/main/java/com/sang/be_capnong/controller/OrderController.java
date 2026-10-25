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
            description = "Creates a new order. Status = PENDING. Splits into multiple orders if items belong to multiple shops. Clears the cart after success.")
    @PostMapping
    @PreAuthorize("hasRole('BUYER')")
    public ApiResponse<List<OrderResponse>> createOrder(@RequestBody @Valid CreateOrderRequest request) {
        return ApiResponse.<List<OrderResponse>>builder()
                .result(orderService.createOrder(request))
                .message("Order placed successfully.")
                .build();
    }

    @Operation(summary = "Xem chi tiết đơn hàng")
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<OrderResponse> getOrderById(@PathVariable("id") int orderId) {
        return ApiResponse.<OrderResponse>builder()
                .result(orderService.getOrderById(orderId))
                .message("Order detail retrieved.")
                .build();
    }

    @Operation(summary = "Danh sách đơn hàng của tôi", description = "[BUYER] Lấy danh sách đơn hàng đã đặt")
    @GetMapping("/my")
    @PreAuthorize("hasRole('BUYER')")
    public ApiResponse<org.springframework.data.domain.Page<OrderResponse>> getMyOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) List<com.sang.be_capnong.enums.OrderStatus> statuses
    ) {
        return ApiResponse.<org.springframework.data.domain.Page<OrderResponse>>builder()
                .result(orderService.getMyOrders(page, size, statuses))
                .message("My orders retrieved.")
                .build();
    }

    @Operation(summary = "Danh sách đơn hàng của Shop", description = "[SHOP only] Lấy danh sách đơn hàng cần xử lý (PENDING, PAID...)")
    @GetMapping("/shop")
    @PreAuthorize("hasRole('SHOP')")
    public ApiResponse<org.springframework.data.domain.Page<OrderResponse>> getShopOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) List<com.sang.be_capnong.enums.OrderStatus> statuses
    ) {
        return ApiResponse.<org.springframework.data.domain.Page<OrderResponse>>builder()
                .result(orderService.getShopOrders(page, size, statuses))
                .message("Shop orders retrieved.")
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
    @Operation(summary = "Shipper fetches ready orders",
            description = "[SHIPPER only] Lấy danh sách các đơn READY để shipper nhận đơn.")
    @GetMapping("/ready")
    @PreAuthorize("hasRole('SHIPPER')")
    public ApiResponse<org.springframework.data.domain.Page<OrderResponse>> getReadyOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.<org.springframework.data.domain.Page<OrderResponse>>builder()
                .result(orderService.getReadyOrders(page, size))
                .message("Ready orders retrieved.")
                .build();
    }

    @Operation(summary = "Shipper fetches my orders",
            description = "[SHIPPER only] Lấy danh sách các đơn mà shipper đang giao hoặc đã giao.")
    @GetMapping("/shipper/my")
    @PreAuthorize("hasRole('SHIPPER')")
    public ApiResponse<org.springframework.data.domain.Page<OrderResponse>> getShipperOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) List<com.sang.be_capnong.enums.OrderStatus> statuses
    ) {
        return ApiResponse.<org.springframework.data.domain.Page<OrderResponse>>builder()
                .result(orderService.getShipperOrders(page, size, statuses))
                .message("Shipper orders retrieved.")
                .build();
    }

    @Operation(summary = "Shipper accepts and heads to shop",
            description = "[SHIPPER only] Shipper accepts the order and moves to shop. Transitions: READY → SHIPPING.")
    @PutMapping("/{id}/ship")
    @PreAuthorize("hasRole('SHIPPER')")
    public ApiResponse<OrderResponse> markOrderAsShipping(@PathVariable("id") int orderId) {
        return ApiResponse.<OrderResponse>builder()
                .result(orderService.markOrderAsShipping(orderId))
                .message("Order is now shipping.")
                .build();
    }

    @Operation(summary = "Shipper picks up order from shop",
            description = "[SHIPPER only] Shipper arrives at shop and picks up the goods. Transitions: SHIPPING → PICKED_UP.")
    @PutMapping("/{id}/pickup")
    @PreAuthorize("hasRole('SHIPPER')")
    public ApiResponse<OrderResponse> markOrderAsPickedUp(@PathVariable("id") int orderId) {
        return ApiResponse.<OrderResponse>builder()
                .result(orderService.markOrderAsPickedUp(orderId))
                .message("Order has been picked up from shop.")
                .build();
    }

    @Operation(summary = "Shipper confirms delivery",
            description = "[SHIPPER only] Order delivered successfully. Transitions: PICKED_UP (or SHIPPING) → DELIVERED.")
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
