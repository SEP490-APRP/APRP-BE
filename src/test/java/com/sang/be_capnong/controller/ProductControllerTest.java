package com.sang.be_capnong.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sang.be_capnong.config.CustomJwtDecoder;
import com.sang.be_capnong.dto.request.ProductCreationRequest;
import com.sang.be_capnong.dto.request.ProductUpdateRequest;
import com.sang.be_capnong.dto.response.PageResponse;
import com.sang.be_capnong.dto.response.ProductResponse;
import com.sang.be_capnong.exception.AppException;
import com.sang.be_capnong.exception.ErrorCode;
import com.sang.be_capnong.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    ProductService productService;

    @MockBean
    CustomJwtDecoder customJwtDecoder;

    ProductResponse sampleResponse;

    @BeforeEach
    void setUp() {
        sampleResponse = ProductResponse.builder()
                .productId(1)
                .productName("Cà chua giải cứu")
                .unit("kg")
                .sellingPrice(BigDecimal.valueOf(10000))
                .stockQuantity(50)
                .status("ACTIVE")
                .expiryDate(LocalDate.now().plusDays(5))
                .build();
    }

    @Test
    void createProduct_valid_returnsSuccess() throws Exception {
        ProductCreationRequest request = ProductCreationRequest.builder()
                .productName("Cà chua giải cứu")
                .categoryId(1)
                .unit("kg")
                .sellingPrice(BigDecimal.valueOf(10000))
                .stockQuantity(50)
                .expiryDate(LocalDate.now().plusDays(5))
                .build();

        when(productService.createProduct(any(ProductCreationRequest.class))).thenReturn(sampleResponse);

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.productName").value("Cà chua giải cứu"))
                .andExpect(jsonPath("$.result.productId").value(1));
    }

    @Test
    void createProduct_missingName_returnsBadRequest() throws Exception {
        ProductCreationRequest request = ProductCreationRequest.builder()
                .productName("")
                .categoryId(1)
                .unit("kg")
                .sellingPrice(BigDecimal.valueOf(10000))
                .stockQuantity(50)
                .expiryDate(LocalDate.now().plusDays(5))
                .build();

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.PRODUCT_NAME_REQUIRED.getCode()));
    }

    @Test
    void createProduct_negativePrice_returnsBadRequest() throws Exception {
        ProductCreationRequest request = ProductCreationRequest.builder()
                .productName("Cà chua")
                .categoryId(1)
                .unit("kg")
                .sellingPrice(BigDecimal.valueOf(-5000))
                .stockQuantity(50)
                .expiryDate(LocalDate.now().plusDays(5))
                .build();

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.PRODUCT_PRICE_INVALID.getCode()));
    }

    @Test
    void createProduct_missingCategory_returnsBadRequest() throws Exception {
        ProductCreationRequest request = ProductCreationRequest.builder()
                .productName("Cà chua")
                .unit("kg")
                .sellingPrice(BigDecimal.valueOf(10000))
                .stockQuantity(50)
                .expiryDate(LocalDate.now().plusDays(5))
                .build();

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.PRODUCT_CATEGORY_REQUIRED.getCode()));
    }

    @Test
    void createProduct_missingUnit_returnsBadRequest() throws Exception {
        ProductCreationRequest request = ProductCreationRequest.builder()
                .productName("Cà chua")
                .categoryId(1)
                .sellingPrice(BigDecimal.valueOf(10000))
                .stockQuantity(50)
                .expiryDate(LocalDate.now().plusDays(5))
                .build();

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.PRODUCT_UNIT_REQUIRED.getCode()));
    }

    @Test
    void createProduct_missingPrice_returnsBadRequest() throws Exception {
        ProductCreationRequest request = ProductCreationRequest.builder()
                .productName("Cà chua")
                .categoryId(1)
                .unit("kg")
                .stockQuantity(50)
                .expiryDate(LocalDate.now().plusDays(5))
                .build();

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.PRODUCT_PRICE_REQUIRED.getCode()));
    }

    @Test
    void createProduct_missingStock_returnsBadRequest() throws Exception {
        ProductCreationRequest request = ProductCreationRequest.builder()
                .productName("Cà chua")
                .categoryId(1)
                .unit("kg")
                .sellingPrice(BigDecimal.valueOf(10000))
                .expiryDate(LocalDate.now().plusDays(5))
                .build();

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.PRODUCT_STOCK_REQUIRED.getCode()));
    }

    @Test
    void createProduct_missingExpiry_returnsBadRequest() throws Exception {
        ProductCreationRequest request = ProductCreationRequest.builder()
                .productName("Cà chua")
                .categoryId(1)
                .unit("kg")
                .sellingPrice(BigDecimal.valueOf(10000))
                .stockQuantity(50)
                .build();

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.PRODUCT_EXPIRY_REQUIRED.getCode()));
    }

    @Test
    void createProduct_negativeStock_returnsBadRequest() throws Exception {
        ProductCreationRequest request = ProductCreationRequest.builder()
                .productName("Cà chua")
                .categoryId(1)
                .unit("kg")
                .sellingPrice(BigDecimal.valueOf(10000))
                .stockQuantity(-1)
                .expiryDate(LocalDate.now().plusDays(5))
                .build();

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.PRODUCT_STOCK_INVALID.getCode()));
    }

    @Test
    void createProduct_discountPercentOutOfRange_returnsBadRequest() throws Exception {
        ProductCreationRequest request = ProductCreationRequest.builder()
                .productName("Cà chua")
                .categoryId(1)
                .unit("kg")
                .sellingPrice(BigDecimal.valueOf(10000))
                .stockQuantity(50)
                .expiryDate(LocalDate.now().plusDays(5))
                .discountPercent(150)
                .build();

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.DISCOUNT_PERCENT_INVALID.getCode()));
    }

    @Test
    void createProduct_nameTooLong_returnsBadRequest() throws Exception {
        ProductCreationRequest request = ProductCreationRequest.builder()
                .productName("a".repeat(256))
                .categoryId(1)
                .unit("kg")
                .sellingPrice(BigDecimal.valueOf(10000))
                .stockQuantity(50)
                .expiryDate(LocalDate.now().plusDays(5))
                .build();

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.PRODUCT_NAME_TOO_LONG.getCode()));
    }

    @Test
    void getProductById_exists_returnsProduct() throws Exception {
        when(productService.getProductById(1)).thenReturn(sampleResponse);

        mockMvc.perform(get("/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.productId").value(1));
    }

    @Test
    void getProductById_notFound_returns404() throws Exception {
        when(productService.getProductById(999)).thenThrow(new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        mockMvc.perform(get("/products/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.PRODUCT_NOT_FOUND.getCode()));
    }

    @Test
    void getProducts_returnsPageResponse() throws Exception {
        PageResponse<ProductResponse> pageResponse = PageResponse.<ProductResponse>builder()
                .page(0)
                .totalPages(1)
                .size(10)
                .totalElements(1)
                .first(true)
                .last(true)
                .content(List.of(sampleResponse))
                .build();

        when(productService.getProducts(any(), any(), any(), anyInt(), anyInt(), any(), any()))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.content[0].productName").value("Cà chua giải cứu"));
    }

    @Test
    void getMyProducts_returnsPageResponse() throws Exception {
        PageResponse<ProductResponse> pageResponse = PageResponse.<ProductResponse>builder()
                .page(0)
                .totalPages(1)
                .size(10)
                .totalElements(1)
                .first(true)
                .last(true)
                .content(List.of(sampleResponse))
                .build();

        when(productService.getMyProducts(any(), anyInt(), anyInt()))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/products/my-shop"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.content[0].productName").value("Cà chua giải cứu"));
    }

    @Test
    void updateProduct_valid_returnsSuccess() throws Exception {
        ProductUpdateRequest request = ProductUpdateRequest.builder()
                .productName("Cà chua chín mọng")
                .categoryId(1)
                .unit("kg")
                .sellingPrice(BigDecimal.valueOf(12000))
                .stockQuantity(40)
                .expiryDate(LocalDate.now().plusDays(3))
                .build();

        ProductResponse updatedResponse = ProductResponse.builder()
                .productId(1)
                .productName("Cà chua chín mọng")
                .build();

        when(productService.updateProduct(eq(1), any(ProductUpdateRequest.class))).thenReturn(updatedResponse);

        mockMvc.perform(put("/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.productName").value("Cà chua chín mọng"));
    }

    @Test
    void updateProduct_blankName_returnsBadRequest() throws Exception {
        ProductUpdateRequest request = ProductUpdateRequest.builder()
                .productName("")
                .categoryId(1)
                .unit("kg")
                .sellingPrice(BigDecimal.valueOf(12000))
                .stockQuantity(40)
                .expiryDate(LocalDate.now().plusDays(3))
                .build();

        mockMvc.perform(put("/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.PRODUCT_NAME_REQUIRED.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorCode.PRODUCT_NAME_REQUIRED.getMessage()));
    }

    @Test
    void deleteProduct_success_returnsOk() throws Exception {
        doNothing().when(productService).deleteProduct(1);

        mockMvc.perform(delete("/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.message").value("Product deleted successfully"));
    }
}
