package com.sang.be_capnong.dto.response.campaign;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Thông tin hiện ra khi quét tem QR trên thùng hàng. Trang này công khai (ai quét cũng xem được)
 * nên KHÔNG chứa số điện thoại, địa chỉ hay số tiền.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PackageTraceResponse {
    String orderCode;
    String campaignTitle;
    String status;
    /** Như PreOrderResponse.hubStage */
    String hubStage;

    String recipientName;
    BigDecimal totalKg;
    int crateKg;
    int crateQuantity;

    /** Nguồn hàng */
    String productName;
    String farmName;
    LocalDate harvestDate;
    String collectionSiteName;

    /** Chất lượng và bảo quản */
    boolean qualityPassed;
    LocalDateTime checkedAt;
    String shelfLifeTier;
    /** Hết hạn độ tươi dự kiến */
    LocalDateTime bestBefore;

    LocalDateTime packedAt;
    LocalDateTime deliveredAt;
}
