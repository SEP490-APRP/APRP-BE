package com.sang.be_capnong.controller;

import com.sang.be_capnong.dto.request.AdminReturnDecisionRequest;
import com.sang.be_capnong.dto.request.ReturnCreateRequest;
import com.sang.be_capnong.dto.request.ShipperReturnActionRequest;
import com.sang.be_capnong.dto.request.ShopReturnRespondRequest;
import com.sang.be_capnong.dto.response.ApiResponse;
import com.sang.be_capnong.dto.response.PageResponse;
import com.sang.be_capnong.dto.response.ReturnRequestResponse;
import com.sang.be_capnong.enums.ReturnStatus;
import com.sang.be_capnong.service.ReturnRequestService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/returns")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ReturnRequestController {

    ReturnRequestService returnRequestService;

    /**
     * Người mua tạo yêu cầu hoàn hàng / hoàn tiền
     */
    @PostMapping
    public ApiResponse<ReturnRequestResponse> createReturnRequest(@RequestBody @Valid ReturnCreateRequest request) {
        log.info("Create return request received for orderDetailId={}", request.getOrderDetailId());
        return ApiResponse.<ReturnRequestResponse>builder()
                .result(returnRequestService.createReturnRequest(request))
                .build();
    }

    /**
     * Shop phản hồi yêu cầu hoàn hàng (Đồng ý hoặc Từ chối -> tranh chấp)
     */
    @PostMapping("/{id}/respond")
    public ApiResponse<ReturnRequestResponse> shopRespond(
            @PathVariable("id") int id,
            @RequestBody @Valid ShopReturnRespondRequest request
    ) {
        log.info("Shop respond to return request id={}", id);
        return ApiResponse.<ReturnRequestResponse>builder()
                .result(returnRequestService.shopRespond(id, request))
                .build();
    }

    /**
     * Admin ra quyết định phân xử khiếu nại
     */
    @PostMapping("/{id}/admin-decision")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ReturnRequestResponse> adminDecision(
            @PathVariable("id") int id,
            @RequestBody @Valid AdminReturnDecisionRequest request
    ) {
        log.info("Admin decision on return request id={}", id);
        return ApiResponse.<ReturnRequestResponse>builder()
                .result(returnRequestService.adminDecision(id, request))
                .build();
    }

    /**
     * Endpoint tương thích trực tiếp với FE Disputes.tsx (adminAction)
     */
    @PostMapping("/admin-action/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Map<String, Object>> adminAction(
            @PathVariable("id") int id,
            @RequestBody Map<String, Object> payload
    ) {
        boolean accept = Boolean.TRUE.equals(payload.get("accept"));
        String responseText = payload.get("response") != null ? payload.get("response").toString() : "";
        BigDecimal refundAmount = payload.get("refundAmount") != null
                ? new BigDecimal(payload.get("refundAmount").toString())
                : null;

        AdminReturnDecisionRequest req = AdminReturnDecisionRequest.builder()
                .accept(accept)
                .adminRemark(responseText)
                .refundAmount(refundAmount)
                .isReturnGoods(false) // Mặc định xử lý hoàn tiền nếu gọi từ giao diện dispute hiện tại
                .build();

        ReturnRequestResponse result = returnRequestService.adminDecision(id, req);
        return ApiResponse.<Map<String, Object>>builder()
                .result(Map.of("request", result))
                .build();
    }

    /**
     * Shipper nhận đơn giao hàng hoàn trả
     */
    @PostMapping("/{id}/shipper/accept")
    public ApiResponse<ReturnRequestResponse> shipperAccept(@PathVariable("id") int id) {
        log.info("Shipper accept return delivery for id={}", id);
        return ApiResponse.<ReturnRequestResponse>builder()
                .result(returnRequestService.shipperAccept(id))
                .build();
    }

    /**
     * Shipper xác nhận đã lấy hàng hoàn từ Người mua
     */
    @PostMapping("/{id}/shipper/pickup")
    public ApiResponse<ReturnRequestResponse> shipperPickup(
            @PathVariable("id") int id,
            @RequestBody(required = false) ShipperReturnActionRequest request
    ) {
        log.info("Shipper picked up return goods for id={}", id);
        return ApiResponse.<ReturnRequestResponse>builder()
                .result(returnRequestService.shipperPickup(id, request != null ? request : new ShipperReturnActionRequest()))
                .build();
    }

    /**
     * Shipper xác nhận đã giao hàng hoàn trả về cho Shop
     */
    @PostMapping("/{id}/shipper/deliver")
    public ApiResponse<ReturnRequestResponse> shipperDeliver(
            @PathVariable("id") int id,
            @RequestBody(required = false) ShipperReturnActionRequest request
    ) {
        log.info("Shipper delivered return goods to shop for id={}", id);
        return ApiResponse.<ReturnRequestResponse>builder()
                .result(returnRequestService.shipperDeliver(id, request != null ? request : new ShipperReturnActionRequest()))
                .build();
    }

    /**
     * Người mua hủy yêu cầu hoàn hàng
     */
    @PostMapping("/{id}/cancel")
    public ApiResponse<ReturnRequestResponse> cancelReturnRequest(@PathVariable("id") int id) {
        log.info("Buyer cancelling return request id={}", id);
        return ApiResponse.<ReturnRequestResponse>builder()
                .result(returnRequestService.cancelReturnRequest(id))
                .build();
    }

    /**
     * Lấy chi tiết 1 yêu cầu hoàn hàng
     */
    @GetMapping("/{id}")
    public ApiResponse<ReturnRequestResponse> getReturnRequestById(@PathVariable("id") int id) {
        return ApiResponse.<ReturnRequestResponse>builder()
                .result(returnRequestService.getReturnRequestById(id))
                .build();
    }

    /**
     * Danh sách yêu cầu hoàn hàng của Người mua hiện tại
     */
    @GetMapping("/buyer/my-returns")
    public ApiResponse<PageResponse<ReturnRequestResponse>> getMyBuyerReturns(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) ReturnStatus status
    ) {
        return ApiResponse.<PageResponse<ReturnRequestResponse>>builder()
                .result(returnRequestService.getMyBuyerReturns(page, size, status))
                .build();
    }

    /**
     * Danh sách yêu cầu hoàn hàng gửi đến Shop của người bán hiện tại
     */
    @GetMapping("/shop/my-returns")
    public ApiResponse<PageResponse<ReturnRequestResponse>> getMyShopReturns(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) ReturnStatus status
    ) {
        return ApiResponse.<PageResponse<ReturnRequestResponse>>builder()
                .result(returnRequestService.getMyShopReturns(page, size, status))
                .build();
    }

    /**
     * Danh sách đơn hoàn hàng do Shipper đang phụ trách
     */
    @GetMapping("/shipper/my-returns")
    public ApiResponse<PageResponse<ReturnRequestResponse>> getMyShipperReturns(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) ReturnStatus status
    ) {
        return ApiResponse.<PageResponse<ReturnRequestResponse>>builder()
                .result(returnRequestService.getMyShipperReturns(page, size, status))
                .build();
    }

    /**
     * Danh sách đơn hoàn hàng đang chờ shipper nhận vận chuyển
     */
    @GetMapping("/shipper/available")
    public ApiResponse<PageResponse<ReturnRequestResponse>> getAvailableReturnOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.<PageResponse<ReturnRequestResponse>>builder()
                .result(returnRequestService.getAvailableReturnOrdersForShipper(page, size))
                .build();
    }

    /**
     * Danh sách khiếu nại dành cho Admin (phân trang + lọc trạng thái / từ khóa)
     */
    @GetMapping("/admin/disputes")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageResponse<ReturnRequestResponse>> getAllDisputesForAdmin(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) ReturnStatus status,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.<PageResponse<ReturnRequestResponse>>builder()
                .result(returnRequestService.getAllDisputesForAdmin(page, size, status, keyword))
                .build();
    }

    /**
     * Lấy toàn bộ danh sách khiếu nại (tương thích trực tiếp với getDisputes() của FE)
     */
    @GetMapping("/admin/disputes/list")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<ReturnRequestResponse>> getAllDisputesList() {
        return ApiResponse.<List<ReturnRequestResponse>>builder()
                .result(returnRequestService.getAllDisputesList())
                .build();
    }

    /**
     * Tương thích với autoCheckPayout của return.service.ts
     */
    @GetMapping("/auto-check-payout/{orderCode}")
    public ApiResponse<Boolean> autoCheckPayout(@PathVariable("orderCode") int orderCode) {
        return ApiResponse.<Boolean>builder()
                .result(returnRequestService.autoCheckPayout(orderCode))
                .message("Payout verified for order " + orderCode)
                .build();
    }

    /**
     * Tương thích với checkPayoutStatus của return.service.ts
     */
    @GetMapping("/check-payout-status/{id}")
    public ApiResponse<Map<String, Object>> checkPayoutStatus(@PathVariable("id") int id) {
        ReturnRequestResponse response = returnRequestService.checkPayoutStatus(id);
        return ApiResponse.<Map<String, Object>>builder()
                .result(Map.of("request", response))
                .build();
    }
}
