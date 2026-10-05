package com.sang.be_capnong.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CartResponse {
    int cartId;
    List<ShopCartResponse> shopCarts;
    BigDecimal cartTotal; // Tổng tiền tất cả các món (mang tính tham khảo)
}
