package com.sang.be_capnong.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sang.be_capnong.dto.google.GoogleDistanceElement;
import com.sang.be_capnong.dto.google.GoogleDistanceMatrixResponse;
import com.sang.be_capnong.dto.google.GoogleGeocodeResponse;
import com.sang.be_capnong.dto.google.GoogleGeocodeResult;
import com.sang.be_capnong.dto.google.GoogleAutocompleteResponse;
import com.sang.be_capnong.dto.response.AddressSuggestionResponse;
import com.sang.be_capnong.service.GoogleMapService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║  GoogleMapServiceImpl                                                   ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║  Triển khai GoogleMapService bằng cách gọi 2 Google Maps API:          ║
 * ║                                                                         ║
 * ║  1. Geocoding API                                                       ║
 * ║     URL: https://maps.googleapis.com/maps/api/geocode/json              ║
 * ║     Params: address, key                                                ║
 * ║     → Trả về lat/lng từ địa chỉ text                                   ║
 * ║                                                                         ║
 * ║  2. Distance Matrix API                                                 ║
 * ║     URL: https://maps.googleapis.com/maps/api/distancematrix/json       ║
 * ║     Params: origins (lat,lng), destinations (lat,lng), key, language    ║
 * ║     → Trả về khoảng cách thực tế theo đường đi                        ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GoogleMapServiceImpl implements GoogleMapService {

    // Spring tự inject qua constructor nhờ @RequiredArgsConstructor
    RestTemplate restTemplate;
    ObjectMapper objectMapper;

    // Inject giá trị từ application.yml
    @NonFinal
    @Value("${google.maps.api.key}")
    String googleApiKey;

    // URL cố định của Google Maps APIs
    private static final String GEOCODING_URL       = "https://maps.googleapis.com/maps/api/geocode/json";
    private static final String DISTANCE_MATRIX_URL = "https://maps.googleapis.com/maps/api/distancematrix/json";
    private static final String AUTOCOMPLETE_URL    = "https://maps.googleapis.com/maps/api/place/autocomplete/json";

    // ═══════════════════════════════════════════════════════════════════════
    // 1. GEOCODING: địa chỉ text → tọa độ lat/lng
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Gọi Google Geocoding API để chuyển địa chỉ text thành tọa độ.
     *
     * Luồng xử lý:
     *  1. Build URL kèm params: address + key
     *  2. Gọi GET request
     *  3. Parse JSON response thành GoogleGeocodeResponse
     *  4. Lấy lat/lng từ results[0].geometry.location
     *  5. Kiểm tra tọa độ có nằm trong Việt Nam không
     *
     * @param address Địa chỉ cần geocode (vd: "99 Nguyễn Huệ, Quận 1, TP.HCM")
     * @return double[]{lat, lng} hoặc null nếu thất bại
     */
    @Override
    public double[] geocodeAddress(String address) {
        try {
            // Bước 1: Build URL
            String url = UriComponentsBuilder.fromUriString(GEOCODING_URL)
                    .queryParam("address", address)
                    .queryParam("key", googleApiKey)
                    .queryParam("language", "vi") // ưu tiên kết quả tiếng Việt
                    .toUriString();

            log.info("[GoogleMap][Geocode] Geocode địa chỉ: '{}'", address);
            return callGeocodeApi(url);
        } catch (Exception e) {
            log.error("[GoogleMap][Geocode] Lỗi khi geocode '{}': {}", address, e.getMessage());
            return null;
        }
    }

    @Override
    public double[] geocodePlaceId(String placeId) {
        try {
            String url = UriComponentsBuilder.fromUriString(GEOCODING_URL)
                    .queryParam("place_id", placeId)
                    .queryParam("key", googleApiKey)
                    .queryParam("language", "vi")
                    .toUriString();

            log.info("[GoogleMap][Geocode] Geocode place_id: '{}'", placeId);
            return callGeocodeApi(url);
        } catch (Exception e) {
            log.error("[GoogleMap][Geocode] Lỗi khi geocode placeId '{}': {}", placeId, e.getMessage());
            return null;
        }
    }

    private double[] callGeocodeApi(String url) {
        ResponseEntity<String> rawResponse =
                restTemplate.exchange(url, HttpMethod.GET, buildHttpEntity(), String.class);

        try {
            GoogleGeocodeResponse response =
                    objectMapper.readValue(rawResponse.getBody(), GoogleGeocodeResponse.class);

            if (!"OK".equals(response.getStatus())) {
                log.warn("[GoogleMap][Geocode] API trả status='{}'", response.getStatus());
                return null;
            }

            if (response.getResults() == null || response.getResults().isEmpty()) {
                log.warn("[GoogleMap][Geocode] Không có kết quả geocode");
                return null;
            }

            GoogleGeocodeResult bestResult = response.getResults().get(0);
            double lat = bestResult.getGeometry().getLocation().getLat();
            double lng = bestResult.getGeometry().getLocation().getLng();

            if (!isInsideVietnam(lat, lng)) {
                log.warn("[GoogleMap][Geocode] Tọa độ ({}, {}) nằm ngoài Việt Nam — bỏ qua", lat, lng);
                return null;
            }

            return new double[]{lat, lng};
        } catch (Exception e) {
            log.error("[GoogleMap][Geocode] Lỗi parse kết quả: {}", e.getMessage());
            return null;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 2. AUTOCOMPLETE: Gợi ý địa chỉ
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Gợi ý địa chỉ từ chuỗi tìm kiếm của người dùng (Google Places Autocomplete).
     */
    @Override
    public java.util.List<AddressSuggestionResponse> getAddressSuggestions(String input) {
        try {
            // Bias về VN để gợi ý chuẩn hơn (components=country:vn)
            String url = UriComponentsBuilder.fromUriString(AUTOCOMPLETE_URL)
                    .queryParam("input", input)
                    .queryParam("key", googleApiKey)
                    .queryParam("language", "vi")
                    .queryParam("components", "country:vn")
                    .toUriString();

            ResponseEntity<String> rawResp =
                    restTemplate.exchange(url, HttpMethod.GET, buildHttpEntity(), String.class);

            GoogleAutocompleteResponse response =
                    objectMapper.readValue(rawResp.getBody(), GoogleAutocompleteResponse.class);

            if (!"OK".equals(response.getStatus()) || response.getPredictions() == null) {
                return java.util.Collections.emptyList();
            }

            return response.getPredictions().stream()
                    .map(p -> AddressSuggestionResponse.builder()
                            .placeId(p.getPlaceId())
                            .description(p.getDescription())
                            .build())
                    .toList();

        } catch (Exception e) {
            log.error("[GoogleMap][Autocomplete] Lỗi cho input='{}': {}", input, e.getMessage());
            return java.util.Collections.emptyList();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // 3. DISTANCE MATRIX: tính khoảng cách thực tế giữa 2 điểm
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Gọi Google Distance Matrix API để lấy khoảng cách thực tế (km).
     *
     * Lưu ý: Google Distance Matrix tính ĐƯỜNG ĐI THỰC TẾ (có tính traffic),
     * không phải khoảng cách đường thẳng (haversine).
     *
     * Luồng xử lý:
     *  1. Format origin và destination theo dạng "lat,lng"
     *  2. Gọi Distance Matrix API
     *  3. Parse response → lấy rows[0].elements[0]
     *  4. Kiểm tra element.status == "OK", lấy distance.value (mét) → chia 1000 → km
     *
     * @return Khoảng cách (km), hoặc Double.MAX_VALUE nếu không tính được
     */
    @Override
    public double getDistanceKm(double originLat, double originLng,
                                double destinationLat, double destinationLng) {
        try {
            // Format tọa độ: Google yêu cầu dạng "lat,lng"
            String origins      = originLat + "," + originLng;
            String destinations = destinationLat + "," + destinationLng;

            // Bước 1: Build URL
            String url = UriComponentsBuilder.fromUriString(DISTANCE_MATRIX_URL)
                    .queryParam("origins", origins)
                    .queryParam("destinations", destinations)
                    .queryParam("key", googleApiKey)
                    .queryParam("mode", "driving")   // đi bằng xe hơi
                    .queryParam("language", "vi")
                    .toUriString();

            log.info("[GoogleMap][DistanceMatrix] ({},{}) → ({},{})",
                    originLat, originLng, destinationLat, destinationLng);

            // Bước 2: Gọi API
            ResponseEntity<GoogleDistanceMatrixResponse> response =
                    restTemplate.exchange(url, HttpMethod.GET, buildHttpEntity(),
                            GoogleDistanceMatrixResponse.class);

            GoogleDistanceMatrixResponse body = response.getBody();

            // Bước 3: Kiểm tra response hợp lệ
            if (body == null) {
                log.warn("[GoogleMap][DistanceMatrix] Response null");
                return Double.MAX_VALUE;
            }

            if (!"OK".equals(body.getStatus())) {
                log.warn("[GoogleMap][DistanceMatrix] API trả status='{}' — kiểm tra API key hoặc quota",
                        body.getStatus());
                return Double.MAX_VALUE;
            }

            if (body.getRows() == null || body.getRows().isEmpty()
                    || body.getRows().get(0).getElements() == null
                    || body.getRows().get(0).getElements().isEmpty()) {
                log.warn("[GoogleMap][DistanceMatrix] Response không có rows/elements");
                return Double.MAX_VALUE;
            }

            // Bước 4: Lấy element đầu tiên (1 origin × 1 destination = 1 element)
            GoogleDistanceElement element = body.getRows().get(0).getElements().get(0);

            if (!"OK".equals(element.getStatus())) {
                log.warn("[GoogleMap][DistanceMatrix] Element status='{}' — không tính được đường đi",
                        element.getStatus());
                return Double.MAX_VALUE;
            }

            // Chuyển từ mét → km
            double km = element.getDistance().getValue() / 1000.0;

            log.info("[GoogleMap][DistanceMatrix] Kết quả = {} km ({})",
                    String.format("%.2f", km), element.getDistance().getText());

            return km;

        } catch (Exception e) {
            log.error("[GoogleMap][DistanceMatrix] Lỗi: {}", e.getMessage());
            return Double.MAX_VALUE;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Tạo HttpEntity với header chuẩn.
     * Cần set User-Agent để tránh bị một số proxy chặn request.
     */
    private HttpEntity<String> buildHttpEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0");
        headers.set("Accept", "application/json");
        return new HttpEntity<>(headers);
    }

    /**
     * Kiểm tra tọa độ có nằm trong lãnh thổ Việt Nam không.
     * Dùng để tránh nhận kết quả geocode sai vị trí.
     *
     * Việt Nam: latitude 8–24°N, longitude 102–110°E
     */
    private boolean isInsideVietnam(double lat, double lng) {
        return lat >= 8.0 && lat <= 24.0 && lng >= 102.0 && lng <= 110.0;
    }
}
