package com.sang.be_capnong.service.impl;

import com.sang.be_capnong.entity.User;
import com.sang.be_capnong.repository.UserRepository;
import com.sang.be_capnong.dto.request.ShippingFeeRequest;
import com.sang.be_capnong.dto.response.ShippingFeeResponse;
import com.sang.be_capnong.service.GoogleMapService;
import com.sang.be_capnong.service.ShippingFeeService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * ╔═══════════════════════════════════════════════════════════════════════════╗
 * ║  ShippingFeeServiceImpl — Tính phí vận chuyển dựa trên khoảng cách      ║
 * ╠═══════════════════════════════════════════════════════════════════════════╣
 * ║                                                                          ║
 * ║  Quy trình tổng quát (4 bước):                                          ║
 * ║                                                                          ║
 * ║  ① Geocode địa chỉ SHOP → [shopLat, shopLng]                            ║
 * ║     (Lấy địa chỉ của shop từ DB theo shopId)                            ║
 * ║                                                                          ║
 * ║  ② Geocode địa chỉ BUYER → [buyerLat, buyerLng]                         ║
 * ║     (Gọi Google Geocoding API cho địa chỉ người mua)                    ║
 * ║                                                                          ║
 * ║  ③ Tính khoảng cách SHOP → BUYER                                         ║
 * ║     (Gọi Google Distance Matrix API, trả về km theo đường thực tế)      ║
 * ║                                                                          ║
 * ║  ④ Áp bảng giá vận chuyển:                                               ║
 * ║     - < 5 km   : 15,000đ                                                 ║
 * ║     - 5–10 km  : 20,000đ                                                 ║
 * ║     - > 10 km  : 20,000đ + 4,000đ/km vượt quá 10km                      ║
 * ║     → Làm tròn lên 1,000đ gần nhất                                      ║
 * ║                                                                          ║
 * ║  Fallback: Nếu bất kỳ bước nào thất bại → trả phí mặc định 15,000đ     ║
 * ╚═══════════════════════════════════════════════════════════════════════════╝
 */
@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ShippingFeeServiceImpl implements ShippingFeeService {

    GoogleMapService googleMapService;
    UserRepository userRepository;

    private static final BigDecimal DEFAULT_FEE = BigDecimal.valueOf(15_000);

    // ═══════════════════════════════════════════════════════════════════════
    // PUBLIC METHODS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Tính phí ship từ địa chỉ shop đến địa chỉ buyer.
     * Tự động ưu tiên buyerPlaceId nếu có.
     *
     * @param request Chứa shopId, buyerAddress và buyerPlaceId
     */
    @Override
    public ShippingFeeResponse calculateShippingFee(ShippingFeeRequest request) {
        // Validate request basic logic
        if (request.getShopId() == null) {
            log.warn("[ShippingFee] Thiếu shopId trong request");
            return fallbackResponse("Vui lòng cung cấp shopId để xem phí chính xác");
        }

        boolean hasAddress = request.getBuyerAddress() != null && !request.getBuyerAddress().isBlank();
        boolean hasPlaceId = request.getBuyerPlaceId() != null && !request.getBuyerPlaceId().isBlank();

        if (!hasAddress && !hasPlaceId) {
            log.warn("[ShippingFee] Cả buyerAddress và buyerPlaceId đều trống");
            return fallbackResponse("Vui lòng nhập địa chỉ giao hàng (hoặc placeId) để xem phí chính xác");
        }

        Integer shopId = request.getShopId();
        String buyerAddress = request.getBuyerAddress();
        String buyerPlaceId = request.getBuyerPlaceId();

        log.info("[ShippingFee] Bắt đầu tính phí: shopId={}, buyerAddress='{}', buyerPlaceId='{}'", 
                shopId, buyerAddress, buyerPlaceId);

        // ── Bước 1: Lấy địa chỉ shop và Geocode ──────────────────────────
        User shopUser = userRepository.findById(shopId).orElse(null);
        if (shopUser == null || shopUser.getAddress() == null || shopUser.getAddress().isBlank()) {
            log.warn("[ShippingFee] Không tìm thấy shop hoặc shop chưa thiết lập địa chỉ (shopId={})", shopId);
            return fallbackResponse("Không xác định được địa chỉ của shop, áp dụng phí mặc định");
        }
        
        String shopAddress = shopUser.getAddress();
        double[] shopCoords = googleMapService.geocodeAddress(shopAddress);
        if (shopCoords == null) {
            log.warn("[ShippingFee] Không geocode được địa chỉ shop='{}'", shopAddress);
            return fallbackResponse("Không xác định được vị trí shop, áp dụng phí mặc định");
        }

        // ── Bước 2: Geocode địa chỉ buyer (destination) ───────────────────
        double[] buyerCoords = null;
        if (buyerPlaceId != null && !buyerPlaceId.isBlank()) {
            buyerCoords = googleMapService.geocodePlaceId(buyerPlaceId);
            if (buyerCoords == null) {
                log.warn("[ShippingFee] Không geocode được buyerPlaceId='{}', fallback sang address", buyerPlaceId);
            }
        }
        
        if (buyerCoords == null && buyerAddress != null && !buyerAddress.isBlank()) {
            buyerCoords = googleMapService.geocodeAddress(buyerAddress);
        }

        if (buyerCoords == null) {
            log.warn("[ShippingFee] Không geocode được địa chỉ buyer='{}'", buyerAddress);
            return fallbackResponse("Không xác định được địa chỉ giao hàng, áp dụng phí mặc định");
        }

        // ── Bước 3 + 4: Tính khoảng cách → tính phí ──────────────────────
        return computeFee(shopCoords, buyerCoords);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Gọi Distance Matrix API lấy khoảng cách, rồi áp bảng phí.
     *
     * @param shop      double[]{lat, lng} của shop
     * @param buyer     double[]{lat, lng} của buyer
     */
    private ShippingFeeResponse computeFee(double[] shop, double[] buyer) {
        // Bước 3: Gọi Google Distance Matrix
        double distanceKm = googleMapService.getDistanceKm(
                shop[0], shop[1],   // origin: shop
                buyer[0], buyer[1]  // destination: buyer
        );

        // Kiểm tra kết quả bất thường
        if (distanceKm == Double.MAX_VALUE) {
            log.warn("[ShippingFee] Không tính được khoảng cách từ Google Maps");
            return fallbackResponse("Không tính được khoảng cách, áp dụng phí mặc định");
        }

        if (distanceKm > 150.0) {
            log.warn("[ShippingFee] Khoảng cách quá lớn ({} km), có thể địa chỉ sai", distanceKm);
            return fallbackResponse("Địa chỉ giao hàng có vẻ ở quá xa (> 150km), áp dụng phí mặc định");
        }

        // Bước 4: Áp bảng giá
        BigDecimal fee = calcFeeByDistance(distanceKm);

        log.info("[ShippingFee] Hoàn thành: distanceKm={} km → shippingFee={} đ",
                String.format("%.2f", distanceKm), fee.toPlainString());

        return ShippingFeeResponse.builder()
                .distanceKm(Math.round(distanceKm * 100.0) / 100.0) // làm tròn 2 chữ số
                .shippingFee(fee)
                .build();
    }

    /**
     * Bảng phí vận chuyển theo khoảng cách (đường thực tế):
     *
     * ┌──────────────┬─────────────────────────────────────────┐
     * │ Khoảng cách  │ Phí                                     │
     * ├──────────────┼─────────────────────────────────────────┤
     * │ < 5 km       │ 15,000đ                                 │
     * │ 5 – 10 km    │ 20,000đ                                 │
     * │ > 10 km      │ 20,000đ + 4,000đ × (km vượt quá 10km)  │
     * └──────────────┴─────────────────────────────────────────┘
     *
     * Kết quả làm tròn lên đến 1,000đ gần nhất (Math.ceil).
     *
     * Ví dụ:
     *   3 km  → 15,000đ
     *   7 km  → 20,000đ
     *  15 km  → 20,000 + (15-10) × 4,000 = 40,000đ
     *  12.3km → 20,000 + 2.3 × 4,000 = 29,200 → làm tròn → 30,000đ
     */
    private BigDecimal calcFeeByDistance(double distanceKm) {
        double fee;

        if (distanceKm < 5.0) {
            fee = 15_000;
        } else if (distanceKm <= 10.0) {
            fee = 20_000;
        } else {
            fee = 20_000 + (distanceKm - 10.0) * 4_000;
        }

        // Làm tròn lên 1,000đ gần nhất
        long rounded = (long) (Math.ceil(fee / 1_000.0) * 1_000);

        log.info("[ShippingFee][calcFee] distanceKm={} → fee_raw={:.0f} → fee_rounded={}",
                String.format("%.2f", distanceKm), fee, rounded);

        return BigDecimal.valueOf(rounded);
    }

    /** Tạo response fallback với phí mặc định và ghi chú lý do. */
    private ShippingFeeResponse fallbackResponse(String note) {
        return ShippingFeeResponse.builder()
                .shippingFee(DEFAULT_FEE)
                .note(note)
                .build();
    }
}
