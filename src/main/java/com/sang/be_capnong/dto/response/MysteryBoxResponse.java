package com.sang.be_capnong.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MysteryBoxResponse {
    int mysteryId;
    int shopId;
    String shopName;
    String boxType;
    String imageUrl;
    BigDecimal price;
    String description;
    String note;
    Boolean isActive;
    Integer totalQuantity;
    Integer soldQuantity;
    List<ProductMysteryResponse> items;
}
