package com.sang.be_capnong.service;

import com.sang.be_capnong.dto.response.ShippingFeeResponse;

import com.sang.be_capnong.dto.request.ShippingFeeRequest;

/**
 * Service tính phí vận chuyển dự kiến trước khi đặt hàng.
 * Sử dụng Google Maps Distance Matrix API để lấy khoảng cách thực tế.
 */
public interface ShippingFeeService {

    /**
     * Tính phí ship từ shop đến địa chỉ buyer.
     *
     * @param request Chứa shopId, buyerAddress và buyerPlaceId
     * @return ShippingFeeResponse chứa distanceKm, shippingFee và note (nếu có lỗi)
     */
    ShippingFeeResponse calculateShippingFee(ShippingFeeRequest request);
}
