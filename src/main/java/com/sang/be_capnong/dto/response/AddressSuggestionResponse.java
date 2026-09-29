package com.sang.be_capnong.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Gợi ý địa chỉ trả về từ tính năng AutoComplete (Google Maps).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AddressSuggestionResponse {
    
    /**
     * ID của địa điểm trên Google Maps.
     * Dùng để gửi lại cho BE lúc tính phí ship nhằm lấy chính xác tọa độ (thay vì dùng text).
     */
    String placeId;

    /**
     * Địa chỉ hiển thị cho người dùng (ví dụ: "99 Nguyễn Huệ, Quận 1, TP Hồ Chí Minh").
     */
    String description;
}
