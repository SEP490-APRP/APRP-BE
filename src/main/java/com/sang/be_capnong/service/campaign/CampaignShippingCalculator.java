package com.sang.be_capnong.service.campaign;

import com.sang.be_capnong.dto.response.campaign.CampaignShippingQuoteResponse;
import com.sang.be_capnong.entity.Campaign;
import com.sang.be_capnong.service.GoogleMapService;
import com.sang.be_capnong.util.GeoUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Phí giao hàng chiến dịch giải cứu: biết trước khi đặt, chỉ có thể giảm sau đó.
 * <pre>
 * 1. Phí gốc theo khoảng cách từ điểm tập kết: < 5km 15k · 5–10km 20k · 10–20km 30k · > 20km 30k + 3k/km
 * 2. Giảm theo tổng kg của đơn: < halfKg trả đủ · từ halfKg giảm 50% · từ freeKg miễn phí trong bán kính
 *    freeRadiusKm (xa hơn giảm 50%)
 * 3. Tự đến điểm tập kết lấy: 0đ
 * 4. Gom đơn: phường có từ groupDiscountMinOrders đơn trở lên giảm thêm 50% phí giao (áp dụng lúc chốt)
 * </pre>
 * Các mốc kg / bán kính / số đơn lấy theo từng chiến dịch (null = mặc định).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CampaignShippingCalculator {

    public static final String DELIVERY = "DELIVERY";
    public static final String PICKUP = "PICKUP";

    public static final BigDecimal DEFAULT_HALF_KG = BigDecimal.TEN;
    public static final BigDecimal DEFAULT_FREE_KG = BigDecimal.valueOf(20);
    public static final double DEFAULT_FREE_RADIUS_KM = 10.0;
    public static final int DEFAULT_GROUP_MIN_ORDERS = 5;

    private final GoogleMapService googleMapService;

    @Value("${capnong.campaign.ship.fee-under-5km:15000}")
    private long feeUnder5 = 15000;
    @Value("${capnong.campaign.ship.fee-5-10km:20000}")
    private long fee5to10 = 20000;
    @Value("${capnong.campaign.ship.fee-10-20km:30000}")
    private long fee10to20 = 30000;
    @Value("${capnong.campaign.ship.fee-per-km-over-20:3000}")
    private long feePerKmOver20 = 3000;

    public static BigDecimal halfKg(Campaign c) {
        return c.getShipHalfKg() != null && c.getShipHalfKg().signum() > 0 ? c.getShipHalfKg() : DEFAULT_HALF_KG;
    }

    public static BigDecimal freeKg(Campaign c) {
        return c.getShipFreeKg() != null && c.getShipFreeKg().signum() > 0 ? c.getShipFreeKg() : DEFAULT_FREE_KG;
    }

    public static double freeRadiusKm(Campaign c) {
        return c.getShipFreeRadiusKm() != null && c.getShipFreeRadiusKm() > 0 ? c.getShipFreeRadiusKm() : DEFAULT_FREE_RADIUS_KM;
    }

    public static int groupMinOrders(Campaign c) {
        return c.getGroupDiscountMinOrders() != null && c.getGroupDiscountMinOrders() > 0
                ? c.getGroupDiscountMinOrders() : DEFAULT_GROUP_MIN_ORDERS;
    }

    public static boolean pickupEnabled(Campaign c) {
        return c.getPickupEnabled() == null || c.getPickupEnabled();
    }

    /** Phí gốc theo khoảng cách (chưa giảm) */
    public BigDecimal baseFee(double km) {
        long fee;
        if (km < 5) fee = feeUnder5;
        else if (km <= 10) fee = fee5to10;
        else if (km <= 20) fee = fee10to20;
        else fee = fee10to20 + (long) Math.ceil(km - 20) * feePerKmOver20;
        return BigDecimal.valueOf(fee);
    }

    /** Phí giao sau khi giảm theo tổng kg (chưa tính giảm gom đơn) */
    public BigDecimal fee(Campaign c, BigDecimal totalKg, double km) {
        BigDecimal base = baseFee(km);
        BigDecimal kg = totalKg == null ? BigDecimal.ZERO : totalKg;
        if (kg.compareTo(freeKg(c)) >= 0) {
            return km <= freeRadiusKm(c) ? BigDecimal.ZERO : half(base);
        }
        if (kg.compareTo(halfKg(c)) >= 0) {
            return half(base);
        }
        return base;
    }

    /** Khoảng cách đường đi từ điểm tập kết tới người mua; null nếu thiếu toạ độ */
    public Double distanceKm(Campaign c, Double lat, Double lng, String address) {
        double[] site = siteCoords(c);
        double[] dest = lat != null && lng != null ? new double[]{lat, lng} : geocode(address);
        if (site == null || dest == null) return null;
        double km = Double.MAX_VALUE;
        try {
            km = googleMapService.getDistanceKm(site[0], site[1], dest[0], dest[1]);
        } catch (Exception e) {
            log.warn("[CampaignShip] Distance Matrix failed: {}", e.getMessage());
        }
        if (km == Double.MAX_VALUE || Double.isNaN(km)) {
            km = GeoUtils.roadKm(site[0], site[1], dest[0], dest[1]);
        }
        return Math.round(km * 10.0) / 10.0;
    }

    public CampaignShippingQuoteResponse quote(Campaign c, BigDecimal totalKg, String method, Double lat, Double lng, String address) {
        BigDecimal half = halfKg(c);
        BigDecimal free = freeKg(c);
        CampaignShippingQuoteResponse.CampaignShippingQuoteResponseBuilder b = CampaignShippingQuoteResponse.builder()
                .fulfillmentMethod(PICKUP.equalsIgnoreCase(method) ? PICKUP : DELIVERY)
                .totalKg(totalKg)
                .halfShipKg(half)
                .freeShipKg(free)
                .freeShipRadiusKm(freeRadiusKm(c))
                .groupDiscountMinOrders(groupMinOrders(c))
                .pickupEnabled(pickupEnabled(c))
                .collectionSiteName(c.getCollectionSiteName())
                .collectionSiteAddress(c.getCollectionSiteAddress());
        if (PICKUP.equalsIgnoreCase(method)) {
            return b.shippingFee(BigDecimal.ZERO).baseFee(BigDecimal.ZERO).discountPercent(100)
                    .note("Tự đến điểm tập kết lấy hàng: miễn phí").build();
        }
        Double km = distanceKm(c, lat, lng, address);
        if (km == null) {
            return b.note("Chọn địa chỉ giao hàng để tính phí").build();
        }
        BigDecimal base = baseFee(km);
        BigDecimal fee = fee(c, totalKg, km);
        int discount = base.signum() == 0 ? 0 : 100 - fee.multiply(BigDecimal.valueOf(100)).divide(base, 0, RoundingMode.HALF_UP).intValue();
        BigDecimal kg = totalKg == null ? BigDecimal.ZERO : totalKg;
        // Gợi ý mua thêm để được giảm tiếp
        BigDecimal nextKg = null;
        String nextLabel = null;
        if (kg.compareTo(half) < 0) {
            nextKg = half.subtract(kg);
            nextLabel = "giảm 50% phí giao";
        } else if (kg.compareTo(free) < 0 && km <= freeRadiusKm(c)) {
            nextKg = free.subtract(kg);
            nextLabel = "miễn phí giao hàng";
        }
        return b.distanceKm(km).baseFee(base).shippingFee(fee).discountPercent(discount)
                .nextTierKg(nextKg).nextTierLabel(nextLabel)
                .note(fee.signum() == 0 ? "Miễn phí giao hàng" : discount > 0 ? "Đã giảm " + discount + "% phí giao" : null)
                .build();
    }

    private double[] siteCoords(Campaign c) {
        if (c.getCollectionSiteLatitude() != null && c.getCollectionSiteLongitude() != null) {
            return new double[]{c.getCollectionSiteLatitude(), c.getCollectionSiteLongitude()};
        }
        return geocode(c.getCollectionSiteAddress());
    }

    private double[] geocode(String address) {
        if (address == null || address.isBlank()) return null;
        try {
            double[] r = googleMapService.geocodeAddress(address);
            return r != null && r.length == 2 ? r : null;
        } catch (Exception e) {
            log.warn("[CampaignShip] Geocode failed for '{}': {}", address, e.getMessage());
            return null;
        }
    }

    private static BigDecimal half(BigDecimal v) {
        // Làm tròn lên nghìn đồng
        return v.divide(BigDecimal.valueOf(2000), 0, RoundingMode.CEILING).multiply(BigDecimal.valueOf(1000));
    }
}
