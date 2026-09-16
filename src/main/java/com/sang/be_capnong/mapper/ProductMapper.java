package com.sang.be_capnong.mapper;

import com.sang.be_capnong.dto.request.ProductCreationRequest;
import com.sang.be_capnong.dto.request.ProductUpdateRequest;
import com.sang.be_capnong.dto.response.ProductResponse;
import com.sang.be_capnong.dto.response.ShopSummaryResponse;
import com.sang.be_capnong.entity.Product;
import com.sang.be_capnong.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring", uses = {CategoryMapper.class, ProductImageMapper.class})
public interface ProductMapper {

    @Mapping(target = "productId", ignore = true)
    @Mapping(target = "shop", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "nutritionInfo", ignore = true)
    @Mapping(target = "moderationResult", ignore = true)
    @Mapping(target = "productAllergens", ignore = true)
    @Mapping(target = "productMysteries", ignore = true)
    @Mapping(target = "productCombos", ignore = true)
    @Mapping(target = "reviews", ignore = true)
    Product toProduct(ProductCreationRequest request);

    ProductResponse toProductResponse(Product product);

    List<ProductResponse> toProductResponseList(List<Product> products);

    ShopSummaryResponse toShopSummaryResponse(User user);

    @Mapping(target = "productId", ignore = true)
    @Mapping(target = "shop", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "images", ignore = true)
    @Mapping(target = "nutritionInfo", ignore = true)
    @Mapping(target = "moderationResult", ignore = true)
    @Mapping(target = "productAllergens", ignore = true)
    @Mapping(target = "productMysteries", ignore = true)
    @Mapping(target = "productCombos", ignore = true)
    @Mapping(target = "reviews", ignore = true)
    void updateProduct(@MappingTarget Product product, ProductUpdateRequest request);
}
