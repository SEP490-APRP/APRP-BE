package com.sang.be_capnong.service;

import com.sang.be_capnong.dto.request.ProductImageCreationRequest;
import com.sang.be_capnong.dto.request.ProductImageUpdateRequest;
import com.sang.be_capnong.dto.response.ProductImageResponse;

import java.util.List;

public interface ProductImageService {

    ProductImageResponse addImage(Integer productId, ProductImageCreationRequest request);

    List<ProductImageResponse> getProductImages(Integer productId);

    ProductImageResponse updateImage(Integer productId, Long imageId, ProductImageUpdateRequest request);

    ProductImageResponse setPrimaryImage(Integer productId, Long imageId);

    void deleteImage(Integer productId, Long imageId);
 
    ProductImageResponse approveImage(Integer productId, Long imageId);
}
