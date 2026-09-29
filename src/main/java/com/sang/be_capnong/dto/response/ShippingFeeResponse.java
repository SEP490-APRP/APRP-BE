package com.sang.be_capnong.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

/**
 * Kết quả trả về khi xem trước phí ship.
 *
 * - distanceKm  : khoảng cách thực tế giữa kho ↔ địa chỉ buyer (km)
 * - shippingFee : phí vận chuyển đã làm tròn lên 1,000đ gần nhất (VNĐ)
 * - note        : thông tin bổ sung (ví dụ: "Áp dụng phí mặc định khi không xác định được tọa độ")
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ShippingFeeResponse {

    /** Khoảng cách kho → buyer (km). Null nếu không geocode được. */
    Double distanceKm;

    /** Phí ship (VNĐ). Luôn có giá trị (fallback 15,000 nếu lỗi). */
    BigDecimal shippingFee;

    /** Ghi chú thêm, ví dụ nguồn tọa độ hoặc lý do dùng phí mặc định. */
    String note;
}
