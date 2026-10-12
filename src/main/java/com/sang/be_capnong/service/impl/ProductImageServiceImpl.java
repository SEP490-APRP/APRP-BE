package com.sang.be_capnong.service.impl;

import com.sang.be_capnong.dto.ModerationCheckResult;
import com.sang.be_capnong.dto.request.ProductImageCreationRequest;
import com.sang.be_capnong.dto.request.ProductImageUpdateRequest;
import com.sang.be_capnong.dto.response.ProductImageResponse;
import com.sang.be_capnong.entity.ModerationLabel;
import com.sang.be_capnong.entity.ModerationResult;
import com.sang.be_capnong.entity.Product;
import com.sang.be_capnong.entity.ProductImage;
import com.sang.be_capnong.exception.AppException;
import com.sang.be_capnong.exception.ErrorCode;
import com.sang.be_capnong.mapper.ProductImageMapper;
import com.sang.be_capnong.repository.ModerationResultRepository;
import com.sang.be_capnong.repository.ProductImageRepository;
import com.sang.be_capnong.repository.ProductRepository;
import com.sang.be_capnong.service.CloudinaryService;
import com.sang.be_capnong.service.ImageModerationService;
import com.sang.be_capnong.service.ProductImageService;
import com.sang.be_capnong.util.SecurityUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Transactional
public class ProductImageServiceImpl implements ProductImageService {

    ProductRepository productRepository;
    ProductImageRepository productImageRepository;
    ProductImageMapper productImageMapper;
    ImageModerationService imageModerationService;
    ModerationResultRepository moderationResultRepository;
    CloudinaryService cloudinaryService;

    @Override
    public ProductImageResponse addImage(Integer productId, ProductImageCreationRequest request) {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        log.info("Adding image to product id: {} by user id: {}", productId, currentUserId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        if (product.getShop().getId() != currentUserId) {
            throw new AppException(ErrorCode.NOT_PRODUCT_OWNER);
        }

        // Quy định tối đa 8 hình ảnh cho mỗi sản phẩm
        long existingCount = productImageRepository.countByProduct_ProductId(productId);
        if (existingCount >= 8) {
            throw new AppException(ErrorCode.MAX_PRODUCT_IMAGES_EXCEEDED);
        }

        // 1. Kiểm duyệt ảnh bằng AI (Google Cloud Vision API) với phân loại 2 mức
        Integer categoryId = product.getCategory() != null ? product.getCategory().getCategoryId() : null;
        ModerationCheckResult checkResult = imageModerationService.checkImageUrl(request.getImageUrl(), product.getProductName(), categoryId);
        if (!checkResult.isAgricultural()) {
            log.warn("Image rejected (Level 1) for product id {}: {}", productId, checkResult.getRejectionReason());
            // Tự động dọn dẹp file ảnh vừa upload lên Cloudinary khi bị AI từ chối để tiết kiệm dung lượng
            cloudinaryService.deleteImageByUrl(request.getImageUrl());
            throw new AppException(ErrorCode.IMAGE_NOT_AGRICULTURAL_PRODUCE);
        }

        boolean isPrimary = false;
        if (Boolean.TRUE.equals(request.getIsPrimary())) {
            isPrimary = true;
            unsetExistingPrimaryImages(productId, null);
        } else if (request.getIsPrimary() == null && existingCount == 0) {
            isPrimary = true;
        }

        int displayOrder = request.getDisplayOrder() != null ? request.getDisplayOrder() : (int) existingCount;

        boolean isFlagged = checkResult.isFlagged();
        boolean isActiveProduct = "ACTIVE".equalsIgnoreCase(product.getStatus());

        ProductImage productImage = productImageMapper.toProductImage(request);
        productImage.setProduct(product);
        productImage.setIsPrimary(isPrimary);
        productImage.setDisplayOrder(displayOrder);
        productImage.setIsFlagged(isFlagged);
        productImage.setFlagReason(isFlagged ? checkResult.getFlagReason() : null);
        // Nếu ảnh bị gắn cờ -> Tạm ẩn (isApproved = false) chờ Admin duyệt; nếu sạch -> isApproved = true
        productImage.setIsApproved(!isFlagged);
        productImage.setCreatedAt(LocalDateTime.now());

        ProductImage savedImage = productImageRepository.save(productImage);

        // TH2: Nếu đổi hoặc thêm ảnh đại diện trên sản phẩm ACTIVE -> bắt buộc duyệt lại -> tự động chuyển PENDING
        if (isPrimary && isActiveProduct) {
            product.setStatus("PENDING");
            product.setRejectReason(null);
            productRepository.save(product);
            log.info("Product id: {} transitioned from ACTIVE to PENDING after new primary image added", productId);
        }

        // 2. Lưu / cập nhật kết quả kiểm duyệt AI vào CSDL để Admin theo dõi
        if (isFlagged) {
            saveOrUpdateModerationResult(product, checkResult);
        }

        return productImageMapper.toProductImageResponse(savedImage);
    }

    @Override
    public List<ProductImageResponse> getProductImages(Integer productId) {
        log.info("Fetching gallery images for product id: {}", productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        List<ProductImage> images = productImageRepository.findByProduct_ProductIdOrderByDisplayOrderAscIdAsc(productId);

        Integer currentUserId = SecurityUtils.getCurrentUserIdOrNull();
        boolean isAdmin = SecurityUtils.isAdmin();
        boolean isOwner = currentUserId != null && product.getShop() != null && product.getShop().getId() == currentUserId;

        // Nếu người xem là khách mua hàng / guest, chỉ trả về các ảnh đã được duyệt (isApproved != false)
        if (!isAdmin && !isOwner) {
            images = images.stream()
                    .filter(img -> !Boolean.FALSE.equals(img.getIsApproved()))
                    .toList();
        }

        return productImageMapper.toProductImageResponseList(images);
    }

    @Override
    public ProductImageResponse updateImage(Integer productId, Long imageId, ProductImageUpdateRequest request) {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        log.info("Updating image id: {} for product id: {} by user id: {}", imageId, productId, currentUserId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        if (product.getShop().getId() != currentUserId) {
            throw new AppException(ErrorCode.NOT_PRODUCT_OWNER);
        }

        ProductImage image = productImageRepository.findById(imageId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_IMAGE_NOT_FOUND));

        if (image.getProduct().getProductId() != productId) {
            throw new AppException(ErrorCode.IMAGE_NOT_BELONG_TO_PRODUCT);
        }

        boolean oldPrimary = Boolean.TRUE.equals(image.getIsPrimary());
        boolean newPrimary = oldPrimary;
        if (Boolean.TRUE.equals(request.getIsPrimary())) {
            unsetExistingPrimaryImages(productId, imageId);
            image.setIsPrimary(true);
            newPrimary = true;
        } else if (Boolean.FALSE.equals(request.getIsPrimary())) {
            image.setIsPrimary(false);
            newPrimary = false;
        }

        boolean imageContentChanged = false;
        if (request.getImageUrl() != null && !request.getImageUrl().isBlank()) {
            String newUrl = request.getImageUrl().trim();
            if (!newUrl.equals(image.getImageUrl())) {
                imageContentChanged = true;
                Integer categoryId = product.getCategory() != null ? product.getCategory().getCategoryId() : null;
                ModerationCheckResult checkResult = imageModerationService.checkImageUrl(newUrl, product.getProductName(), categoryId);
                if (!checkResult.isAgricultural()) {
                    log.warn("Updated image rejected (Level 1) for product id {}: {}", productId, checkResult.getRejectionReason());
                    // Dọn dẹp ảnh mới vừa upload lên Cloudinary nhưng bị AI từ chối
                    cloudinaryService.deleteImageByUrl(newUrl);
                    throw new AppException(ErrorCode.IMAGE_NOT_AGRICULTURAL_PRODUCE);
                }
                // Nếu ảnh mới hợp lệ, dọn dẹp ảnh cũ trên Cloudinary để giải phóng bộ nhớ
                String oldUrl = image.getImageUrl();
                cloudinaryService.deleteImageByUrl(oldUrl);

                boolean isFlagged = checkResult.isFlagged();
                boolean isActiveProduct = "ACTIVE".equalsIgnoreCase(product.getStatus());
                image.setIsFlagged(isFlagged);
                image.setFlagReason(isFlagged ? checkResult.getFlagReason() : null);
                image.setIsApproved(!isFlagged);

                if (isFlagged) {
                    saveOrUpdateModerationResult(product, checkResult);
                }
            }
            image.setImageUrl(newUrl);
        }

        if (request.getDisplayOrder() != null) {
            image.setDisplayOrder(request.getDisplayOrder());
        }

        ProductImage updatedImage = productImageRepository.save(image);

        // TH2: Nếu sản phẩm đang ACTIVE và ảnh đại diện bị đổi nội dung hoặc một ảnh khác được chỉ định làm đại diện
        if ("ACTIVE".equalsIgnoreCase(product.getStatus()) && ((oldPrimary && imageContentChanged) || (!oldPrimary && newPrimary))) {
            product.setStatus("PENDING");
            product.setRejectReason(null);
            productRepository.save(product);
            log.info("Product id: {} transitioned to PENDING because primary image was updated", productId);
        }

        return productImageMapper.toProductImageResponse(updatedImage);
    }

    @Override
    public ProductImageResponse setPrimaryImage(Integer productId, Long imageId) {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        log.info("Setting image id: {} as primary for product id: {} by user id: {}", imageId, productId, currentUserId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        if (product.getShop().getId() != currentUserId) {
            throw new AppException(ErrorCode.NOT_PRODUCT_OWNER);
        }

        ProductImage image = productImageRepository.findById(imageId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_IMAGE_NOT_FOUND));

        if (image.getProduct().getProductId() != productId) {
            throw new AppException(ErrorCode.IMAGE_NOT_BELONG_TO_PRODUCT);
        }

        unsetExistingPrimaryImages(productId, imageId);
        image.setIsPrimary(true);

        ProductImage savedImage = productImageRepository.save(image);

        // TH2: Nếu đổi ảnh đại diện trên sản phẩm ACTIVE -> bắt buộc duyệt lại -> tự động chuyển PENDING
        if ("ACTIVE".equalsIgnoreCase(product.getStatus())) {
            product.setStatus("PENDING");
            product.setRejectReason(null);
            productRepository.save(product);
            log.info("Product id: {} transitioned from ACTIVE to PENDING after primary image changed", productId);
        }

        return productImageMapper.toProductImageResponse(savedImage);
    }

    @Override
    public void deleteImage(Integer productId, Long imageId) {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        log.info("Deleting image id: {} for product id: {} by user id: {}", imageId, productId, currentUserId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        if (product.getShop().getId() != currentUserId) {
            throw new AppException(ErrorCode.NOT_PRODUCT_OWNER);
        }

        ProductImage image = productImageRepository.findById(imageId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_IMAGE_NOT_FOUND));

        if (image.getProduct().getProductId() != productId) {
            throw new AppException(ErrorCode.IMAGE_NOT_BELONG_TO_PRODUCT);
        }

        String imageUrlToDelete = image.getImageUrl();
        boolean wasPrimary = Boolean.TRUE.equals(image.getIsPrimary());
        productImageRepository.delete(image);

        // Tự động xóa file ảnh khỏi Cloudinary để dọn rác
        cloudinaryService.deleteImageByUrl(imageUrlToDelete);

        List<ProductImage> remaining = productImageRepository.findByProduct_ProductIdOrderByDisplayOrderAscIdAsc(productId);

        // Vấn đề 5: Xóa hết ảnh (count == 0) -> Cho phép nhưng tự động chuyển sản phẩm sang INACTIVE và ghi rõ lý do
        if (remaining.isEmpty()) {
            product.setStatus("INACTIVE");
            product.setRejectReason("Sản phẩm đã bị tạm ẩn tự động do không còn hình ảnh nào.");
            productRepository.save(product);
            log.warn("Product id: {} set to INACTIVE because all images were deleted", productId);

            moderationResultRepository.findByProduct_ProductId(productId).ifPresent(mr -> {
                mr.setIsFlagged(false);
                if (mr.getModerationLabels() != null) {
                    mr.getModerationLabels().clear();
                }
                moderationResultRepository.save(mr);
            });
            return;
        }

        if (wasPrimary) {
            ProductImage nextPrimary = remaining.get(0);
            nextPrimary.setIsPrimary(true);
            productImageRepository.save(nextPrimary);
        }

        // Vấn đề 4: Tự động gỡ cờ cảnh báo nếu tất cả các ảnh còn lại đều sạch
        boolean hasRemainingFlagged = remaining.stream().anyMatch(img -> Boolean.TRUE.equals(img.getIsFlagged()));
        if (!hasRemainingFlagged) {
            moderationResultRepository.findByProduct_ProductId(productId).ifPresent(mr -> {
                mr.setIsFlagged(false);
                if (mr.getModerationLabels() != null) {
                    mr.getModerationLabels().clear();
                }
                moderationResultRepository.save(mr);
                log.info("Unflagged product id: {} because all remaining images are clean", productId);
            });
        }
    }

    @Override
    public ProductImageResponse approveImage(Integer productId, Long imageId) {
        log.info("Admin approving image id: {} for product id: {}", imageId, productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        ProductImage image = productImageRepository.findById(imageId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_IMAGE_NOT_FOUND));

        if (image.getProduct().getProductId() != productId) {
            throw new AppException(ErrorCode.IMAGE_NOT_BELONG_TO_PRODUCT);
        }

        image.setIsApproved(true);
        image.setIsFlagged(false);
        image.setFlagReason(null);
        ProductImage savedImage = productImageRepository.save(image);

        // Kiểm tra xem tất cả các ảnh còn lại của sản phẩm đã sạch cờ chưa
        List<ProductImage> allImages = productImageRepository.findByProduct_ProductIdOrderByDisplayOrderAscIdAsc(productId);
        boolean hasRemainingFlagged = allImages.stream().anyMatch(img -> Boolean.TRUE.equals(img.getIsFlagged()));
        if (!hasRemainingFlagged) {
            moderationResultRepository.findByProduct_ProductId(productId).ifPresent(mr -> {
                mr.setIsFlagged(false);
                if (mr.getModerationLabels() != null) {
                    mr.getModerationLabels().clear();
                }
                moderationResultRepository.save(mr);
                log.info("Unflagged product id: {} because image id: {} was approved and no flags remain", productId, imageId);
            });
        }

        return productImageMapper.toProductImageResponse(savedImage);
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

    private void saveOrUpdateModerationResult(Product product, ModerationCheckResult checkResult) {
        try {
            ModerationResult moderationResult = moderationResultRepository.findByProduct_ProductId(product.getProductId())
                    .orElseGet(() -> ModerationResult.builder()
                            .product(product)
                            .moderationLabels(new ArrayList<>())
                            .build());

            moderationResult.setIsFlagged(checkResult.isFlagged());
            moderationResult.setOverallConfidence(checkResult.getOverallConfidence());
            moderationResult.setCheckedAt(LocalDateTime.now());
            moderationResult.setRawResponse(checkResult.getRawResponse());

            if (moderationResult.getModerationLabels() != null) {
                moderationResult.getModerationLabels().clear();
            } else {
                moderationResult.setModerationLabels(new ArrayList<>());
            }

            if (checkResult.getLabels() != null) {
                for (ModerationCheckResult.ModerationLabelItem item : checkResult.getLabels()) {
                    ModerationLabel label = ModerationLabel.builder()
                            .labelName(item.getLabelName())
                            .parentLabel(item.getParentLabel())
                            .confidence(item.getConfidence())
                            .moderationResult(moderationResult)
                            .build();
                    moderationResult.getModerationLabels().add(label);
                }
            }

            moderationResultRepository.save(moderationResult);
            log.info("Saved ModerationResult for product id: {}, overallConfidence: {}%",
                    product.getProductId(), checkResult.getOverallConfidence());
        } catch (Exception e) {
            log.error("Failed to save ModerationResult for product id {}: {}", product.getProductId(), e.getMessage(), e);
        }
    }
}

