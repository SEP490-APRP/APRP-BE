package com.sang.be_capnong.service;

import com.sang.be_capnong.dto.request.CategoryCreationRequest;
import com.sang.be_capnong.dto.request.CategoryUpdateRequest;
import com.sang.be_capnong.dto.response.CategoryResponse;
import com.sang.be_capnong.entity.Category;
import com.sang.be_capnong.entity.Product;
import com.sang.be_capnong.exception.AppException;
import com.sang.be_capnong.exception.ErrorCode;
import com.sang.be_capnong.mapper.CategoryMapper;
import com.sang.be_capnong.repository.CategoryRepository;
import com.sang.be_capnong.service.impl.CategoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    CategoryRepository categoryRepository;

    @Mock
    CategoryMapper categoryMapper;

    @InjectMocks
    CategoryServiceImpl categoryService;

    Category sampleCategory;
    CategoryResponse sampleResponse;
    CategoryCreationRequest creationRequest;
    CategoryUpdateRequest updateRequest;

    @BeforeEach
    void setUp() {
        sampleCategory = Category.builder()
                .categoryId(1)
                .name("Rau củ")
                .description("Các loại rau củ cứu hộ")
                .build();

        sampleResponse = CategoryResponse.builder()
                .categoryId(1)
                .name("Rau củ")
                .description("Các loại rau củ cứu hộ")
                .build();

        creationRequest = CategoryCreationRequest.builder()
                .name("Rau củ")
                .description("Các loại rau củ cứu hộ")
                .build();

        updateRequest = CategoryUpdateRequest.builder()
                .name("Rau củ sạch")
                .description("Mô tả mới")
                .build();
    }

    @Test
    void createCategory_success() {
        when(categoryRepository.existsByNameIgnoreCase("Rau củ")).thenReturn(false);
        when(categoryMapper.toCategory(creationRequest)).thenReturn(sampleCategory);
        when(categoryRepository.save(any(Category.class))).thenReturn(sampleCategory);
        when(categoryMapper.toCategoryResponse(sampleCategory)).thenReturn(sampleResponse);

        CategoryResponse result = categoryService.createCategory(creationRequest);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Rau củ");
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void createCategory_trimsNameAndDescription() {
        creationRequest = CategoryCreationRequest.builder()
                .name("  Rau củ  ")
                .description("  Mô tả rau củ  ")
                .build();

        when(categoryRepository.existsByNameIgnoreCase("Rau củ")).thenReturn(false);
        when(categoryMapper.toCategory(creationRequest)).thenReturn(sampleCategory);
        when(categoryRepository.save(any(Category.class))).thenReturn(sampleCategory);
        when(categoryMapper.toCategoryResponse(sampleCategory)).thenReturn(sampleResponse);

        categoryService.createCategory(creationRequest);

        verify(categoryRepository).existsByNameIgnoreCase("Rau củ");
        assertThat(sampleCategory.getName()).isEqualTo("Rau củ");
        assertThat(sampleCategory.getDescription()).isEqualTo("Mô tả rau củ");
    }

    @Test
    void createCategory_duplicateName_throwsAppException() {
        when(categoryRepository.existsByNameIgnoreCase("Rau củ")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.createCategory(creationRequest))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CATEGORY_EXISTED);

        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void getAllCategories_success() {
        when(categoryRepository.findAll()).thenReturn(List.of(sampleCategory));
        when(categoryMapper.toCategoryResponseList(anyList())).thenReturn(List.of(sampleResponse));

        List<CategoryResponse> result = categoryService.getAllCategories();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Rau củ");
    }

    @Test
    void getCategoryById_success() {
        when(categoryRepository.findById(1)).thenReturn(Optional.of(sampleCategory));
        when(categoryMapper.toCategoryResponse(sampleCategory)).thenReturn(sampleResponse);

        CategoryResponse result = categoryService.getCategoryById(1);

        assertThat(result).isNotNull();
        assertThat(result.getCategoryId()).isEqualTo(1);
    }

    @Test
    void getCategoryById_notFound_throwsAppException() {
        when(categoryRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getCategoryById(999))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CATEGORY_NOT_FOUND);
    }

    @Test
    void updateCategory_success() {
        when(categoryRepository.findById(1)).thenReturn(Optional.of(sampleCategory));
        when(categoryRepository.existsByNameIgnoreCaseAndCategoryIdNot("Rau củ sạch", 1)).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(sampleCategory);
        when(categoryMapper.toCategoryResponse(sampleCategory)).thenReturn(sampleResponse);

        CategoryResponse result = categoryService.updateCategory(1, updateRequest);

        assertThat(result).isNotNull();
        verify(categoryMapper).updateCategory(sampleCategory, updateRequest);
        verify(categoryRepository).save(sampleCategory);
    }

    @Test
    void updateCategory_duplicateName_throwsAppException() {
        when(categoryRepository.findById(1)).thenReturn(Optional.of(sampleCategory));
        when(categoryRepository.existsByNameIgnoreCaseAndCategoryIdNot("Rau củ sạch", 1)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.updateCategory(1, updateRequest))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CATEGORY_EXISTED);

        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void updateCategory_notFound_throwsAppException() {
        when(categoryRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.updateCategory(999, updateRequest))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CATEGORY_NOT_FOUND);

        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void deleteCategory_success() {
        sampleCategory.setProducts(Collections.emptyList());
        when(categoryRepository.findById(1)).thenReturn(Optional.of(sampleCategory));

        categoryService.deleteCategory(1);

        verify(categoryRepository).delete(sampleCategory);
    }

    @Test
    void deleteCategory_hasProducts_throwsAppException() {
        sampleCategory.setProducts(List.of(new Product()));
        when(categoryRepository.findById(1)).thenReturn(Optional.of(sampleCategory));

        assertThatThrownBy(() -> categoryService.deleteCategory(1))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CATEGORY_HAS_PRODUCTS);

        verify(categoryRepository, never()).delete(any(Category.class));
    }

    @Test
    void deleteCategory_notFound_throwsAppException() {
        when(categoryRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.deleteCategory(999))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CATEGORY_NOT_FOUND);

        verify(categoryRepository, never()).delete(any(Category.class));
    }
}
