package com.sang.be_capnong.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sang.be_capnong.config.CustomJwtDecoder;
import com.sang.be_capnong.dto.request.ProductImageCreationRequest;
import com.sang.be_capnong.dto.request.ProductImageUpdateRequest;
import com.sang.be_capnong.dto.response.ProductImageResponse;
import com.sang.be_capnong.exception.ErrorCode;
import com.sang.be_capnong.service.ProductImageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductImageController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductImageControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    ProductImageService productImageService;

    @MockBean
    CustomJwtDecoder customJwtDecoder;

    ProductImageResponse sampleResponse;

    @BeforeEach
    void setUp() {
        sampleResponse = ProductImageResponse.builder()
                .id(1L)
                .imageUrl("https://cdn.capnong.vn/cam-1.jpg")
                .isPrimary(true)
                .displayOrder(0)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void addImage_valid_returnsSuccess() throws Exception {
        ProductImageCreationRequest request = ProductImageCreationRequest.builder()
                .imageUrl("https://cdn.capnong.vn/cam-1.jpg")
                .isPrimary(true)
                .displayOrder(0)
                .build();

        when(productImageService.addImage(eq(10), any(ProductImageCreationRequest.class))).thenReturn(sampleResponse);

        mockMvc.perform(post("/products/10/images")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.id").value(1))
                .andExpect(jsonPath("$.result.imageUrl").value("https://cdn.capnong.vn/cam-1.jpg"));
    }

    @Test
    void addImage_missingUrl_returnsBadRequest() throws Exception {
        ProductImageCreationRequest request = ProductImageCreationRequest.builder()
                .imageUrl("")
                .isPrimary(true)
                .build();

        mockMvc.perform(post("/products/10/images")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.IMAGE_URL_REQUIRED.getCode()));
    }

    @Test
    void addImage_urlTooLong_returnsBadRequest() throws Exception {
        ProductImageCreationRequest request = ProductImageCreationRequest.builder()
                .imageUrl("a".repeat(501))
                .build();

        mockMvc.perform(post("/products/10/images")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.IMAGE_URL_TOO_LONG.getCode()));
    }

    @Test
    void getProductImages_returnsList() throws Exception {
        when(productImageService.getProductImages(10)).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/products/10/images"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result[0].id").value(1));
    }

    @Test
    void updateImage_returnsSuccess() throws Exception {
        ProductImageUpdateRequest request = ProductImageUpdateRequest.builder()
                .imageUrl("https://cdn.capnong.vn/cam-updated.jpg")
                .isPrimary(false)
                .displayOrder(2)
                .build();

        ProductImageResponse updatedResponse = ProductImageResponse.builder()
                .id(1L)
                .imageUrl("https://cdn.capnong.vn/cam-updated.jpg")
                .isPrimary(false)
                .displayOrder(2)
                .build();

        when(productImageService.updateImage(eq(10), eq(1L), any(ProductImageUpdateRequest.class)))
                .thenReturn(updatedResponse);

        mockMvc.perform(put("/products/10/images/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.imageUrl").value("https://cdn.capnong.vn/cam-updated.jpg"));
    }

    @Test
    void updateImage_urlTooLong_returnsBadRequest() throws Exception {
        ProductImageUpdateRequest request = ProductImageUpdateRequest.builder()
                .imageUrl("a".repeat(501))
                .build();

        mockMvc.perform(put("/products/10/images/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.IMAGE_URL_TOO_LONG.getCode()));
    }

    @Test
    void setPrimaryImage_returnsSuccess() throws Exception {
        when(productImageService.setPrimaryImage(10, 1L)).thenReturn(sampleResponse);

        mockMvc.perform(patch("/products/10/images/1/primary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.isPrimary").value(true));
    }

    @Test
    void deleteImage_returnsSuccess() throws Exception {
        doNothing().when(productImageService).deleteImage(10, 1L);

        mockMvc.perform(delete("/products/10/images/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.message").value("Product image deleted successfully"));
    }
}
