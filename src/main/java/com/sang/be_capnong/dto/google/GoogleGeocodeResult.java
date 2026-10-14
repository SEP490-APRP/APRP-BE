package com.sang.be_capnong.dto.google;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Một kết quả geocoding (có thể có nhiều kết quả cho 1 địa chỉ).
 * Chúng ta chỉ lấy kết quả đầu tiên (kết quả tốt nhất theo Google).
 *
 * JSON:
 * {
 *   "formatted_address": "99 Nguyễn Huệ, Bến Nghé, Quận 1, Thành phố Hồ Chí Minh, Việt Nam",
 *   "geometry": { "location": { "lat": 10.773, "lng": 106.704 } }
 * }
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GoogleGeocodeResult {

    /** Địa chỉ đã được Google chuẩn hoá (dùng để debug/log) */
    @JsonProperty("formatted_address")
    private String formattedAddress;

    @JsonProperty("place_id")
    private String placeId;

    /** Chứa tọa độ lat/lng */
    private GoogleGeometry geometry;
}
