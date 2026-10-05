package com.sang.be_capnong.service;

import com.sang.be_capnong.dto.request.CartAddRequest;
import com.sang.be_capnong.dto.request.CartRemoveRequest;
import com.sang.be_capnong.dto.request.CartUpdateRequest;
import com.sang.be_capnong.dto.response.ApiResponse;
import com.sang.be_capnong.dto.response.CartResponse;

public interface CartService {
    
    // 1. Xem giỏ hàng của user hiện tại
    CartResponse getMyCart();

    // 2. Thêm sản phẩm vào giỏ hàng
    ApiResponse<Void> addToCart(CartAddRequest request);

    // 3. Cập nhật số lượng
    ApiResponse<Void> updateCartItem(CartUpdateRequest request);

    // 4. Xóa 1 hoặc nhiều sản phẩm khỏi giỏ hàng
    ApiResponse<Void> removeCartItems(CartRemoveRequest request);

    // 5. Xóa toàn bộ giỏ hàng (gọi sau khi đặt hàng thành công)
    void clearCart();
}
