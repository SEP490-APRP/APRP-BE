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

/**
 * Phí giao hàng chiến dịch giải cứu: cố định theo tổng kg của đơn, biết trước khi đặt.
 * <pre>
 * Đơn từ 50 kg: miễn phí · từ 30 kg tới dưới 50 kg: 50.000đ · dưới 30 kg: 70.000đ · tự đến điểm tập kết lấy: 0đ
 * </pre>
 * Riêng phí gốc theo khoảng cách ({@link #baseFee}) chỉ dùng để tính cước trả shipper giao chặng cuối.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CampaignShippingCalculator {

    public static final String DELIVERY = "DELIVERY";
    public static final String PICKUP = "PICKUP";

    /** Từ mốc này miễn phí giao */
    public static final BigDecimal FREE_KG = BigDecimal.valueOf(50);
    /** Từ mốc này (tới dưới FREE_KG) thu MID_FEE, nhẹ hơn thu SMALL_FEE */
    public static final BigDecimal MID_KG = BigDecimal.valueOf(30);
    public static final BigDecimal MID_FEE = BigDecimal.valueOf(50_000);
    public static final BigDecimal SMALL_FEE = BigDecimal.valueOf(70_000);

    private final GoogleMapService googleMapService;

    @Value("${capnong.campaign.ship.fee-under-5km:15000}")
    private long feeUnder5 = 15000;
    @Value("${capnong.campaign.ship.fee-5-10km:20000}")
    private long fee5to10 = 20000;
    @Value("${capnong.campaign.ship.fee-10-20km:30000}")
    private long fee10to20 = 30000;
    @Value("${capnong.campaign.ship.fee-per-km-over-20:3000}")
    private long feePerKmOver20 = 3000;

    public static boolean pickupEnabled(Campaign c) {
        return c.getPickupEnabled() == null || c.getPickupEnabled();
    }

    /** Phí theo khoảng cách: cước trả shipper giao chặng cuối */
    public BigDecimal baseFee(double km) {
        long fee;
        if (km < 5) fee = feeUnder5;
        else if (km <= 10) fee = fee5to10;
        else if (km <= 20) fee = fee10to20;
        else fee = fee10to20 + (long) Math.ceil(km - 20) * feePerKmOver20;
        return BigDecimal.valueOf(fee);
    }

    /** Phí giao người mua trả, cố định theo tổng kg của đơn */
    public BigDecimal fee(BigDecimal totalKg) {
        BigDecimal kg = totalKg == null ? BigDecimal.ZERO : totalKg;
        if (kg.compareTo(FREE_KG) >= 0) return BigDecimal.ZERO;
        return kg.compareTo(MID_KG) >= 0 ? MID_FEE : SMALL_FEE;
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
        CampaignShippingQuoteResponse.CampaignShippingQuoteResponseBuilder b = CampaignShippingQuoteResponse.builder()
                .fulfillmentMethod(PICKUP.equalsIgnoreCase(method) ? PICKUP : DELIVERY)
                .totalKg(totalKg)
                .halfShipKg(MID_KG)
                .freeShipKg(FREE_KG)
                .midShipFee(MID_FEE)
                .smallShipFee(SMALL_FEE)
                .pickupEnabled(pickupEnabled(c))
                .collectionSiteName(c.getCollectionSiteName())
                .collectionSiteAddress(c.getCollectionSiteAddress());
        if (PICKUP.equalsIgnoreCase(method)) {
            return b.shippingFee(BigDecimal.ZERO).baseFee(BigDecimal.ZERO).discountPercent(100)
                    .note("Tự đến điểm tập kết lấy hàng: miễn phí").build();
        }
        BigDecimal kg = totalKg == null ? BigDecimal.ZERO : totalKg;
        BigDecimal fee = fee(kg);
        // Gợi ý mua thêm để được mức phí thấp hơn
        BigDecimal nextKg = null;
        String nextLabel = null;
        if (kg.compareTo(MID_KG) < 0) {
            nextKg = MID_KG.subtract(kg);
            nextLabel = "phí giao còn 50.000đ";
        } else if (kg.compareTo(FREE_KG) < 0) {
            nextKg = FREE_KG.subtract(kg);
            nextLabel = "miễn phí giao hàng";
        }
        // Khoảng cách chỉ để tham khảo, không ảnh hưởng phí người mua trả
        boolean hasAddress = (lat != null && lng != null) || (address != null && !address.isBlank());
        return b.distanceKm(hasAddress ? distanceKm(c, lat, lng, address) : null)
                .baseFee(fee).shippingFee(fee).discountPercent(fee.signum() == 0 ? 100 : 0)
                .nextTierKg(nextKg).nextTierLabel(nextLabel)
                .note(fee.signum() == 0 ? "Miễn phí giao hàng" : null)
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
}
