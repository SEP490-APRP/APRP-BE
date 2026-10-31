package com.sang.be_capnong.mapper;

import com.sang.be_capnong.dto.request.ReviewCreateRequest;
import com.sang.be_capnong.dto.response.ReviewResponse;
import com.sang.be_capnong.entity.Image;
import com.sang.be_capnong.entity.Review;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Collections;
import java.util.List;

@Mapper(componentModel = "spring")
public interface ReviewMapper {

    @Mapping(target = "reviewId", ignore = true)
    @Mapping(target = "buyer", ignore = true)
    @Mapping(target = "shop", ignore = true)
    @Mapping(target = "orderDetail", ignore = true)
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "mysteryBox", ignore = true)
    @Mapping(target = "buildCombo", ignore = true)
    @Mapping(target = "createAt", ignore = true)
    @Mapping(target = "replyFromShop", ignore = true)
    @Mapping(target = "reactions", ignore = true)
    @Mapping(target = "images", ignore = true)
    Review toReview(ReviewCreateRequest request);

    @Mapping(target = "id", source = "reviewId")
    @Mapping(target = "reviewId", source = "reviewId")
    @Mapping(target = "buyerId", source = "buyer.id")
    @Mapping(target = "buyerName", source = "buyer.fullName")
    @Mapping(target = "fullName", source = "buyer.fullName")
    @Mapping(target = "shopId", source = "shop.id")
    @Mapping(target = "shopName", source = "shop.shopName")
    @Mapping(target = "orderDetailId", source = "orderDetail.orderDetailId")
    @Mapping(target = "orderId", source = "orderDetail.order.orderId")
    @Mapping(target = "productId", source = "product.productId")
    @Mapping(target = "productName", source = "product.productName")
    @Mapping(target = "mysteryBoxId", source = "mysteryBox.mysteryId")
    @Mapping(target = "boxType", source = "mysteryBox.boxType")
    @Mapping(target = "comboId", source = "buildCombo.comboId")
    @Mapping(target = "comboName", source = "buildCombo.comboName")
    @Mapping(target = "createdAt", source = "createAt")
    @Mapping(target = "imageUrls", source = "images", qualifiedByName = "mapImagesToUrls")
    @Mapping(target = "likeCount", ignore = true)
    @Mapping(target = "dislikeCount", ignore = true)
    @Mapping(target = "currentUserReaction", ignore = true)
    ReviewResponse toReviewResponse(Review review);

    List<ReviewResponse> toReviewResponseList(List<Review> reviews);

    @Named("mapImagesToUrls")
    default List<String> mapImagesToUrls(List<Image> images) {
        if (images == null || images.isEmpty()) {
            return Collections.emptyList();
        }
        return images.stream().map(Image::getImageUrl).toList();
    }
}
