package com.sang.be_capnong.service;

import com.sang.be_capnong.dto.request.ProductImageCreationRequest;
import com.sang.be_capnong.dto.request.ProductImageUpdateRequest;
import com.sang.be_capnong.dto.response.ProductImageResponse;
import com.sang.be_capnong.entity.Product;
import com.sang.be_capnong.entity.ProductImage;
import com.sang.be_capnong.entity.Role;
import com.sang.be_capnong.entity.User;
import com.sang.be_capnong.exception.AppException;
import com.sang.be_capnong.exception.ErrorCode;
import com.sang.be_capnong.mapper.ProductImageMapper;
import com.sang.be_capnong.repository.ProductImageRepository;
import com.sang.be_capnong.repository.ProductRepository;
import com.sang.be_capnong.service.impl.ProductImageServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.sang.be_capnong.dto.ModerationCheckResult;
import com.sang.be_capnong.repository.ModerationResultRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductImageServiceTest {

    @Mock
    ProductRepository productRepository;

    @Mock
    ProductImageRepository productImageRepository;

    @Mock
    ProductImageMapper productImageMapper;

    @Mock
    ImageModerationService imageModerationService;

    @Mock
    ModerationResultRepository moderationResultRepository;

    @Mock
    CloudinaryService cloudinaryService;

    @InjectMocks
    ProductImageServiceImpl productImageService;

    User shopUser;
    User otherShopUser;
    Product sampleProduct;
    Product otherProduct;
    ProductImage sampleImage;
    ProductImageResponse sampleResponse;
    ProductImageCreationRequest creationRequest;
    ProductImageUpdateRequest updateRequest;

    @BeforeEach
    void setUp() {
        lenient().when(imageModerationService.checkImageUrl(any(), any(), any()))
                .thenReturn(ModerationCheckResult.builder()
                        .isAgricultural(true)
                        .isFlagged(false)
                        .overallConfidence(BigDecimal.valueOf(95.0))
                        .labels(List.of())
                        .build());
        Role shopRole = Role.builder().id(2).name("SHOP").build();
        shopUser = User.builder().id(1).role(shopRole).build();
        otherShopUser = User.builder().id(2).role(shopRole).build();

        sampleProduct = Product.builder()
                .productId(10)
                .productName("Xoài cát Cần Thơ")
                .shop(shopUser)
                .build();

        otherProduct = Product.builder()
                .productId(20)
                .productName("Cam sành")
                .shop(otherShopUser)
                .build();

        sampleImage = ProductImage.builder()
                .id(100L)
                .imageUrl("https://cdn.capnong.vn/xoai-1.jpg")
                .isPrimary(true)
                .displayOrder(0)
                .createdAt(LocalDateTime.now())
                .product(sampleProduct)
                .build();

        sampleResponse = ProductImageResponse.builder()
                .id(100L)
                .imageUrl("https://cdn.capnong.vn/xoai-1.jpg")
                .isPrimary(true)
                .displayOrder(0)
                .createdAt(LocalDateTime.now())
                .build();

        creationRequest = ProductImageCreationRequest.builder()
                .imageUrl("https://cdn.capnong.vn/xoai-1.jpg")
                .isPrimary(true)
                .displayOrder(0)
                .build();

        updateRequest = ProductImageUpdateRequest.builder()
                .imageUrl("https://cdn.capnong.vn/xoai-updated.jpg")
                .isPrimary(false)
                .displayOrder(1)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void mockAuthenticatedUser(Integer userId) {
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn(String.valueOf(userId));
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void addImage_success() {
        mockAuthenticatedUser(1);
        when(productRepository.findById(10)).thenReturn(Optional.of(sampleProduct));
        when(productImageRepository.countByProduct_ProductId(10)).thenReturn(0L);
        when(productImageMapper.toProductImage(creationRequest)).thenReturn(sampleImage);
        when(productImageRepository.save(any(ProductImage.class))).thenReturn(sampleImage);
        when(productImageMapper.toProductImageResponse(sampleImage)).thenReturn(sampleResponse);

        ProductImageResponse result = productImageService.addImage(10, creationRequest);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(100L);
        verify(productImageRepository).save(any(ProductImage.class));
    }

    @Test
    void addImage_productNotFound_throwsAppException() {
        mockAuthenticatedUser(1);
        when(productRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productImageService.addImage(999, creationRequest))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);

        verify(productImageRepository, never()).save(any(ProductImage.class));
    }

    @Test
    void addImage_notOwner_throwsAppException() {
        mockAuthenticatedUser(2); // different farmer
        when(productRepository.findById(10)).thenReturn(Optional.of(sampleProduct));

        assertThatThrownBy(() -> productImageService.addImage(10, creationRequest))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_PRODUCT_OWNER);

        verify(productImageRepository, never()).save(any(ProductImage.class));
    }

    @Test
    void addImage_asPrimary_unsetsPreviousPrimary() {
        mockAuthenticatedUser(1);
        ProductImage previousPrimary = ProductImage.builder().id(99L).isPrimary(true).build();
        when(productRepository.findById(10)).thenReturn(Optional.of(sampleProduct));
        when(productImageRepository.countByProduct_ProductId(10)).thenReturn(1L);
        when(productImageRepository.findByProduct_ProductIdAndIsPrimaryTrue(10))
                .thenReturn(List.of(previousPrimary));
        when(productImageMapper.toProductImage(creationRequest)).thenReturn(sampleImage);
        when(productImageRepository.save(any(ProductImage.class))).thenReturn(sampleImage);
        when(productImageMapper.toProductImageResponse(sampleImage)).thenReturn(sampleResponse);

        ProductImageResponse result = productImageService.addImage(10, creationRequest);

        assertThat(result).isNotNull();
        assertThat(previousPrimary.getIsPrimary()).isFalse();
        verify(productImageRepository).save(previousPrimary);
    }

    @Test
    void addImage_firstImage_isPrimaryNull_autoPrimary() {
        mockAuthenticatedUser(1);
        ProductImageCreationRequest request = ProductImageCreationRequest.builder()
                .imageUrl("https://cdn.capnong.vn/xoai-2.jpg")
                .build();

        when(productRepository.findById(10)).thenReturn(Optional.of(sampleProduct));
        when(productImageRepository.countByProduct_ProductId(10)).thenReturn(0L);
        when(productImageMapper.toProductImage(request)).thenReturn(sampleImage);
        when(productImageRepository.save(any(ProductImage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productImageMapper.toProductImageResponse(sampleImage)).thenReturn(sampleResponse);

        productImageService.addImage(10, request);

        assertThat(sampleImage.getIsPrimary()).isTrue();
        assertThat(sampleImage.getDisplayOrder()).isEqualTo(0);
    }

    @Test
    void addImage_notFirstImage_isPrimaryNull_notAutoPrimary_WithDefaultOrder() {
        mockAuthenticatedUser(1);
        ProductImageCreationRequest request = ProductImageCreationRequest.builder()
                .imageUrl("https://cdn.capnong.vn/xoai-2.jpg")
                .build();

        when(productRepository.findById(10)).thenReturn(Optional.of(sampleProduct));
        when(productImageRepository.countByProduct_ProductId(10)).thenReturn(2L);
        when(productImageMapper.toProductImage(request)).thenReturn(sampleImage);
        when(productImageRepository.save(any(ProductImage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productImageMapper.toProductImageResponse(sampleImage)).thenReturn(sampleResponse);

        productImageService.addImage(10, request);

        assertThat(sampleImage.getIsPrimary()).isFalse();
        assertThat(sampleImage.getDisplayOrder()).isEqualTo(2);
    }

    @Test
    void addImage_explicitDisplayOrder_kept() {
        mockAuthenticatedUser(1);
        ProductImageCreationRequest request = ProductImageCreationRequest.builder()
                .imageUrl("https://cdn.capnong.vn/xoai-2.jpg")
                .isPrimary(false)
                .displayOrder(5)
                .build();

        when(productRepository.findById(10)).thenReturn(Optional.of(sampleProduct));
        when(productImageRepository.countByProduct_ProductId(10)).thenReturn(2L);
        when(productImageMapper.toProductImage(request)).thenReturn(sampleImage);
        when(productImageRepository.save(any(ProductImage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productImageMapper.toProductImageResponse(sampleImage)).thenReturn(sampleResponse);

        productImageService.addImage(10, request);

        assertThat(sampleImage.getIsPrimary()).isFalse();
        assertThat(sampleImage.getDisplayOrder()).isEqualTo(5);
    }

    @Test
    void getProductImages_success() {
        when(productRepository.findById(10)).thenReturn(Optional.of(sampleProduct));
        when(productImageRepository.findByProduct_ProductIdOrderByDisplayOrderAscIdAsc(10))
                .thenReturn(List.of(sampleImage));
        when(productImageMapper.toProductImageResponseList(anyList()))
                .thenReturn(List.of(sampleResponse));

        List<ProductImageResponse> result = productImageService.getProductImages(10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(100L);
    }

    @Test
    void getProductImages_productNotFound_throwsAppException() {
        when(productRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productImageService.getProductImages(999))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);
    }

    @Test
    void updateImage_success() {
        mockAuthenticatedUser(1);
        when(productRepository.findById(10)).thenReturn(Optional.of(sampleProduct));
        when(productImageRepository.findById(100L)).thenReturn(Optional.of(sampleImage));
        when(productImageRepository.save(any(ProductImage.class))).thenReturn(sampleImage);
        when(productImageMapper.toProductImageResponse(sampleImage)).thenReturn(sampleResponse);

        ProductImageResponse result = productImageService.updateImage(10, 100L, updateRequest);

        assertThat(result).isNotNull();
        verify(productImageRepository).save(sampleImage);
    }

    @Test
    void updateImage_productNotFound_throwsAppException() {
        mockAuthenticatedUser(1);
        when(productRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productImageService.updateImage(999, 100L, updateRequest))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);

        verify(productImageRepository, never()).save(any(ProductImage.class));
    }

    @Test
    void updateImage_notOwner_throwsAppException() {
        mockAuthenticatedUser(2); // different farmer
        when(productRepository.findById(10)).thenReturn(Optional.of(sampleProduct));

        assertThatThrownBy(() -> productImageService.updateImage(10, 100L, updateRequest))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_PRODUCT_OWNER);

        verify(productImageRepository, never()).save(any(ProductImage.class));
    }

    @Test
    void updateImage_imageNotFound_throwsAppException() {
        mockAuthenticatedUser(1);
        when(productRepository.findById(10)).thenReturn(Optional.of(sampleProduct));
        when(productImageRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productImageService.updateImage(10, 999L, updateRequest))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_IMAGE_NOT_FOUND);

        verify(productImageRepository, never()).save(any(ProductImage.class));
    }

    @Test
    void updateImage_setPrimary_unsetsOtherPrimaries() {
        mockAuthenticatedUser(1);
        ProductImage oldPrimary = ProductImage.builder().id(99L).isPrimary(true).build();
        when(productRepository.findById(10)).thenReturn(Optional.of(sampleProduct));
        when(productImageRepository.findById(100L)).thenReturn(Optional.of(sampleImage));
        when(productImageRepository.findByProduct_ProductIdAndIsPrimaryTrue(10))
                .thenReturn(List.of(oldPrimary));
        when(productImageRepository.save(any(ProductImage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productImageMapper.toProductImageResponse(sampleImage)).thenReturn(sampleResponse);

        updateRequest.setIsPrimary(true);

        productImageService.updateImage(10, 100L, updateRequest);

        assertThat(oldPrimary.getIsPrimary()).isFalse();
        assertThat(sampleImage.getIsPrimary()).isTrue();
        verify(productImageRepository).save(oldPrimary);
        verify(productImageRepository).save(sampleImage);
    }

    @Test
    void updateImage_blankUrl_keepsOriginal() {
        mockAuthenticatedUser(1);
        when(productRepository.findById(10)).thenReturn(Optional.of(sampleProduct));
        when(productImageRepository.findById(100L)).thenReturn(Optional.of(sampleImage));
        when(productImageRepository.save(any(ProductImage.class))).thenReturn(sampleImage);
        when(productImageMapper.toProductImageResponse(sampleImage)).thenReturn(sampleResponse);

        updateRequest.setImageUrl("   ");
        updateRequest.setIsPrimary(false);

        productImageService.updateImage(10, 100L, updateRequest);

        assertThat(sampleImage.getImageUrl()).isEqualTo("https://cdn.capnong.vn/xoai-1.jpg");
        verify(productImageRepository).save(sampleImage);
    }

    @Test
    void updateImage_nonBlankUrl_trims() {
        mockAuthenticatedUser(1);
        when(productRepository.findById(10)).thenReturn(Optional.of(sampleProduct));
        when(productImageRepository.findById(100L)).thenReturn(Optional.of(sampleImage));
        when(productImageRepository.save(any(ProductImage.class))).thenReturn(sampleImage);
        when(productImageMapper.toProductImageResponse(sampleImage)).thenReturn(sampleResponse);

        updateRequest.setImageUrl("  https://cdn.capnong.vn/xoai-new.jpg  ");
        updateRequest.setIsPrimary(false);

        productImageService.updateImage(10, 100L, updateRequest);

        assertThat(sampleImage.getImageUrl()).isEqualTo("https://cdn.capnong.vn/xoai-new.jpg");
        verify(productImageRepository).save(sampleImage);
    }

    @Test
    void updateImage_notBelongToProduct_throwsAppException() {
        mockAuthenticatedUser(1);
        sampleImage.setProduct(otherProduct); // belongs to product 20
        when(productRepository.findById(10)).thenReturn(Optional.of(sampleProduct));
        when(productImageRepository.findById(100L)).thenReturn(Optional.of(sampleImage));

        assertThatThrownBy(() -> productImageService.updateImage(10, 100L, updateRequest))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.IMAGE_NOT_BELONG_TO_PRODUCT);
    }

    @Test
    void setPrimaryImage_success() {
        mockAuthenticatedUser(1);
        ProductImage oldPrimary = ProductImage.builder().id(99L).isPrimary(true).build();
        when(productRepository.findById(10)).thenReturn(Optional.of(sampleProduct));
        when(productImageRepository.findById(100L)).thenReturn(Optional.of(sampleImage));
        when(productImageRepository.findByProduct_ProductIdAndIsPrimaryTrue(10))
                .thenReturn(List.of(oldPrimary));
        when(productImageRepository.save(any(ProductImage.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productImageMapper.toProductImageResponse(sampleImage)).thenReturn(sampleResponse);

        ProductImageResponse result = productImageService.setPrimaryImage(10, 100L);

        assertThat(result).isNotNull();
        assertThat(oldPrimary.getIsPrimary()).isFalse();
        assertThat(sampleImage.getIsPrimary()).isTrue();
    }

    @Test
    void setPrimaryImage_productNotFound_throwsAppException() {
        mockAuthenticatedUser(1);
        when(productRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productImageService.setPrimaryImage(999, 100L))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);

        verify(productImageRepository, never()).save(any(ProductImage.class));
    }

    @Test
    void setPrimaryImage_notOwner_throwsAppException() {
        mockAuthenticatedUser(2); // different farmer
        when(productRepository.findById(10)).thenReturn(Optional.of(sampleProduct));

        assertThatThrownBy(() -> productImageService.setPrimaryImage(10, 100L))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_PRODUCT_OWNER);

        verify(productImageRepository, never()).save(any(ProductImage.class));
    }

    @Test
    void setPrimaryImage_imageNotFound_throwsAppException() {
        mockAuthenticatedUser(1);
        when(productRepository.findById(10)).thenReturn(Optional.of(sampleProduct));
        when(productImageRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productImageService.setPrimaryImage(10, 999L))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_IMAGE_NOT_FOUND);

        verify(productImageRepository, never()).save(any(ProductImage.class));
    }

    @Test
    void setPrimaryImage_notBelongToProduct_throwsAppException() {
        mockAuthenticatedUser(1);
        sampleImage.setProduct(otherProduct); // belongs to product 20
        when(productRepository.findById(10)).thenReturn(Optional.of(sampleProduct));
        when(productImageRepository.findById(100L)).thenReturn(Optional.of(sampleImage));

        assertThatThrownBy(() -> productImageService.setPrimaryImage(10, 100L))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.IMAGE_NOT_BELONG_TO_PRODUCT);

        verify(productImageRepository, never()).save(any(ProductImage.class));
    }

    @Test
    void deleteImage_success() {
        mockAuthenticatedUser(1);
        sampleImage.setIsPrimary(false);
        when(productRepository.findById(10)).thenReturn(Optional.of(sampleProduct));
        when(productImageRepository.findById(100L)).thenReturn(Optional.of(sampleImage));

        productImageService.deleteImage(10, 100L);

        verify(productImageRepository).delete(sampleImage);
    }

    @Test
    void deleteImage_whenPrimary_promotesNextImage() {
        mockAuthenticatedUser(1);
        sampleImage.setIsPrimary(true);
        ProductImage remainingImage = ProductImage.builder().id(101L).isPrimary(false).build();

        when(productRepository.findById(10)).thenReturn(Optional.of(sampleProduct));
        when(productImageRepository.findById(100L)).thenReturn(Optional.of(sampleImage));
        when(productImageRepository.findByProduct_ProductIdOrderByDisplayOrderAscIdAsc(10))
                .thenReturn(new ArrayList<>(List.of(remainingImage)));

        productImageService.deleteImage(10, 100L);

        verify(productImageRepository).delete(sampleImage);
        assertThat(remainingImage.getIsPrimary()).isTrue();
        verify(productImageRepository).save(remainingImage);
    }

    @Test
    void deleteImage_productNotFound_throwsAppException() {
        mockAuthenticatedUser(1);
        when(productRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productImageService.deleteImage(999, 100L))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_NOT_FOUND);

        verify(productImageRepository, never()).delete(any(ProductImage.class));
    }

    @Test
    void deleteImage_notOwner_throwsAppException() {
        mockAuthenticatedUser(2); // different farmer
        when(productRepository.findById(10)).thenReturn(Optional.of(sampleProduct));

        assertThatThrownBy(() -> productImageService.deleteImage(10, 100L))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_PRODUCT_OWNER);

        verify(productImageRepository, never()).delete(any(ProductImage.class));
    }

    @Test
    void deleteImage_imageNotFound_throwsAppException() {
        mockAuthenticatedUser(1);
        when(productRepository.findById(10)).thenReturn(Optional.of(sampleProduct));
        when(productImageRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productImageService.deleteImage(10, 999L))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PRODUCT_IMAGE_NOT_FOUND);

        verify(productImageRepository, never()).delete(any(ProductImage.class));
    }

    @Test
    void deleteImage_notBelongToProduct_throwsAppException() {
        mockAuthenticatedUser(1);
        sampleImage.setProduct(otherProduct); // belongs to product 20
        when(productRepository.findById(10)).thenReturn(Optional.of(sampleProduct));
        when(productImageRepository.findById(100L)).thenReturn(Optional.of(sampleImage));

        assertThatThrownBy(() -> productImageService.deleteImage(10, 100L))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.IMAGE_NOT_BELONG_TO_PRODUCT);

        verify(productImageRepository, never()).delete(any(ProductImage.class));
    }

    @Test
    void deleteImage_primaryNoRemaining_doesNotPromote() {
        mockAuthenticatedUser(1);
        sampleImage.setIsPrimary(true);
        when(productRepository.findById(10)).thenReturn(Optional.of(sampleProduct));
        when(productImageRepository.findById(100L)).thenReturn(Optional.of(sampleImage));
        when(productImageRepository.findByProduct_ProductIdOrderByDisplayOrderAscIdAsc(10))
                .thenReturn(new ArrayList<>());

        productImageService.deleteImage(10, 100L);

        verify(productImageRepository).delete(sampleImage);
        verify(productImageRepository, never()).save(any(ProductImage.class));
        verify(cloudinaryService).deleteImageByUrl(sampleImage.getImageUrl());
    }

    @Test
    void addImage_rejectedByModeration_cleansUpCloudinaryImage() {
        mockAuthenticatedUser(1);
        when(productRepository.findById(10)).thenReturn(Optional.of(sampleProduct));
        when(imageModerationService.checkImageUrl(eq("https://cdn.capnong.vn/car.jpg"), any(), any()))
                .thenReturn(ModerationCheckResult.builder()
                        .isAgricultural(false)
                        .isFlagged(true)
                        .rejectionReason("Không phải nông sản")
                        .build());

        ProductImageCreationRequest req = ProductImageCreationRequest.builder()
                .imageUrl("https://cdn.capnong.vn/car.jpg")
                .build();

        assertThatThrownBy(() -> productImageService.addImage(10, req))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.IMAGE_NOT_AGRICULTURAL_PRODUCE);

        verify(cloudinaryService).deleteImageByUrl("https://cdn.capnong.vn/car.jpg");
        verify(productImageRepository, never()).save(any());
    }

    @Test
    void updateImage_successWithNewUrl_cleansUpOldCloudinaryImage() {
        mockAuthenticatedUser(1);
        when(productRepository.findById(10)).thenReturn(Optional.of(sampleProduct));
        when(productImageRepository.findById(100L)).thenReturn(Optional.of(sampleImage));
        when(productImageRepository.save(any(ProductImage.class))).thenReturn(sampleImage);
        when(productImageMapper.toProductImageResponse(sampleImage)).thenReturn(sampleResponse);

        ProductImageUpdateRequest req = ProductImageUpdateRequest.builder()
                .imageUrl("https://cdn.capnong.vn/new-tomato.jpg")
                .build();

        productImageService.updateImage(10, 100L, req);

        // Ảnh cũ sampleImage có URL "https://cdn.capnong.vn/xoai-1.jpg" phải được dọn dẹp
        verify(cloudinaryService).deleteImageByUrl("https://cdn.capnong.vn/xoai-1.jpg");
    }

    @Test
    void addImage_level2Flagged_savesImageAndModerationResultWithFlag() {
        mockAuthenticatedUser(1);
        when(productRepository.findById(10)).thenReturn(Optional.of(sampleProduct));
        when(imageModerationService.checkImageUrl(eq("https://cdn.capnong.vn/watermelon-ugly.jpg"), any(), any()))
                .thenReturn(ModerationCheckResult.builder()
                        .isAgricultural(true)
                        .isFlagged(true)
                        .overallConfidence(BigDecimal.valueOf(55.0))
                        .rejectionReason("Nông sản có hình thức xấu mã hoặc ảnh chụp góc khuất")
                        .labels(List.of())
                        .build());

        when(productImageRepository.countByProduct_ProductId(10)).thenReturn(1L);
        when(productImageMapper.toProductImage(any())).thenReturn(sampleImage);
        when(productImageRepository.save(any(ProductImage.class))).thenReturn(sampleImage);
        when(productImageMapper.toProductImageResponse(sampleImage)).thenReturn(sampleResponse);
        when(moderationResultRepository.findByProduct_ProductId(10)).thenReturn(Optional.empty());

        ProductImageCreationRequest req = ProductImageCreationRequest.builder()
                .imageUrl("https://cdn.capnong.vn/watermelon-ugly.jpg")
                .build();

        ProductImageResponse res = productImageService.addImage(10, req);

        assertThat(res).isNotNull();
        // Mức 2: Ảnh KHÔNG bị xóa trên Cloudinary
        verify(cloudinaryService, never()).deleteImageByUrl(any());
        // Mức 2: Ảnh ĐƯỢC lưu vào database
        verify(productImageRepository).save(any(ProductImage.class));
        // Mức 2: Kết quả kiểm duyệt gắn cờ ĐƯỢC lưu vào CSDL
        verify(moderationResultRepository).save(any());
    }

    @Test
    void addImage_exceedsMaxImagesLimit_throwsAppException() {
        mockAuthenticatedUser(1);
        when(productRepository.findById(10)).thenReturn(Optional.of(sampleProduct));
        when(productImageRepository.countByProduct_ProductId(10)).thenReturn(8L);

        ProductImageCreationRequest req = ProductImageCreationRequest.builder()
                .imageUrl("https://cdn.capnong.vn/9th-image.jpg")
                .build();

        assertThatThrownBy(() -> productImageService.addImage(10, req))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MAX_PRODUCT_IMAGES_EXCEEDED);

        verify(productImageRepository, never()).save(any());
    }

    @Test
    void setPrimaryImage_activeProduct_transitionsProductToPending() {
        mockAuthenticatedUser(1);
        sampleProduct.setStatus("ACTIVE");
        when(productRepository.findById(10)).thenReturn(Optional.of(sampleProduct));
        when(productImageRepository.findById(100L)).thenReturn(Optional.of(sampleImage));
        when(productImageRepository.findByProduct_ProductIdAndIsPrimaryTrue(10)).thenReturn(List.of());
        when(productImageRepository.save(any(ProductImage.class))).thenReturn(sampleImage);
        when(productImageMapper.toProductImageResponse(sampleImage)).thenReturn(sampleResponse);

        productImageService.setPrimaryImage(10, 100L);

        assertThat(sampleProduct.getStatus()).isEqualTo("PENDING");
        assertThat(sampleProduct.getRejectReason()).isNull();
        verify(productRepository).save(sampleProduct);
    }

    @Test
    void deleteImage_removesAllImages_transitionsProductToInactive() {
        mockAuthenticatedUser(1);
        sampleProduct.setStatus("ACTIVE");
        when(productRepository.findById(10)).thenReturn(Optional.of(sampleProduct));
        when(productImageRepository.findById(100L)).thenReturn(Optional.of(sampleImage));
        when(productImageRepository.findByProduct_ProductIdOrderByDisplayOrderAscIdAsc(10))
                .thenReturn(List.of());

        productImageService.deleteImage(10, 100L);

        verify(productImageRepository).delete(sampleImage);
        verify(cloudinaryService).deleteImageByUrl(sampleImage.getImageUrl());
        assertThat(sampleProduct.getStatus()).isEqualTo("INACTIVE");
        assertThat(sampleProduct.getRejectReason()).contains("không còn hình ảnh nào");
        verify(productRepository).save(sampleProduct);
    }

    @Test
    void deleteImage_removesFlaggedImage_autoUnflagsProductWhenRemainingClean() {
        mockAuthenticatedUser(1);
        sampleImage.setIsFlagged(true);
        ProductImage cleanImage = ProductImage.builder()
                .id(101L)
                .imageUrl("https://cdn.capnong.vn/clean.jpg")
                .isFlagged(false)
                .isApproved(true)
                .product(sampleProduct)
                .build();

        com.sang.be_capnong.entity.ModerationResult mr = com.sang.be_capnong.entity.ModerationResult.builder()
                .id(1)
                .product(sampleProduct)
                .isFlagged(true)
                .moderationLabels(new ArrayList<>())
                .build();

        when(productRepository.findById(10)).thenReturn(Optional.of(sampleProduct));
        when(productImageRepository.findById(100L)).thenReturn(Optional.of(sampleImage));
        when(productImageRepository.findByProduct_ProductIdOrderByDisplayOrderAscIdAsc(10))
                .thenReturn(List.of(cleanImage));
        when(moderationResultRepository.findByProduct_ProductId(10)).thenReturn(Optional.of(mr));

        productImageService.deleteImage(10, 100L);

        verify(productImageRepository).delete(sampleImage);
        assertThat(mr.getIsFlagged()).isFalse();
        verify(moderationResultRepository).save(mr);
    }

    @Test
    void approveImage_adminApprovesImage_unflagsProductWhenAllClean() {
        sampleImage.setIsFlagged(true);
        sampleImage.setIsApproved(false);
        sampleImage.setFlagReason("Ảnh xấu mã");

        com.sang.be_capnong.entity.ModerationResult mr = com.sang.be_capnong.entity.ModerationResult.builder()
                .id(1)
                .product(sampleProduct)
                .isFlagged(true)
                .moderationLabels(new ArrayList<>())
                .build();

        when(productRepository.findById(10)).thenReturn(Optional.of(sampleProduct));
        when(productImageRepository.findById(100L)).thenReturn(Optional.of(sampleImage));
        when(productImageRepository.save(any(ProductImage.class))).thenReturn(sampleImage);
        when(productImageRepository.findByProduct_ProductIdOrderByDisplayOrderAscIdAsc(10))
                .thenReturn(List.of(sampleImage));
        when(moderationResultRepository.findByProduct_ProductId(10)).thenReturn(Optional.of(mr));
        when(productImageMapper.toProductImageResponse(sampleImage)).thenReturn(sampleResponse);

        ProductImageResponse res = productImageService.approveImage(10, 100L);

        assertThat(res).isNotNull();
        assertThat(sampleImage.getIsApproved()).isTrue();
        assertThat(sampleImage.getIsFlagged()).isFalse();
        assertThat(sampleImage.getFlagReason()).isNull();
        assertThat(mr.getIsFlagged()).isFalse();
        verify(moderationResultRepository).save(mr);
    }
}
