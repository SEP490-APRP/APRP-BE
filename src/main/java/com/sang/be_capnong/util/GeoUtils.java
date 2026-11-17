package com.sang.be_capnong.util;

/** Tính khoảng cách và thời gian dự kiến (ước lượng, không gọi Google) */
public final class GeoUtils {

    /** Hệ số quy đổi đường chim bay sang đường đi thực tế trong đô thị */
    public static final double ROAD_FACTOR = 1.25;

    /** Tốc độ trung bình xe máy giao hàng trong nội thành (km/h) */
    public static final double AVG_SPEED_KMH = 25.0;

    private GeoUtils() {
    }

    /** Khoảng cách đường chim bay (km), công thức haversine */
    public static double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        double r = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    /** Quãng đường ước tính (km, làm tròn 1 chữ số) */
    public static double roadKm(double lat1, double lng1, double lat2, double lng2) {
        return Math.round(haversineKm(lat1, lng1, lat2, lng2) * ROAD_FACTOR * 10.0) / 10.0;
    }

    /** Số phút dự kiến đi hết quãng đường, tối thiểu 1 phút nếu còn quãng đường */
    public static int etaMinutes(double roadKm) {
        if (roadKm <= 0) return 0;
        return (int) Math.max(1, Math.ceil(roadKm / AVG_SPEED_KMH * 60));
    }
}
