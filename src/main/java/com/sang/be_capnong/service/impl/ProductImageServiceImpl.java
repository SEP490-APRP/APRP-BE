package com.sang.be_capnong.service.impl;

import com.sang.be_capnong.dto.request.ProductImageCreationRequest;
import com.sang.be_capnong.dto.request.ProductImageUpdateRequest;
import com.sang.be_capnong.dto.response.ProductImageResponse;
import com.sang.be_capnong.entity.Product;
import com.sang.be_capnong.entity.ProductImage;
import com.sang.be_capnong.exception.AppException;
import com.sang.be_capnong.exception.ErrorCode;
import com.sang.be_capnong.mapper.ProductImageMapper;
import com.sang.be_capnong.repository.ProductImageRepository;
import com.sang.be_capnong.repository.ProductRepository;
import com.sang.be_capnong.service.ProductImageService;
import com.sang.be_capnong.util.SecurityUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ProductImageServiceImpl implements ProductImageService {

    ProductRepository productRepository;
    ProductImageRepository productImageRepository;
    ProductImageMapper productImageMapper;

    @Override
    public ProductImageResponse addImage(Integer productId, ProductImageCreationRequest request) {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        log.info("Adding image to product id: {} by user id: {}", productId, currentUserId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        if (product.getShop() == null || !Objects.equals(product.getShop().getId(), currentUserId)) {
            throw new AppException(ErrorCode.NOT_PRODUCT_OWNER);
        }

        long existingCount = productImageRepository.countByProduct_ProductId(productId);

        boolean isPrimary = false;
        if (Boolean.TRUE.equals(request.getIsPrimary())) {
            isPrimary = true;
            unsetExistingPrimaryImages(productId, null);
        } else if (request.getIsPrimary() == null && existingCount == 0) {
            isPrimary = true;
        }

        int displayOrder = request.getDisplayOrder() != null ? request.getDisplayOrder() : (int) existingCount;

        ProductImage productImage = productImageMapper.toProductImage(request);
        productImage.setProduct(product);
        productImage.setIsPrimary(isPrimary);
        productImage.setDisplayOrder(displayOrder);
        productImage.setCreatedAt(LocalDateTime.now());

        ProductImage savedImage = productImageRepository.save(productImage);
        return productImageMapper.toProductImageResponse(savedImage);
    }

    @Override
    public List<ProductImageResponse> getProductImages(Integer productId) {
        log.info("Fetching all gallery images for product id: {}", productId);

        if (!productRepository.existsById(productId)) {
            throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        List<ProductImage> images = productImageRepository.findByProduct_ProductIdOrderByDisplayOrderAscIdAsc(productId);
        return productImageMapper.toProductImageResponseList(images);
    }

    @Override
    public ProductImageResponse updateImage(Integer productId, Long imageId, ProductImageUpdateRequest request) {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        log.info("Updating image id: {} for product id: {} by user id: {}", imageId, productId, currentUserId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        if (product.getShop() == null || !Objects.equals(product.getShop().getId(), currentUserId)) {
            throw new AppException(ErrorCode.NOT_PRODUCT_OWNER);
        }

        ProductImage image = productImageRepository.findById(imageId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_IMAGE_NOT_FOUND));

        if (!Objects.equals(image.getProduct().getProductId(), productId)) {
            throw new AppException(ErrorCode.IMAGE_NOT_BELONG_TO_PRODUCT);
        }

        if (Boolean.TRUE.equals(request.getIsPrimary())) {
            unsetExistingPrimaryImages(productId, imageId);
            image.setIsPrimary(true);
        } else if (Boolean.FALSE.equals(request.getIsPrimary())) {
            image.setIsPrimary(false);
        }

        if (request.getImageUrl() != null && !request.getImageUrl().isBlank()) {
            image.setImageUrl(request.getImageUrl().trim());
        }

        if (request.getDisplayOrder() != null) {
            image.setDisplayOrder(request.getDisplayOrder());
        }

        ProductImage updatedImage = productImageRepository.save(image);
        return productImageMapper.toProductImageResponse(updatedImage);
    }

    @Override
    public ProductImageResponse setPrimaryImage(Integer productId, Long imageId) {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        log.info("Setting image id: {} as primary for product id: {} by user id: {}", imageId, productId, currentUserId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        if (product.getShop() == null || !Objects.equals(product.getShop().getId(), currentUserId)) {
            throw new AppException(ErrorCode.NOT_PRODUCT_OWNER);
        }

        ProductImage image = productImageRepository.findById(imageId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_IMAGE_NOT_FOUND));

        if (!Objects.equals(image.getProduct().getProductId(), productId)) {
            throw new AppException(ErrorCode.IMAGE_NOT_BELONG_TO_PRODUCT);
        }

        unsetExistingPrimaryImages(productId, imageId);
        image.setIsPrimary(true);

        ProductImage savedImage = productImageRepository.save(image);
        return productImageMapper.toProductImageResponse(savedImage);
    }

    @Override
    public void deleteImage(Integer productId, Long imageId) {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        log.info("Deleting image id: {} for product id: {} by user id: {}", imageId, productId, currentUserId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        if (product.getShop() == null || !Objects.equals(product.getShop().getId(), currentUserId)) {
            throw new AppException(ErrorCode.NOT_PRODUCT_OWNER);
        }

        ProductImage image = productImageRepository.findById(imageId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_IMAGE_NOT_FOUND));

        if (!Objects.equals(image.getProduct().getProductId(), productId)) {
            throw new AppException(ErrorCode.IMAGE_NOT_BELONG_TO_PRODUCT);
        }

        boolean wasPrimary = Boolean.TRUE.equals(image.getIsPrimary());
        productImageRepository.delete(image);

        if (wasPrimary) {
            List<ProductImage> remaining = productImageRepository.findByProduct_ProductIdOrderByDisplayOrderAscIdAsc(productId);
            if (!remaining.isEmpty()) {
                ProductImage nextPrimary = remaining.get(0);
                nextPrimary.setIsPrimary(true);
                productImageRepository.save(nextPrimary);
            }
        }
    }

    private void unsetExistingPrimaryImages(int productId, Long exceptImageId) {
        List<ProductImage> primaries = productImageRepository.findByProduct_ProductIdAndIsPrimaryTrue(productId);
        for (ProductImage primary : primaries) {
            if (exceptImageId == null || !primary.getId().equals(exceptImageId)) {
                primary.setIsPrimary(false);
                productImageRepository.save(primary);
            }
        }
    }
}
