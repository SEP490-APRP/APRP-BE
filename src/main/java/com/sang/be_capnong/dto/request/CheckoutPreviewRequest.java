package com.sang.be_capnong.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CheckoutPreviewRequest {
    @NotEmpty(message = "Order must have at least one item")
    @Valid
    List<OrderItemRequest> items;

    String shippingAddress;

    List<String> voucherCodes;
    
    List<Integer> cartItemIds;
}
