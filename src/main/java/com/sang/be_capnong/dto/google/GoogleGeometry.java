package com.sang.be_capnong.dto.google;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Thông tin hình học của một địa điểm (bao gồm tọa độ trung tâm).
 *
 * JSON:
 * "geometry": {
 *   "location": { "lat": 10.7769, "lng": 106.7009 },
 *   ...
 * }
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GoogleGeometry {
    private GoogleGeoLocation location;
}
