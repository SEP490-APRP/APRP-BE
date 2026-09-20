package com.sang.be_capnong.controller;

import com.sang.be_capnong.dto.request.ProductCreationRequest;
import com.sang.be_capnong.dto.request.ProductRejectRequest;
import com.sang.be_capnong.dto.request.ProductUpdateRequest;
import com.sang.be_capnong.dto.response.ApiResponse;
import com.sang.be_capnong.dto.response.PageResponse;
import com.sang.be_capnong.dto.response.ProductResponse;
import com.sang.be_capnong.service.ProductService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ProductController {

    ProductService productService;

    @PostMapping
    @PreAuthorize("hasAuthority('SHOP')")
    public ApiResponse<ProductResponse> createProduct(@RequestBody @Valid ProductCreationRequest request) {
        log.info("REST request to create product: {}", request.getProductName());
        return ApiResponse.<ProductResponse>builder()
                .result(productService.createProduct(request))
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductResponse> getProductById(@PathVariable("id") Integer id) {
        log.info("REST request to get product by id: {}", id);
        return ApiResponse.<ProductResponse>builder()
                .result(productService.getProductById(id))
                .build();
    }

    @GetMapping
    public ApiResponse<PageResponse<ProductResponse>> getProducts(
            @RequestParam(name = "categoryId", required = false) Integer categoryId,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "status", required = false, defaultValue = "ACTIVE") String status,
            @RequestParam(name = "page", required = false, defaultValue = "1") int page,
            @RequestParam(name = "size", required = false, defaultValue = "10") int size,
            @RequestParam(name = "sortBy", required = false, defaultValue = "productId") String sortBy,
            @RequestParam(name = "sortDirection", required = false, defaultValue = "desc") String sortDirection
    ) {
        log.info("REST request to get catalog products");
        return ApiResponse.<PageResponse<ProductResponse>>builder()
                .result(productService.getProducts(categoryId, keyword, status, page, size, sortBy, sortDirection))
                .build();
    }

    @GetMapping("/my-shop")
    @PreAuthorize("hasAuthority('SHOP')")
    public ApiResponse<PageResponse<ProductResponse>> getMyProducts(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", required = false, defaultValue = "1") int page,
            @RequestParam(name = "size", required = false, defaultValue = "10") int size
    ) {
        log.info("REST request to get products for my shop");
        return ApiResponse.<PageResponse<ProductResponse>>builder()
                .result(productService.getMyProducts(status, page, size))
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SHOP')")
    public ApiResponse<ProductResponse> updateProduct(
            @PathVariable("id") Integer id,
            @RequestBody @Valid ProductUpdateRequest request
    ) {
        log.info("REST request to update product id: {}", id);
        return ApiResponse.<ProductResponse>builder()
                .result(productService.updateProduct(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SHOP', 'ADMIN')")
    public ApiResponse<Void> deleteProduct(@PathVariable("id") Integer id) {
        log.info("REST request to delete product id: {}", id);
        productService.deleteProduct(id);
        return ApiResponse.<Void>builder()
                .message("Product deleted successfully")
                .build();
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ApiResponse<ProductResponse> approveProduct(@PathVariable("id") Integer id) {
        log.info("REST request to approve product id: {}", id);
        return ApiResponse.<ProductResponse>builder()
                .result(productService.approveProduct(id))
                .build();
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ApiResponse<ProductResponse> rejectProduct(
            @PathVariable("id") Integer id,
            @RequestBody @Valid ProductRejectRequest request
    ) {
        log.info("REST request to reject product id: {}", id);
        return ApiResponse.<ProductResponse>builder()
                .result(productService.rejectProduct(id, request))
                .build();
    }
}
