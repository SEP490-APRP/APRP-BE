package com.sang.be_capnong.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShopReturnRespondRequest {

    /** Đồng ý (true) hoặc Từ chối (false) yêu cầu hoàn trả */
    boolean accept;

    /** Phản hồi / lý do của shop */
    String shopResponse;

    /** true: Yêu cầu lấy lại hàng hoàn về shop; false: Hoàn tiền ngay không cần lấy lại hàng */
    Boolean isReturnGoods;

    /** Hình thức thanh toán cước ship hoàn do shop chi trả: "WALLET" (ví sàn) hoặc "COD" (tiền mặt cho shipper). Mặc định: "WALLET" */
    String shippingPaymentMethod;

    /** Cước phí ship hoàn (nếu không truyền, tự động tính theo cước của đơn hàng) */
    BigDecimal returnShippingFee;
}
