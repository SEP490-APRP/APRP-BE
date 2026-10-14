package com.sang.be_capnong.service;

import com.sang.be_capnong.dto.response.PageResponse;
import com.sang.be_capnong.dto.response.ProductResponse;
import com.sang.be_capnong.entity.Category;
import com.sang.be_capnong.entity.Product;
import com.sang.be_capnong.entity.User;
import com.sang.be_capnong.exception.AppException;
import com.sang.be_capnong.exception.ErrorCode;
import com.sang.be_capnong.mapper.ProductMapper;
import com.sang.be_capnong.repository.*;
import com.sang.be_capnong.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private ProductMysteryRepository productMysteryRepository;
    @Mock
    private MysteryBoxRepository mysteryBoxRepository;
    @Mock
    private ProductComboRepository productComboRepository;
    @Mock
    private BuildComboRepository buildComboRepository;
    @Mock
    private ProductImageRepository productImageRepository;
    @Mock
    private ModerationResultRepository moderationResultRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private User shopOwner;
    private Product activeProduct;
    private Product pendingProduct;
    private ProductResponse activeProductResponse;
    private ProductResponse pendingProductResponse;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        shopOwner = User.builder()
                .id(10)
                .fullName("Nông trại Cần Thơ")
                .build();

        Category category = Category.builder()
                .categoryId(1)
                .name("Trái cây")
                .build();

        activeProduct = Product.builder()
                .productId(101)
                .productName("Xoài cát Hòa Lộc")
                .status("ACTIVE")
                .shop(shopOwner)
                .category(category)
                .images(new ArrayList<>())
                .build();

        pendingProduct = Product.builder()
                .productId(102)
                .productName("Sầu riêng Ri6 đang duyệt")
                .status("PENDING")
                .shop(shopOwner)
                .category(category)
                .images(new ArrayList<>())
                .build();

        activeProductResponse = ProductResponse.builder()
                .productId(101)
                .productName("Xoài cát Hòa Lộc")
                .status("ACTIVE")
                .images(new ArrayList<>())
                .build();

        pendingProductResponse = ProductResponse.builder()
                .productId(102)
                .productName("Sầu riêng Ri6 đang duyệt")
                .status("PENDING")
                .images(new ArrayList<>())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void mockUser(int userId, String role) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                String.valueOf(userId),
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // ==========================================
    // 1. getProductById Tests
    // ==========================================

    @Test
    void getProductById_activeProduct_guestViewer_success() {
        when(productRepository.findById(101)).thenReturn(Optional.of(activeProduct));
        when(productMapper.toProductResponse(activeProduct)).thenReturn(activeProductResponse);

        ProductResponse result = productService.getProductById(101);

        assertThat(result).isNotNull();
        assertThat(result.getProductId()).isEqualTo(101);
    }

    @Test
    void getProductById_pendingProduct_guestViewer_throwsNotFound() {
        when(productRepository.findById(102)).thenReturn(Optional.of(pendingProduct));

        assertThatThrownBy(() -> productService.getProductById(102))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    void getProductById_pendingProduct_otherUserViewer_throwsNotFound() {
        mockUser(999, "BUYER");
        when(productRepository.findById(102)).thenReturn(Optional.of(pendingProduct));

        assertThatThrownBy(() -> productService.getProductById(102))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    void getProductById_pendingProduct_ownerViewer_success() {
        mockUser(10, "SHOP"); // owner id is 10
        when(productRepository.findById(102)).thenReturn(Optional.of(pendingProduct));
        when(productMapper.toProductResponse(pendingProduct)).thenReturn(pendingProductResponse);

        ProductResponse result = productService.getProductById(102);

        assertThat(result).isNotNull();
        assertThat(result.getProductId()).isEqualTo(102);
    }

    @Test
    void getProductById_pendingProduct_adminViewer_success() {
        mockUser(1, "ADMIN");
        when(productRepository.findById(102)).thenReturn(Optional.of(pendingProduct));
        when(productMapper.toProductResponse(pendingProduct)).thenReturn(pendingProductResponse);

        ProductResponse result = productService.getProductById(102);

        assertThat(result).isNotNull();
        assertThat(result.getProductId()).isEqualTo(102);
    }

    // ==========================================
    // 2. getProducts Tests
    // ==========================================

    @Test
    void getProducts_invalidStatus_throwsInvalidStatus() {
        assertThatThrownBy(() -> productService.getProducts(null, null, "INVALID_XYZ", 1, 10, null, null))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_STATUS);
    }

    @Test
    void getProducts_pageSizeExceedsMax_cappedAt100() {
        Page<Product> page = new PageImpl<>(List.of(activeProduct));
        when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(productMapper.toProductResponse(activeProduct)).thenReturn(activeProductResponse);

        productService.getProducts(null, null, "ACTIVE", 1, 500, null, null);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(productRepository).findAll(any(Specification.class), pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageSize()).isEqualTo(100);
    }

    @Test
    void getProducts_guestRequestsNonActiveStatus_enforcesActiveOnly() {
        // Guest asks for PENDING, should be forced to ACTIVE
        Page<Product> page = new PageImpl<>(List.of(activeProduct));
        when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(productMapper.toProductResponse(activeProduct)).thenReturn(activeProductResponse);

        PageResponse<ProductResponse> response = productService.getProducts(null, null, "PENDING", 1, 10, null, null);

        assertThat(response).isNotNull();
        verify(productRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void getProducts_adminRequestsNonActiveStatus_allowed() {
        mockUser(1, "ADMIN");
        Page<Product> page = new PageImpl<>(List.of(pendingProduct));
        when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(productMapper.toProductResponse(pendingProduct)).thenReturn(pendingProductResponse);

        PageResponse<ProductResponse> response = productService.getProducts(null, null, "PENDING", 1, 10, null, null);

        assertThat(response).isNotNull();
        verify(productRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    // ==========================================
    // 3. getMyProducts Tests
    // ==========================================

    @Test
    void getMyProducts_invalidStatus_throwsInvalidStatus() {
        mockUser(10, "SHOP");

        assertThatThrownBy(() -> productService.getMyProducts("UNKNOWN_XYZ", 1, 10))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_STATUS);
    }

    @Test
    void getMyProducts_pageSizeExceedsMax_cappedAt100() {
        mockUser(10, "SHOP");
        Page<Product> page = new PageImpl<>(List.of(activeProduct));
        when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(productMapper.toProductResponseList(any())).thenReturn(List.of(activeProductResponse));

        productService.getMyProducts("ACTIVE", 1, 350);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(productRepository).findAll(any(Specification.class), pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageSize()).isEqualTo(100);
    }
}
