package com.sang.be_capnong.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
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
public class ReviewResponse {
    int id;
    int reviewId;
    Integer buyerId;
    String buyerName;
    String fullName;
    Integer shopId;
    String shopName;
    Integer orderDetailId;
    Integer orderId;
    Integer productId;
    String productName;
    Integer mysteryBoxId;
    String boxType;
    Integer comboId;
    String comboName;
    BigDecimal ratingStar;
    String comment;
    String evidence;
    List<String> imageUrls;
    String replyFromShop;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime createAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime createdAt;

    long likeCount;
    long dislikeCount;
    String currentUserReaction;
}
