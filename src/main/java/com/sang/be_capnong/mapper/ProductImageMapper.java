package com.sang.be_capnong.mapper;

import com.sang.be_capnong.dto.request.ProductImageCreationRequest;
import com.sang.be_capnong.dto.request.ProductImageUpdateRequest;
import com.sang.be_capnong.dto.response.ProductImageResponse;
import com.sang.be_capnong.entity.ProductImage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductImageMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "isApproved", ignore = true)
    @Mapping(target = "isFlagged", ignore = true)
    @Mapping(target = "flagReason", ignore = true)
    ProductImage toProductImage(ProductImageCreationRequest request);

    ProductImageResponse toProductImageResponse(ProductImage productImage);

    List<ProductImageResponse> toProductImageResponseList(List<ProductImage> productImages);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "isApproved", ignore = true)
    @Mapping(target = "isFlagged", ignore = true)
    @Mapping(target = "flagReason", ignore = true)
    void updateProductImage(@MappingTarget ProductImage productImage, ProductImageUpdateRequest request);
}
