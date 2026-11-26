package com.sang.be_capnong.service.campaign;

import com.sang.be_capnong.dto.request.hub.HubTripStop;
import com.sang.be_capnong.util.GeoUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Cước xe tải chở hàng từ điểm tập kết tới các hub (chỉ tính chiều đi), theo bảng giá xe tải thùng bạt / thùng kín:
 * <pre>
 * Loại xe        Trọng tải      Mở cửa (4 km đầu)   Km thứ 5 - 20   Trên 20 km
 * Xe tải nhỏ     đến 1 tấn      250.000đ            13.000đ/km      11.000đ/km
 * Xe tải trung   đến 2,5 tấn    350.000đ            15.000đ/km      13.000đ/km
 * Xe tải lớn     đến 5 tấn      500.000đ            20.000đ/km      17.000đ/km
 * </pre>
 * Loại xe chọn theo tổng kg của chuyến. Quãng đường = điểm tập kết -> hub 1 -> hub 2 ... (chim bay x hệ số đường thực tế),
 * ghé thêm hub không tính phụ phí, chỉ tính thêm quãng đường. Làm tròn tới 1.000đ.
 */
@Component
public class HubTripFeeCalculator {

    /** Một hạng xe trong bảng giá */
    public record Tier(String label, double maxKg, long openFee, long perKm, long perKmOver20) {}

    public static final double OPEN_KM = 4;
    private static final double FAR_KM = 20;
    public static final List<Tier> TIERS = List.of(
            new Tier("Xe tải nhỏ", 1000, 250_000, 13_000, 11_000),
            new Tier("Xe tải trung", 2500, 350_000, 15_000, 13_000),
            new Tier("Xe tải lớn", 5000, 500_000, 20_000, 17_000));

    /** Xe nhỏ hơn mức này không được nhận chuyến chở hàng ra hub (xe máy không đủ) */
    @Value("${capnong.campaign.hubtrip.min-vehicle-kg:300}")
    private int minVehicleKg = 300;

    public int getMinVehicleKg() {
        return minVehicleKg;
    }

    /** Hạng xe nhỏ nhất chở được số kg này; nặng hơn mọi hạng thì tính xe lớn nhất */
    public Tier tierFor(double totalKg) {
        return TIERS.stream().filter(t -> totalKg <= t.maxKg()).findFirst().orElse(TIERS.get(TIERS.size() - 1));
    }

    /** Quãng đường điểm tập kết -> các hub theo thứ tự ghé (km), null nếu thiếu toạ độ */
    public Double routeKm(Double siteLat, Double siteLng, List<HubTripStop> stops) {
        if (siteLat == null || siteLng == null || stops == null || stops.isEmpty()) return null;
        double km = 0;
        double lat = siteLat, lng = siteLng;
        for (HubTripStop s : stops) {
            if (s.getHubLatitude() == null || s.getHubLongitude() == null) return null;
            km += GeoUtils.haversineKm(lat, lng, s.getHubLatitude(), s.getHubLongitude()) * GeoUtils.ROAD_FACTOR;
            lat = s.getHubLatitude();
            lng = s.getHubLongitude();
        }
        return Math.round(km * 10) / 10.0;
    }

    /** Cước gộp (trước phí dịch vụ) của một chuyến; thiếu toạ độ thì tính giá mở cửa */
    public BigDecimal fee(Double routeKm, double totalKg) {
        double km = routeKm != null ? routeKm : 0;
        Tier t = tierFor(totalKg);
        double raw = t.openFee()
                + Math.max(0, Math.min(km, FAR_KM) - OPEN_KM) * t.perKm()
                + Math.max(0, km - FAR_KM) * t.perKmOver20();
        return BigDecimal.valueOf(Math.round(raw / 1000.0) * 1000L);
    }

    /** Thông số cho FE ước tính chi phí trước khi lưu chuyến */
    public Map<String, Object> config() {
        return Map.of(
                "openKm", OPEN_KM,
                "minVehicleKg", minVehicleKg,
                "roadFactor", GeoUtils.ROAD_FACTOR,
                "tiers", TIERS.stream().map(t -> Map.of("label", t.label(), "maxKg", t.maxKg(), "openFee", t.openFee(),
                        "perKm", t.perKm(), "perKmOver20", t.perKmOver20())).toList());
    }
}
