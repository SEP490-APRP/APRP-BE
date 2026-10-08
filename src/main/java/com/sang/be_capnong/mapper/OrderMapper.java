package com.sang.be_capnong.mapper;

import com.sang.be_capnong.dto.response.OrderResponse;
import com.sang.be_capnong.entity.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    // Dùng cho các endpoint State Machine (chỉ cần trả info cơ bản, không cần items)
    @Mapping(source = "buyer.fullName", target = "buyerName")
    @Mapping(source = "shop.shopName", target = "shopName")
    @Mapping(source = "shop.id", target = "shopId")
    @Mapping(source = "checkoutBatch.id", target = "batchId")
    @Mapping(target = "voucherDiscountAmount", ignore = true)
    @Mapping(target = "items", ignore = true)
    OrderResponse toOrderResponse(Order order);
}

