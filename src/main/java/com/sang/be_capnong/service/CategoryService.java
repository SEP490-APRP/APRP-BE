package com.sang.be_capnong.service;

import com.sang.be_capnong.dto.request.CategoryCreationRequest;
import com.sang.be_capnong.dto.request.CategoryUpdateRequest;
import com.sang.be_capnong.dto.response.CategoryResponse;

import java.util.List;

public interface CategoryService {

    CategoryResponse createCategory(CategoryCreationRequest request);

    List<CategoryResponse> getAllCategories();

    CategoryResponse getCategoryById(Integer categoryId);

    CategoryResponse updateCategory(Integer categoryId, CategoryUpdateRequest request);

    void deleteCategory(Integer categoryId);
}
