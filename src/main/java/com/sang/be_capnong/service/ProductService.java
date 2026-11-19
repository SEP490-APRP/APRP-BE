package com.sang.be_capnong.service;

import com.sang.be_capnong.dto.request.ProductCreationRequest;
import com.sang.be_capnong.dto.request.ProductUpdateRequest;
import com.sang.be_capnong.dto.response.PageResponse;
import com.sang.be_capnong.dto.response.ProductResponse;

import com.sang.be_capnong.dto.request.ProductRejectRequest;

import java.math.BigDecimal;

public interface ProductService {

    ProductResponse createProduct(ProductCreationRequest request);

    ProductResponse getProductById(Integer productId);

    PageResponse<ProductResponse> getProducts(
            Integer categoryId,
            String keyword,
            String status,
            Integer shopId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String origin,
            Boolean isVietgap,
            int page,
            int size,
            String sortBy,
            String sortDirection
    );

    default PageResponse<ProductResponse> getProducts(
            Integer categoryId,
            String keyword,
            String status,
            Integer shopId,
            int page,
            int size,
            String sortBy,
            String sortDirection
    ) {
        return getProducts(categoryId, keyword, status, shopId, null, null, null, null, page, size, sortBy, sortDirection);
    }

    PageResponse<ProductResponse> getMyProducts(
            String status,
            int page,
            int size
    );

    ProductResponse updateProduct(Integer productId, ProductUpdateRequest request);

    void deleteProduct(Integer productId);

    ProductResponse approveProduct(Integer productId);

    ProductResponse rejectProduct(Integer productId, ProductRejectRequest request);
}
