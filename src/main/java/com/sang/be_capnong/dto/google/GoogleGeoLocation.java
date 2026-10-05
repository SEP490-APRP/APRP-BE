package com.sang.be_capnong.dto.google;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Toạ độ địa lý trả về từ Google Geocoding API.
 *
 * JSON:
 * "location": { "lat": 10.7769, "lng": 106.7009 }
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GoogleGeoLocation {
    private double lat;
    private double lng;
}
