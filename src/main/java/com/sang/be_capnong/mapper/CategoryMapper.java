package com.sang.be_capnong.mapper;

import com.sang.be_capnong.dto.request.CategoryCreationRequest;
import com.sang.be_capnong.dto.request.CategoryUpdateRequest;
import com.sang.be_capnong.dto.response.CategoryResponse;
import com.sang.be_capnong.entity.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    @Mapping(target = "categoryId", ignore = true)
    @Mapping(target = "products", ignore = true)
    Category toCategory(CategoryCreationRequest request);

    CategoryResponse toCategoryResponse(Category category);

    List<CategoryResponse> toCategoryResponseList(List<Category> categories);

    @Mapping(target = "categoryId", ignore = true)
    @Mapping(target = "products", ignore = true)
    void updateCategory(@MappingTarget Category category, CategoryUpdateRequest request);
}
