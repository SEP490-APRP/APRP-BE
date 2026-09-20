package com.sang.be_capnong.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sang.be_capnong.config.CustomJwtDecoder;
import com.sang.be_capnong.dto.request.CategoryCreationRequest;
import com.sang.be_capnong.dto.request.CategoryUpdateRequest;
import com.sang.be_capnong.dto.response.CategoryResponse;
import com.sang.be_capnong.exception.AppException;
import com.sang.be_capnong.exception.ErrorCode;
import com.sang.be_capnong.service.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
class CategoryControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    CategoryService categoryService;

    @MockBean
    CustomJwtDecoder customJwtDecoder;

    CategoryResponse sampleResponse;

    @BeforeEach
    void setUp() {
        sampleResponse = CategoryResponse.builder()
                .categoryId(1)
                .name("Trái cây")
                .description("Trái cây giải cứu")
                .build();
    }

    @Test
    void createCategory_valid_returnsSuccess() throws Exception {
        CategoryCreationRequest request = CategoryCreationRequest.builder()
                .name("Trái cây")
                .description("Trái cây giải cứu")
                .build();

        when(categoryService.createCategory(any(CategoryCreationRequest.class))).thenReturn(sampleResponse);

        mockMvc.perform(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.name").value("Trái cây"))
                .andExpect(jsonPath("$.result.categoryId").value(1));
    }

    @Test
    void createCategory_blankName_returnsBadRequest() throws Exception {
        CategoryCreationRequest request = CategoryCreationRequest.builder()
                .name("")
                .description("Mô tả")
                .build();

        mockMvc.perform(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.CATEGORY_NAME_REQUIRED.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorCode.CATEGORY_NAME_REQUIRED.getMessage()));
    }

    @Test
    void createCategory_nameTooLong_returnsBadRequest() throws Exception {
        CategoryCreationRequest request = CategoryCreationRequest.builder()
                .name("a".repeat(101))
                .description("Mô tả")
                .build();

        mockMvc.perform(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.CATEGORY_NAME_TOO_LONG.getCode()));
    }

    @Test
    void getAllCategories_returnsList() throws Exception {
        when(categoryService.getAllCategories()).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result[0].name").value("Trái cây"));
    }

    @Test
    void getCategoryById_exists_returnsCategory() throws Exception {
        when(categoryService.getCategoryById(1)).thenReturn(sampleResponse);

        mockMvc.perform(get("/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.categoryId").value(1));
    }

    @Test
    void getCategoryById_notFound_returns404() throws Exception {
        when(categoryService.getCategoryById(999)).thenThrow(new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        mockMvc.perform(get("/categories/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.CATEGORY_NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorCode.CATEGORY_NOT_FOUND.getMessage()));
    }

    @Test
    void updateCategory_valid_returnsSuccess() throws Exception {
        CategoryUpdateRequest request = CategoryUpdateRequest.builder()
                .name("Trái cây miền Tây")
                .description("Cập nhật")
                .build();

        CategoryResponse updatedResponse = CategoryResponse.builder()
                .categoryId(1)
                .name("Trái cây miền Tây")
                .description("Cập nhật")
                .build();

        when(categoryService.updateCategory(eq(1), any(CategoryUpdateRequest.class))).thenReturn(updatedResponse);

        mockMvc.perform(put("/categories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.name").value("Trái cây miền Tây"));
    }

    @Test
    void updateCategory_blankName_returnsBadRequest() throws Exception {
        CategoryUpdateRequest request = CategoryUpdateRequest.builder()
                .name("")
                .description("Cập nhật")
                .build();

        mockMvc.perform(put("/categories/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.CATEGORY_NAME_REQUIRED.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorCode.CATEGORY_NAME_REQUIRED.getMessage()));
    }

    @Test
    void deleteCategory_success_returnsOk() throws Exception {
        doNothing().when(categoryService).deleteCategory(1);

        mockMvc.perform(delete("/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.message").value("Category deleted successfully"));
    }
}
