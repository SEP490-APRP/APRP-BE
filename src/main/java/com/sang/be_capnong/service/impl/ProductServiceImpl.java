package com.sang.be_capnong.service.impl;

import com.sang.be_capnong.dto.request.ProductCreationRequest;
import com.sang.be_capnong.dto.request.ProductUpdateRequest;
import com.sang.be_capnong.dto.response.PageResponse;
import com.sang.be_capnong.dto.response.ProductResponse;
import com.sang.be_capnong.entity.Category;
import com.sang.be_capnong.entity.Product;
import com.sang.be_capnong.entity.User;
import com.sang.be_capnong.exception.AppException;
import com.sang.be_capnong.exception.ErrorCode;
import com.sang.be_capnong.mapper.ProductMapper;
import com.sang.be_capnong.repository.CategoryRepository;
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

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ProductServiceImpl implements ProductService {

    ProductRepository productRepository;
    CategoryRepository categoryRepository;
    UserRepository userRepository;
    ProductMapper productMapper;

    @Override
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
        product.setStatus("ACTIVE");

        Product savedProduct = productRepository.save(product);
        return productMapper.toProductResponse(savedProduct);
    }

    @Override
    public ProductResponse getProductById(Integer productId) {
        log.info("Fetching product details for id: {}", productId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        return productMapper.toProductResponse(product);
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

        return PageResponse.<ProductResponse>builder()
                .currentPage(page)
                .totalPages(productPage.getTotalPages())
                .pageSize(pageSize)
                .totalElements(productPage.getTotalElements())
                .data(productMapper.toProductResponseList(productPage.getContent()))
                .build();
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

        return PageResponse.<ProductResponse>builder()
                .currentPage(page)
                .totalPages(productPage.getTotalPages())
                .pageSize(pageSize)
                .totalElements(productPage.getTotalElements())
                .data(productMapper.toProductResponseList(productPage.getContent()))
                .build();
    }

    @Override
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

        productMapper.updateProduct(product, request);
        product.setCategory(category);
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            product.setStatus(request.getStatus().trim());
        }

        Product updatedProduct = productRepository.save(product);
        return productMapper.toProductResponse(updatedProduct);
    }

    @Override
    public void deleteProduct(Integer productId) {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        log.info("Deactivating/deleting product id: {} by user id: {}", productId, currentUserId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        boolean isAdmin = currentUser.getRole() != null && "ADMIN".equalsIgnoreCase(currentUser.getRole().getName());
        if (!isAdmin && product.getShop().getId() != currentUserId) {
            throw new AppException(ErrorCode.NOT_PRODUCT_OWNER);
        }

        product.setStatus("INACTIVE");
        productRepository.save(product);
    }
}
