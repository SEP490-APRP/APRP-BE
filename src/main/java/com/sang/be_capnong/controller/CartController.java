package com.sang.be_capnong.controller;

import com.sang.be_capnong.dto.request.CartAddRequest;
import com.sang.be_capnong.dto.request.CartRemoveRequest;
import com.sang.be_capnong.dto.request.CartUpdateRequest;
import com.sang.be_capnong.dto.response.ApiResponse;
import com.sang.be_capnong.dto.response.CartResponse;
import com.sang.be_capnong.service.CartService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CartController {

    CartService cartService;

    @GetMapping
    public ApiResponse<CartResponse> getMyCart() {
        return ApiResponse.<CartResponse>builder()
                .result(cartService.getMyCart())
                .build();
    }

    @PostMapping("/add")
    public ApiResponse<Void> addToCart(@RequestBody @Valid CartAddRequest request) {
        return cartService.addToCart(request);
    }

    @PutMapping("/update")
    public ApiResponse<Void> updateCartItem(@RequestBody @Valid CartUpdateRequest request) {
        return cartService.updateCartItem(request);
    }

    @DeleteMapping("/remove")
    public ApiResponse<Void> removeCartItems(@RequestBody @Valid CartRemoveRequest request) {
        return cartService.removeCartItems(request);
    }
}
