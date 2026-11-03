package com.sang.be_capnong.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReturnCreateRequest {

    @NotNull(message = "Chi tiết đơn hàng không được để trống")
    Integer orderDetailId;

    @NotBlank(message = "Lý do yêu cầu hoàn trả không được để trống")
    String reason;

    BigDecimal refundAmount;

    Boolean isReturnGoods;

    List<String> evidenceUrls;
}
