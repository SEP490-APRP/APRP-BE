package com.sang.be_capnong.service;

import com.sang.be_capnong.dto.response.AddressSuggestionResponse;
import java.util.List;

/**
 * Service tương tác với Google Maps API để:
 *  1. Geocoding: chuyển địa chỉ text / placeId → tọa độ (lat, lng)
 *  2. Distance Matrix: tính khoảng cách thực tế giữa 2 điểm
 *  3. Places Autocomplete: Gọi ý nhập địa chỉ cho người mua
 *
 * Tại sao dùng Google Maps thay vì Goong?
 *  - Google Maps có độ phủ rộng hơn, dữ liệu Việt Nam chính xác hơn
 *  - Distance Matrix tính đường thực tế (không phải đường chim bay)
 *  - Dễ tích hợp với các dịch vụ Google khác trong tương lai
 */
public interface GoogleMapService {

    /**
     * Chuyển địa chỉ text sang tọa độ lat/lng.
     *
     * @param address Chuỗi địa chỉ cần geocode
     * @return Mảng double[]{lat, lng}, hoặc null nếu thất bại
     */
    double[] geocodeAddress(String address);

    /**
     * Lấy tọa độ chính xác bằng Place ID (từ kết quả autocomplete).
     * 
     * @param placeId Google Place ID
     * @return Mảng double[]{lat, lng}, hoặc null nếu thất bại
     */
    double[] geocodePlaceId(String placeId);

    /**
     * Gợi ý địa chỉ từ chuỗi tìm kiếm của người dùng (Autocomplete).
     * Mặc định bias ưu tiên kết quả trả về trong VN / HCM để kết quả chuẩn.
     *
     * @param input Chuỗi người dùng gõ
     * @return Danh sách các gợi ý địa chỉ
     */
    List<AddressSuggestionResponse> getAddressSuggestions(String input);

    /**
     * Tính khoảng cách thực tế (theo đường đi, tính bằng km)
     * giữa 2 điểm dùng Google Maps Distance Matrix API.
     *
     * @param originLat      Vĩ độ điểm xuất phát (kho hàng)
     * @param originLng      Kinh độ điểm xuất phát
     * @param destinationLat Vĩ độ điểm đến (địa chỉ buyer)
     * @param destinationLng Kinh độ điểm đến
     * @return Khoảng cách (km), hoặc Double.MAX_VALUE nếu thất bại
     */
    double getDistanceKm(double originLat, double originLng,
                         double destinationLat, double destinationLng);
}
