package com.sang.be_capnong.dto.google;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * Toàn bộ response trả về từ Google Maps Geocoding API.
 *
 * Ví dụ response:
 * {
 *   "status": "OK",
 *   "results": [
 *     {
 *       "formatted_address": "99 Nguyễn Huệ, Quận 1, TP.HCM",
 *       "geometry": { "location": { "lat": 10.773, "lng": 106.704 } }
 *     }
 *   ]
 * }
 *
 * Status có thể có:
 *  - "OK"              → thành công, đọc results[0].geometry.location
 *  - "ZERO_RESULTS"    → không tìm thấy địa chỉ
 *  - "REQUEST_DENIED"  → API key lỗi hoặc chưa bật Geocoding API
 *  - "INVALID_REQUEST" → thiếu tham số address
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GoogleGeocodeResponse {
    private String status;
    private List<GoogleGeocodeResult> results;
}
