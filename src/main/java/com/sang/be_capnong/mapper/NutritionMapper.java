package com.sang.be_capnong.mapper;

import com.sang.be_capnong.dto.request.NutritionInfoRequest;
import com.sang.be_capnong.dto.response.NutritionInfoResponse;
import com.sang.be_capnong.entity.NutritionInfo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface NutritionMapper {

    @Mapping(target = "productId", source = "product.productId")
    NutritionInfoResponse toNutritionInfoResponse(NutritionInfo nutritionInfo);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "product", ignore = true)
    NutritionInfo toNutritionInfo(NutritionInfoRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "product", ignore = true)
    void updateNutritionInfo(@MappingTarget NutritionInfo nutritionInfo, NutritionInfoRequest request);
}
