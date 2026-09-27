package com.sang.be_capnong.dto.google;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Toàn bộ response trả về từ Google Maps Distance Matrix API.
 *
 * Ví dụ response khi gọi với 1 origin + 1 destination:
 * {
 *   "status": "OK",
 *   "origin_addresses": ["Kho hàng, TP.HCM, Việt Nam"],
 *   "destination_addresses": ["99 Nguyễn Huệ, Quận 1, TP.HCM, Việt Nam"],
 *   "rows": [
 *     {
 *       "elements": [
 *         {
 *           "status": "OK",
 *           "distance": { "text": "7.5 km", "value": 7540 }
 *         }
 *       ]
 *     }
 *   ]
 * }
 *
 * Status tổng (rows level):
 *  - "OK"              → thành công, đọc rows[0].elements[0].distance.value
 *  - "INVALID_REQUEST" → params thiếu hoặc sai format
 *  - "MAX_ELEMENTS_EXCEEDED" → quá giới hạn (origins × destinations)
 *  - "OVER_DAILY_LIMIT"      → hết quota ngày
 *  - "REQUEST_DENIED"        → API key không hợp lệ hoặc chưa bật service
 *  - "UNKNOWN_ERROR"         → lỗi phía Google server
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GoogleDistanceMatrixResponse {

    /** Trạng thái tổng của toàn bộ request */
    private String status;

    /** Danh sách địa chỉ origin đã được Google chuẩn hoá */
    @JsonProperty("origin_addresses")
    private List<String> originAddresses;

    /** Danh sách địa chỉ destination đã được Google chuẩn hoá */
    @JsonProperty("destination_addresses")
    private List<String> destinationAddresses;

    /** Ma trận kết quả: mỗi row ứng với 1 origin */
    private List<GoogleDistanceRow> rows;
}
