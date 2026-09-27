package com.sang.be_capnong.dto.google;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Đại diện cho một ô (element) trong Google Distance Matrix.
 *
 * Google API trả về (với 1 origin và 1 destination):
 * {
 *   "status": "OK",
 *   "distance": { "text": "7.5 km", "value": 7540 },
 *   "duration": { "text": "20 mins", "value": 1200 }
 * }
 *
 * Các giá trị status có thể có:
 *  - "OK"                  → tính được khoảng cách, dùng distance.value
 *  - "NOT_FOUND"           → không tìm thấy địa chỉ trên bản đồ
 *  - "ZERO_RESULTS"        → không có đường đi
 *  - "MAX_ROUTE_LENGTH_EXCEEDED" → đường đi quá dài
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GoogleDistanceElement {

    /** Trạng thái tính toán, chỉ dùng khi status == "OK" */
    private String status;

    /** Khoảng cách từ origin đến destination */
    private GoogleDistance distance;
}
