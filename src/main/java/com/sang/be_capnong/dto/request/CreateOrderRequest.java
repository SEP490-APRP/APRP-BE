package com.sang.be_capnong.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateOrderRequest {

    // Thông tin người nhận (nếu null -> dùng thông tin của buyer)
    String recipientName;
    String recipientPhone;

    @NotBlank(message = "Shipping address is required")
    String shippingAddress;

    String note;

    /**
     * Phương thức thanh toán:
     * - "COD"      -> Order status = PENDING (thanh toán khi nhận hàng)
     * - "TRANSFER" -> Order status = PAID    (đã thanh toán online/chuyển khoản)
     */
    @NotBlank(message = "Payment method is required (COD or TRANSFER)")
    String paymentMethod;

    // Danh sách sản phẩm muốn mua
    @NotEmpty(message = "Order must have at least one item")
    @Valid
    List<OrderItemRequest> items;

    List<String> voucherCodes;
    List<Integer> cartItemIds;
}
