package com.sang.be_capnong.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Request body gửi lên khi muốn xem trước phí vận chuyển.
 *
 * Ví dụ:
 * {
 *   "address": "99 Nguyễn Huệ, Quận 1, TP Hồ Chí Minh"
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShippingFeeRequest {

    /**
     * ID của chủ shop (người gửi hàng).
     * Hệ thống sẽ truy xuất địa chỉ của shop này trong DB để tính làm điểm xuất phát.
     */
    Integer shopId;

    /**
     * Địa chỉ giao hàng của người mua dạng chuỗi (text).
     */
    String buyerAddress;

    /**
     * ID của địa điểm trên Google Maps (lấy từ kết quả gợi ý địa chỉ).
     * Tuyến lấy theo placeId sẽ chính xác hơn là dùng chuỗi buyerAddress.
     * Chọn truyền lên FE 1 trong 2: buyerAddress hoặc buyerPlaceId.
     */
    String buyerPlaceId;
}
