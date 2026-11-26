package com.sang.be_capnong.dto.response.campaign;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

/** Xem trước khi chốt phần thiếu: đơn nào bị giảm hoặc huỷ, hoàn cọc bao nhiêu */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShortfallPreviewResponse {
    BigDecimal shortfallKg;
    /** Số kg bị bớt (có thể dư chút so với thiếu vì giảm nguyên thùng) */
    BigDecimal removedKg;
    BigDecimal refundTotal;
    List<Item> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Item {
        int preOrderId;
        String code;
        String recipientName;
        int crateKg;
        int oldCrates;
        /** 0 = huỷ đơn */
        int newCrates;
        BigDecimal refund;
    }
}
