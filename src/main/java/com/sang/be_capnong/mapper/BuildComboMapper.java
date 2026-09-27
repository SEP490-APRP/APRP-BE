package com.sang.be_capnong.mapper;

import com.sang.be_capnong.dto.request.BuildComboCreationRequest;
import com.sang.be_capnong.dto.request.BuildComboUpdateRequest;
import com.sang.be_capnong.dto.response.BuildComboResponse;
import com.sang.be_capnong.dto.response.ProductComboResponse;
import com.sang.be_capnong.dto.response.ShopSummaryResponse;
import com.sang.be_capnong.entity.BuildCombo;
import com.sang.be_capnong.entity.ProductCombo;
import com.sang.be_capnong.entity.ProductImage;
import com.sang.be_capnong.entity.User;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BuildComboMapper {

    @Mapping(target = "comboId", ignore = true)
    @Mapping(target = "shop", ignore = true)
    @Mapping(target = "soldQuantity", constant = "0")
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "productCombos", ignore = true)
    BuildCombo toBuildCombo(BuildComboCreationRequest request);

    @Mapping(target = "items", source = "productCombos")
    BuildComboResponse toBuildComboResponse(BuildCombo buildCombo);

    List<BuildComboResponse> toBuildComboResponseList(List<BuildCombo> buildCombos);

    ShopSummaryResponse toShopSummaryResponse(User user);

    @Mapping(target = "productId", source = "product.productId")
    @Mapping(target = "productName", source = "product.productName")
    @Mapping(target = "unit", source = "product.unit")
    @Mapping(target = "sellingPrice", source = "product.sellingPrice")
    @Mapping(target = "imageUrl", source = "product.images", qualifiedByName = "getFirstImageUrl")
    ProductComboResponse toProductComboResponse(ProductCombo productCombo);

    List<ProductComboResponse> toProductComboResponseList(List<ProductCombo> productCombos);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "comboId", ignore = true)
    @Mapping(target = "shop", ignore = true)
    @Mapping(target = "soldQuantity", ignore = true)
    @Mapping(target = "productCombos", ignore = true)
    void updateBuildCombo(@MappingTarget BuildCombo combo, BuildComboUpdateRequest request);

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
