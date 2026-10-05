package com.sang.be_capnong.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderItemRequest {

    // Chỉ cần điền đúng 1 trong 3 ID bên dưới tùy loại sản phẩm muốn mua

    // Nếu mua sản phẩm đơn lẻ
    Integer productId;

    // Nếu mua mystery box (túi mù)
    Integer mysteryBoxId;

    // Nếu mua combo
    Integer buildComboId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    Integer quantity;
}
