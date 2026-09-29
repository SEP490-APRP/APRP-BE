package com.sang.be_capnong.dto.google;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Đại diện cho thông tin khoảng cách trong Google Maps Distance Matrix response.
 *
 * Google API trả về:
 * {
 *   "text": "7.5 km",   ← chuỗi hiển thị
 *   "value": 7540        ← giá trị chính xác tính bằng MÉT
 * }
 *
 * Chúng ta dùng `value` để tính phí ship (chia 1000 để ra km).
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GoogleDistance {
    /** Khoảng cách hiển thị dạng chuỗi, ví dụ: "7.5 km" */
    private String text;

    /** Khoảng cách chính xác tính bằng mét, ví dụ: 7540 */
    private double value;
}
