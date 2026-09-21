package com.sang.be_capnong.mapper;

import com.sang.be_capnong.dto.request.MysteryBoxCreationRequest;
import com.sang.be_capnong.dto.request.MysteryBoxUpdateRequest;
import com.sang.be_capnong.dto.response.MysteryBoxResponse;
import com.sang.be_capnong.dto.response.ProductMysteryResponse;
import com.sang.be_capnong.entity.MysteryBox;
import com.sang.be_capnong.entity.ProductImage;
import com.sang.be_capnong.entity.ProductMystery;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MysteryBoxMapper {

    @Mapping(target = "mysteryId", ignore = true)
    @Mapping(target = "shop", ignore = true)
    @Mapping(target = "isActive", constant = "true")
    @Mapping(target = "soldQuantity", constant = "0")
    @Mapping(target = "productMysteries", ignore = true)
    MysteryBox toMysteryBox(MysteryBoxCreationRequest request);

    @Mapping(target = "shopId", source = "shop.id")
    @Mapping(target = "shopName", source = "shop.shopName")
    @Mapping(target = "items", ignore = true)
    MysteryBoxResponse toMysteryBoxResponse(MysteryBox mysteryBox);

    List<MysteryBoxResponse> toMysteryBoxResponseList(List<MysteryBox> mysteryBoxes);

    @Mapping(target = "productId", source = "product.productId")
    @Mapping(target = "productName", source = "product.productName")
    @Mapping(target = "unit", source = "product.unit")
    @Mapping(target = "imageUrl", source = "product.images", qualifiedByName = "getFirstImageUrl")
    ProductMysteryResponse toProductMysteryResponse(ProductMystery productMystery);

    List<ProductMysteryResponse> toProductMysteryResponseList(List<ProductMystery> productMysteries);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "mysteryId", ignore = true)
    @Mapping(target = "shop", ignore = true)
    @Mapping(target = "soldQuantity", ignore = true)
    @Mapping(target = "productMysteries", ignore = true)
    void updateMysteryBox(@MappingTarget MysteryBox mysteryBox, MysteryBoxUpdateRequest request);

    @Named("getFirstImageUrl")
    default String getFirstImageUrl(List<ProductImage> images) {
        if (images == null || images.isEmpty()) {
            return null;
        }
        return images.stream()
                .filter(img -> Boolean.TRUE.equals(img.getIsPrimary()))
                .map(ProductImage::getImageUrl)
                .findFirst()
                .orElse(images.get(0).getImageUrl());
    }
}
