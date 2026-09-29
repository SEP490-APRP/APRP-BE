package com.sang.be_capnong.controller;

import com.sang.be_capnong.dto.request.ShippingFeeRequest;
import com.sang.be_capnong.dto.response.ApiResponse;
import com.sang.be_capnong.dto.response.ShippingFeeResponse;
import com.sang.be_capnong.service.ShippingFeeService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  ShippingController — REST API xem trước phí vận chuyển                ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  BASE URL: /shipping                                                    ║
 * ║                                                                         ║
 * ║  Endpoint duy nhất:                                                     ║
 * ║    POST /shipping/preview                                               ║
 * ║    Body: { "address": "..." }                                           ║
 * ║    Response: { distanceKm: 7.54, shippingFee: 20000 }                  ║
 * ║                                                                         ║
 * ║  LUỒNG ĐẦY ĐỦ:                                                         ║
 * ║                                                                         ║
 * ║  [Client] POST /shipping/preview                                        ║
 * ║       │                                                                 ║
 * ║       ▼                                                                 ║
 * ║  [ShippingController]                                                   ║
 * ║       │  validate request (address không rỗng)                          ║
 * ║       │                                                                 ║
 * ║                                                                         ║
 * ║  [ShippingFeeService.calculateShippingFee()]                            ║
 * ║       │  validate request trực tiếp trong service                       ║
 * ║       │                                                                 ║
 * ║       ├─① userRepository.findById(shopId) → shopAddress                ║
 * ║       │     → Google Geocoding API → [shopLat, shopLng]                 ║
 * ║       │                                                                 ║
 * ║       ├─② GoogleMapService.geocodeAddress(buyerAddress)                ║
 * ║       │     → Google Geocoding API → [buyerLat, buyerLng]              ║
 * ║       │                                                                 ║
 * ║       ├─③ GoogleMapService.getDistanceKm(shop, buyer)                  ║
 * ║       │     → Google Distance Matrix API → distanceKm                   ║
 * ║       │                                                                 ║
 * ║       └─④ calcFeeByDistance(distanceKm) → shippingFee                  ║
 * ║                                                                         ║
 * ║       ▼                                                                 ║
 * ║  [Client] ← { distanceKm: 7.54, shippingFee: 20000 }                  ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
@Slf4j
@RestController
@RequestMapping("/shipping")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ShippingController {

    ShippingFeeService shippingFeeService;
    com.sang.be_capnong.service.GoogleMapService googleMapService;

    /**
     * POST /shipping/preview
     *
     * Xem trước phí vận chuyển trước khi đặt hàng.
     * Cần có điểm xuất phát (shopId) và điểm đến (buyerAddress hoặc buyerPlaceId).
     *
     * Request Body:
     * {
     *   "shopId": 12,
     *   "buyerAddress": "99 Nguyễn Huệ, Quận 1, TP Hồ Chí Minh",
     *   "buyerPlaceId": "ChIJD7fiBh9u5kcRYJSMaMOCCwQ" 
     * }
     *
     * Response thành công:
     * {
     *   "code": 1000,
     *   "result": {
     *     "distanceKm": 7.54,
     *     "shippingFee": 20000
     *   }
     * }
     *
     * Response khi địa chỉ không xác định được (fallback):
     * {
     *   "code": 1000,
     *   "result": {
     *     "shippingFee": 15000,
     *     "note": "Không xác định được địa chỉ giao hàng, áp dụng phí mặc định"
     *   }
     * }
     */
    @PostMapping("/preview")
    public ApiResponse<ShippingFeeResponse> previewShippingFee(
            @RequestBody ShippingFeeRequest request
    ) {
        log.info("[ShippingController] Nhận yêu cầu preview phí ship: shopId={}, buyerAddress='{}', buyerPlaceId='{}'",
                request.getShopId(), request.getBuyerAddress(), request.getBuyerPlaceId());

        // Gọi service tính phí (đã wrap toàn bộ logic validate bên trong)
        ShippingFeeResponse response = shippingFeeService.calculateShippingFee(request);

        return ApiResponse.<ShippingFeeResponse>builder()
                .result(response)
                .build();
    }

    /**
     * GET /shipping/address-suggestions?input=...
     *
     * Gợi ý địa chỉ realtime từ chuỗi người dùng nhập vào sử dụng Google Places Autocomplete.
     */
    @GetMapping("/address-suggestions")
    public ApiResponse<java.util.List<com.sang.be_capnong.dto.response.AddressSuggestionResponse>> getAddressSuggestions(
            @RequestParam String input
    ) {
        log.info("[ShippingController] Gợi ý địa chỉ cho input='{}'", input);

        return ApiResponse.<java.util.List<com.sang.be_capnong.dto.response.AddressSuggestionResponse>>builder()
                .result(googleMapService.getAddressSuggestions(input))
                .build();
    }
}
