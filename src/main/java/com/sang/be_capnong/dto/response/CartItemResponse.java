package com.sang.be_capnong.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CartItemResponse {
    int cartItemId;
    String itemType;    // "PRODUCT", "MYSTERY_BOX", "BUILD_COMBO"
    int itemId;
    String name;
    BigDecimal unitPrice;
    int quantity;       // Số lượng khách chọn mua
    int stockQuantity;  // Tồn kho thực tế
    String imageUrl;
    String errorNote;   // Lỗi nếu sản phẩm hết hàng hoặc ngừng bán
    boolean isAvailable; // Xác định xem sản phẩm này còn được phép thanh toán không
}
