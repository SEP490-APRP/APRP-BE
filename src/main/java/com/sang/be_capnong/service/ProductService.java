package com.sang.be_capnong.service;

import com.sang.be_capnong.dto.request.ProductCreationRequest;
import com.sang.be_capnong.dto.request.ProductUpdateRequest;
import com.sang.be_capnong.dto.response.PageResponse;
import com.sang.be_capnong.dto.response.ProductResponse;

public interface ProductService {

    ProductResponse createProduct(ProductCreationRequest request);

    ProductResponse getProductById(Integer productId);

    PageResponse<ProductResponse> getProducts(
            Integer categoryId,
            String keyword,
            String status,
            int page,
            int size,
            String sortBy,
            String sortDirection
    );

    PageResponse<ProductResponse> getMyProducts(
            String status,
            int page,
            int size
    );

    ProductResponse updateProduct(Integer productId, ProductUpdateRequest request);

    void deleteProduct(Integer productId);
}
