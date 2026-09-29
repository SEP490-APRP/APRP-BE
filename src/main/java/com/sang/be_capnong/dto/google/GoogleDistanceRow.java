package com.sang.be_capnong.dto.google;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * Một hàng trong Distance Matrix, ứng với 1 điểm xuất phát (origin).
 *
 * Nếu gọi với 1 origin + 1 destination, rows sẽ có 1 phần tử,
 * và rows[0].elements sẽ có 1 phần tử.
 *
 * Cấu trúc JSON:
 * "rows": [
 *   {
 *     "elements": [ { "status": "OK", "distance": {...} } ]
 *   }
 * ]
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GoogleDistanceRow {
    private List<GoogleDistanceElement> elements;
}
