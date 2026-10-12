package com.sang.be_capnong.service.impl;

import com.sang.be_capnong.dto.request.ProductCreationRequest;
import com.sang.be_capnong.dto.request.ProductUpdateRequest;
import com.sang.be_capnong.dto.response.PageResponse;
import com.sang.be_capnong.dto.response.ProductImageResponse;
import com.sang.be_capnong.dto.response.ProductResponse;
import com.sang.be_capnong.entity.BuildCombo;
import com.sang.be_capnong.entity.Category;
import com.sang.be_capnong.entity.MysteryBox;
import com.sang.be_capnong.entity.Product;
import com.sang.be_capnong.entity.ProductCombo;
import com.sang.be_capnong.entity.ProductMystery;
import com.sang.be_capnong.entity.User;
import com.sang.be_capnong.exception.AppException;
import com.sang.be_capnong.exception.ErrorCode;
import com.sang.be_capnong.mapper.ProductMapper;
import com.sang.be_capnong.entity.ProductImage;
import com.sang.be_capnong.repository.BuildComboRepository;
import com.sang.be_capnong.repository.CategoryRepository;
import com.sang.be_capnong.repository.ModerationResultRepository;
import com.sang.be_capnong.repository.MysteryBoxRepository;
import com.sang.be_capnong.repository.ProductComboRepository;
import com.sang.be_capnong.repository.ProductImageRepository;
import com.sang.be_capnong.repository.ProductMysteryRepository;
import com.sang.be_capnong.repository.ProductRepository;
import com.sang.be_capnong.repository.UserRepository;
import com.sang.be_capnong.service.ProductService;
import com.sang.be_capnong.util.SecurityUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import com.sang.be_capnong.dto.request.ProductRejectRequest;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    ProductRepository productRepository;
    CategoryRepository categoryRepository;
    UserRepository userRepository;
    ProductMapper productMapper;
    ProductMysteryRepository productMysteryRepository;
    MysteryBoxRepository mysteryBoxRepository;
    ProductComboRepository productComboRepository;
    BuildComboRepository buildComboRepository;
    ProductImageRepository productImageRepository;
    ModerationResultRepository moderationResultRepository;

    @Override
    @Transactional
    public ProductResponse createProduct(ProductCreationRequest request) {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        log.info("Creating product '{}' by shop user id: {}", request.getProductName(), currentUserId);

        User shop = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (shop.getRole() == null || !"SHOP".equalsIgnoreCase(shop.getRole().getName())) {
            throw new AppException(ErrorCode.USER_NOT_A_SHOP);
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        if (request.getExpiryDate().isBefore(LocalDate.now())) {
            throw new AppException(ErrorCode.PRODUCT_EXPIRY_INVALID);
        }

        Product product = productMapper.toProduct(request);
        product.setShop(shop);
        product.setCategory(category);
        product.setStatus("PENDING");
        product.setUpdatedAt(LocalDateTime.now());

        Product savedProduct = productRepository.save(product);
        return productMapper.toProductResponse(savedProduct);
    }

    @Override
    public ProductResponse getProductById(Integer productId) {
        log.info("Fetching product details for id: {}", productId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        ProductResponse response = productMapper.toProductResponse(product);
        return filterImagesForViewer(product, response);
    }

    @Override
    public PageResponse<ProductResponse> getProducts(
            Integer categoryId,
            String keyword,
            String status,
            int page,
            int size,
            String sortBy,
            String sortDirection
    ) {
        log.info("Fetching products list - category: {}, keyword: {}, status: {}, page: {}, size: {}",
                categoryId, keyword, status, page, size);

        String effectiveStatus = (status != null && !status.isBlank()) ? status : "ACTIVE";
        String effectiveSortBy = (sortBy != null && !sortBy.isBlank()) ? sortBy : "productId";
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;

        int pageIndex = Math.max(0, page - 1);
        int pageSize = Math.max(1, size);
        Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by(direction, effectiveSortBy));

        Page<Product> productPage = productRepository.findProducts(effectiveStatus, categoryId, keyword, pageable);

        List<ProductResponse> content = productPage.getContent().stream()
                .map(p -> filterImagesForViewer(p, productMapper.toProductResponse(p)))
                .toList();

        return PageResponse.from(productPage, content);
    }

    private ProductResponse filterImagesForViewer(Product product, ProductResponse response) {
        if (response == null || response.getImages() == null) {
            return response;
        }

        Integer currentUserId = SecurityUtils.getCurrentUserIdOrNull();
        boolean isAdmin = SecurityUtils.isAdmin();
        boolean isOwner = currentUserId != null && product.getShop() != null && product.getShop().getId() == currentUserId;

        if (isAdmin || isOwner) {
            return response;
        }

        // Khách mua hàng / guest chỉ thấy các ảnh đã được duyệt (isApproved != false)
        List<ProductImageResponse> visibleImages = response.getImages().stream()
                .filter(img -> !Boolean.FALSE.equals(img.getIsApproved()))
                .toList();
        response.setImages(visibleImages);
        return response;
    }

    @Override
    public PageResponse<ProductResponse> getMyProducts(String status, int page, int size) {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        log.info("Fetching shop products for current shop user id: {}, status: {}", currentUserId, status);

        int pageIndex = Math.max(0, page - 1);
        int pageSize = Math.max(1, size);
        Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by(Sort.Direction.DESC, "productId"));

        String filterStatus = (status != null && !status.isBlank()) ? status : null;
        Page<Product> productPage = productRepository.findByShopIdAndOptionalStatus(currentUserId, filterStatus, pageable);

        return PageResponse.from(productPage, productMapper.toProductResponseList(productPage.getContent()));
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(Integer productId, ProductUpdateRequest request) {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        log.info("Updating product id: {} by user id: {}", productId, currentUserId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        if (product.getShop().getId() != currentUserId) {
            throw new AppException(ErrorCode.NOT_PRODUCT_OWNER);
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        if (request.getExpiryDate().isBefore(LocalDate.now())) {
            throw new AppException(ErrorCode.PRODUCT_EXPIRY_INVALID);
        }

        // Shop cannot self-activate product directly if it is not already ACTIVE
        if (request.getStatus() != null && "ACTIVE".equalsIgnoreCase(request.getStatus().trim())) {
            if (!"ACTIVE".equalsIgnoreCase(product.getStatus())) {
                throw new AppException(ErrorCode.CANNOT_ACTIVATE_DIRECTLY);
            }
        }

        String oldProductName = product.getProductName();
        String currentStatus = product.getStatus() != null ? product.getStatus() : "PENDING";

        productMapper.updateProduct(product, request);
        product.setCategory(category);

        // Status transition lifecycle
        if ("REJECTED".equalsIgnoreCase(currentStatus)) {
            product.setStatus("PENDING");
            product.setRejectReason(null);
        } else if ("PENDING".equalsIgnoreCase(currentStatus)) {
            product.setStatus("PENDING");
        } else if ("ACTIVE".equalsIgnoreCase(currentStatus)) {
            if (request.getStatus() != null && "INACTIVE".equalsIgnoreCase(request.getStatus().trim())) {
                product.setStatus("INACTIVE");
            } else if (!oldProductName.equalsIgnoreCase(request.getProductName().trim())) {
                product.setStatus("PENDING");
            } else {
                product.setStatus("ACTIVE");
            }
        } else if ("INACTIVE".equalsIgnoreCase(currentStatus)) {
            if (request.getStatus() != null && "ACTIVE".equalsIgnoreCase(request.getStatus().trim())) {
                product.setStatus("PENDING");
            } else if (request.getStatus() != null && !request.getStatus().isBlank()) {
                product.setStatus(request.getStatus().trim());
            }
        }

        product.setUpdatedAt(LocalDateTime.now());
        Product updatedProduct = productRepository.save(product);

        if (!"ACTIVE".equalsIgnoreCase(updatedProduct.getStatus())) {
            cascadeDeactivateLinkedBoxesAndCombos(productId);
        }

        return productMapper.toProductResponse(updatedProduct);
    }

    @Override
    @Transactional
    public void deleteProduct(Integer productId) {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        log.info("Deactivating/deleting product id: {} by user id: {}", productId, currentUserId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        if (product.getShop() == null || product.getShop().getId() != currentUserId) {
            throw new AppException(ErrorCode.NOT_PRODUCT_OWNER);
        }

        product.setStatus("INACTIVE");
        product.setUpdatedAt(LocalDateTime.now());
        productRepository.save(product);

        cascadeDeactivateLinkedBoxesAndCombos(productId);
    }

    @Override
    @Transactional
    public ProductResponse approveProduct(Integer productId) {
        log.info("Admin approving product id: {}", productId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        if (!"PENDING".equalsIgnoreCase(product.getStatus())) {
            throw new AppException(ErrorCode.PRODUCT_NOT_PENDING);
        }

        product.setStatus("ACTIVE");
        product.setRejectReason(null);
        product.setUpdatedAt(LocalDateTime.now());

        // Phê duyệt toàn bộ ảnh và gỡ cờ khi Admin duyệt sản phẩm sang ACTIVE
        List<ProductImage> images = productImageRepository.findByProduct_ProductIdOrderByDisplayOrderAscIdAsc(productId);
        for (ProductImage img : images) {
            img.setIsApproved(true);
            img.setIsFlagged(false);
            img.setFlagReason(null);
        }
        productImageRepository.saveAll(images);

        moderationResultRepository.findByProduct_ProductId(productId).ifPresent(mr -> {
            mr.setIsFlagged(false);
            if (mr.getModerationLabels() != null) {
                mr.getModerationLabels().clear();
            }
            moderationResultRepository.save(mr);
        });

        Product savedProduct = productRepository.save(product);
        log.info("Product id: {} successfully approved to ACTIVE", productId);
        return productMapper.toProductResponse(savedProduct);
    }

    @Override
    @Transactional
    public ProductResponse rejectProduct(Integer productId, ProductRejectRequest request) {
        log.info("Admin rejecting product id: {} with reason: {}", productId, request.getReason());
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        if (request.getReason() == null || request.getReason().isBlank()) {
            throw new AppException(ErrorCode.REJECT_REASON_REQUIRED);
        }

        product.setStatus("REJECTED");
        product.setRejectReason(request.getReason().trim());
        product.setUpdatedAt(LocalDateTime.now());

        Product savedProduct = productRepository.save(product);
        log.info("Product id: {} successfully set to REJECTED", productId);

        cascadeDeactivateLinkedBoxesAndCombos(productId);

        return productMapper.toProductResponse(savedProduct);
    }

    private void cascadeDeactivateLinkedBoxesAndCombos(Integer productId) {
        log.info("Cascading deactivation for mystery boxes and combos containing product id: {}", productId);

        // Deactivate active Mystery Boxes containing this product
        List<ProductMystery> productMysteries = productMysteryRepository.findByProduct_ProductId(productId);
        for (ProductMystery pm : productMysteries) {
            MysteryBox box = pm.getMysteryBox();
            if (box != null && Boolean.TRUE.equals(box.getIsActive())) {
                box.setIsActive(false);
                mysteryBoxRepository.save(box);
                log.info("Cascaded deactivation (isActive=false) to Mystery Box id: {} due to non-active product id: {}",
                        box.getMysteryId(), productId);
            }
        }

        // Deactivate active Build Combos containing this product
        List<ProductCombo> productCombos = productComboRepository.findByProduct_ProductId(productId);
        for (ProductCombo pc : productCombos) {
            BuildCombo combo = pc.getBuildCombo();
            if (combo != null && "ACTIVE".equalsIgnoreCase(combo.getStatus())) {
                combo.setStatus("INACTIVE");
                buildComboRepository.save(combo);
                log.info("Cascaded deactivation (status=INACTIVE) to Build Combo id: {} due to non-active product id: {}",
                        combo.getComboId(), productId);
            }
        }
    }
}
