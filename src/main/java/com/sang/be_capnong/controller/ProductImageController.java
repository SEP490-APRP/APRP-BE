package com.sang.be_capnong.controller;

import com.sang.be_capnong.dto.request.ProductImageCreationRequest;
import com.sang.be_capnong.dto.request.ProductImageUpdateRequest;
import com.sang.be_capnong.dto.response.ApiResponse;
import com.sang.be_capnong.dto.response.ProductImageResponse;
import com.sang.be_capnong.service.ProductImageService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products/{productId}/images")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ProductImageController {

    ProductImageService productImageService;

    @PostMapping
    @PreAuthorize("hasAuthority('SHOP')")
    public ApiResponse<ProductImageResponse> addImage(
            @PathVariable("productId") Integer productId,
            @RequestBody @Valid ProductImageCreationRequest request
    ) {
        log.info("REST request to add image to product id: {}", productId);
        return ApiResponse.<ProductImageResponse>builder()
                .result(productImageService.addImage(productId, request))
                .build();
    }

    @GetMapping
    public ApiResponse<List<ProductImageResponse>> getProductImages(
            @PathVariable("productId") Integer productId
    ) {
        log.info("REST request to get images for product id: {}", productId);
        return ApiResponse.<List<ProductImageResponse>>builder()
                .result(productImageService.getProductImages(productId))
                .build();
    }

    @PutMapping("/{imageId}")
    @PreAuthorize("hasAuthority('SHOP')")
    public ApiResponse<ProductImageResponse> updateImage(
            @PathVariable("productId") Integer productId,
            @PathVariable("imageId") Long imageId,
            @RequestBody @Valid ProductImageUpdateRequest request
    ) {
        log.info("REST request to update image id: {} for product id: {}", imageId, productId);
        return ApiResponse.<ProductImageResponse>builder()
                .result(productImageService.updateImage(productId, imageId, request))
                .build();
    }

    @PatchMapping("/{imageId}/primary")
    @PreAuthorize("hasAuthority('SHOP')")
    public ApiResponse<ProductImageResponse> setPrimaryImage(
            @PathVariable("productId") Integer productId,
            @PathVariable("imageId") Long imageId
    ) {
        log.info("REST request to set primary image id: {} for product id: {}", imageId, productId);
        return ApiResponse.<ProductImageResponse>builder()
                .result(productImageService.setPrimaryImage(productId, imageId))
                .build();
    }

    @DeleteMapping("/{imageId}")
    @PreAuthorize("hasAuthority('SHOP')")
    public ApiResponse<Void> deleteImage(
            @PathVariable("productId") Integer productId,
            @PathVariable("imageId") Long imageId
    ) {
        log.info("REST request to delete image id: {} for product id: {}", imageId, productId);
        productImageService.deleteImage(productId, imageId);
        return ApiResponse.<Void>builder()
                .message("Product image deleted successfully")
                .build();
    }
}
