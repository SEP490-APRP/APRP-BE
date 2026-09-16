package com.sang.be_capnong.service.impl;

import com.sang.be_capnong.dto.request.CategoryCreationRequest;
import com.sang.be_capnong.dto.request.CategoryUpdateRequest;
import com.sang.be_capnong.dto.response.CategoryResponse;
import com.sang.be_capnong.entity.Category;
import com.sang.be_capnong.exception.AppException;
import com.sang.be_capnong.exception.ErrorCode;
import com.sang.be_capnong.mapper.CategoryMapper;
import com.sang.be_capnong.repository.CategoryRepository;
import com.sang.be_capnong.service.CategoryService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class CategoryServiceImpl implements CategoryService {

    CategoryRepository categoryRepository;
    CategoryMapper categoryMapper;

    @Override
    public CategoryResponse createCategory(CategoryCreationRequest request) {
        log.info("Creating category with name: {}", request.getName());

        String normalizedName = request.getName().trim();
        if (categoryRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new AppException(ErrorCode.CATEGORY_EXISTED);
        }

        Category category = categoryMapper.toCategory(request);
        category.setName(normalizedName);
        if (request.getDescription() != null) {
            category.setDescription(request.getDescription().trim());
        }

        Category savedCategory = categoryRepository.save(category);
        return categoryMapper.toCategoryResponse(savedCategory);
    }

    @Override
    public List<CategoryResponse> getAllCategories() {
        log.info("Fetching all categories");
        List<Category> categories = categoryRepository.findAll();
        return categoryMapper.toCategoryResponseList(categories);
    }

    @Override
    public CategoryResponse getCategoryById(Integer categoryId) {
        log.info("Fetching category by id: {}", categoryId);
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
        return categoryMapper.toCategoryResponse(category);
    }

    @Override
    public CategoryResponse updateCategory(Integer categoryId, CategoryUpdateRequest request) {
        log.info("Updating category id: {}", categoryId);

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        String normalizedName = request.getName().trim();
        if (categoryRepository.existsByNameIgnoreCaseAndCategoryIdNot(normalizedName, categoryId)) {
            throw new AppException(ErrorCode.CATEGORY_EXISTED);
        }

        categoryMapper.updateCategory(category, request);
        category.setName(normalizedName);
        if (request.getDescription() != null) {
            category.setDescription(request.getDescription().trim());
        }

        Category updatedCategory = categoryRepository.save(category);
        return categoryMapper.toCategoryResponse(updatedCategory);
    }

    @Override
    public void deleteCategory(Integer categoryId) {
        log.info("Deleting category id: {}", categoryId);

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        if (category.getProducts() != null && !category.getProducts().isEmpty()) {
            throw new AppException(ErrorCode.CATEGORY_HAS_PRODUCTS);
        }

        categoryRepository.delete(category);
    }
}
