package com.sang.be_capnong.dto.request.campaign;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

/** Admin nhận thêm hàng tại điểm tập kết cho phần còn thiếu: từ shop đã có (shopId) hoặc shop mới (newShop) */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminSupplementRequest {
    Integer shopId;
    NewShop newShop;
    /** Số kg đạt chuẩn nhận thêm */
    BigDecimal weightKg;

    /** Thông tin shop hoàn toàn mới, dùng để tạo tài khoản shop (shop đặt mật khẩu qua "Quên mật khẩu") */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class NewShop {
        String shopName;
        String ownerName;
        String phoneNumber;
        String email;
        String address;
        String bankName;
        String bankAccount;
        String bankAccountHolder;
    }
}
