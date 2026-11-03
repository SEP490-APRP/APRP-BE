package com.sang.be_capnong.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sang.be_capnong.enums.ReturnStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReturnRequestResponse {

    int id;
    int returnId;

    Integer orderId;
    Integer orderDetailId;

    String productName;
    String productImage;
    String itemType;
    Integer quantity;
    BigDecimal unitPrice;
    BigDecimal itemTotal;

    Integer buyerId;
    String buyerName;
    String buyerPhone;
    String buyerAddress;

    Integer shopId;
    String shopName;
    String shopPhone;
    String shopAddress;

    Integer shipperId;
    String shipperName;
    String shipperPhone;

    String reason;
    ReturnStatus status;
    BigDecimal refundAmount;
    Boolean isReturnGoods;
    BigDecimal returnShippingFee;

    String evidence;
    List<String> evidenceUrls;

    String shopResponse;
    String adminRemark;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime updatedAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime pickedUpAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime returnedAt;
}
