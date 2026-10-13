package com.sang.be_capnong.service.impl;

import com.sang.be_capnong.dto.request.BuildComboCreationRequest;
import com.sang.be_capnong.dto.request.BuildComboUpdateRequest;
import com.sang.be_capnong.dto.request.ProductComboItemRequest;
import com.sang.be_capnong.dto.response.BuildComboResponse;
import com.sang.be_capnong.dto.response.PageResponse;
import com.sang.be_capnong.dto.response.ProductComboResponse;
import com.sang.be_capnong.entity.BuildCombo;
import com.sang.be_capnong.entity.Product;
import com.sang.be_capnong.entity.ProductCombo;
import com.sang.be_capnong.entity.User;
import com.sang.be_capnong.enums.MealType;
import com.sang.be_capnong.enums.UserStatus;
import com.sang.be_capnong.exception.AppException;
import com.sang.be_capnong.exception.ErrorCode;
import com.sang.be_capnong.mapper.BuildComboMapper;
import com.sang.be_capnong.repository.BuildComboRepository;
import com.sang.be_capnong.repository.ProductComboRepository;
import com.sang.be_capnong.repository.ProductRepository;
import com.sang.be_capnong.repository.UserRepository;
import com.sang.be_capnong.repository.specification.BuildComboSpecification;
import com.sang.be_capnong.service.BuildComboService;
import com.sang.be_capnong.util.SecurityUtils;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class BuildComboServiceImpl implements BuildComboService {

    BuildComboRepository buildComboRepository;
    ProductComboRepository productComboRepository;
    ProductRepository productRepository;
    UserRepository userRepository;
    BuildComboMapper buildComboMapper;

    @Override
    @Transactional
    public BuildComboResponse createCombo(BuildComboCreationRequest request) {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        log.info("Creating build combo '{}' by shop user id: {}", request.getComboName(), currentUserId);

        User shop = getAndValidateShopUser(currentUserId);

        // Validate duplicates in items list
        Set<Integer> seenProductIds = new HashSet<>();
        for (ProductComboItemRequest itemReq : request.getItems()) {
            if (!seenProductIds.add(itemReq.getProductId())) {
                throw new AppException(ErrorCode.DUPLICATE_PRODUCT_IN_COMBO);
            }
        }

        BuildCombo buildCombo = buildComboMapper.toBuildCombo(request);
        buildCombo.setShop(shop);
        buildCombo.setSoldQuantity(0);
        buildCombo.setStatus("ACTIVE");

        BuildCombo savedCombo = buildComboRepository.save(buildCombo);

        List<Product> productsToUpdate = new ArrayList<>();
        List<ProductCombo> productCombosToSave = new ArrayList<>();

        for (ProductComboItemRequest itemReq : request.getItems()) {

            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

            if (product.getShop() == null || !Objects.equals(product.getShop().getId(), currentUserId)) {
                throw new AppException(ErrorCode.PRODUCT_NOT_BELONG_TO_SHOP);
            }

            if (!"ACTIVE".equalsIgnoreCase(product.getStatus())) {
                throw new AppException(ErrorCode.PRODUCT_NOT_ACTIVE);
            }

            int requiredTotalStock = itemReq.getProductQuantity() * request.getTotalQuantity();
            if (product.getStockQuantity() == null || product.getStockQuantity() < requiredTotalStock) {
                log.warn("Product id: {} insufficient stock. Current: {}, Required: {}",
                        product.getProductId(), product.getStockQuantity(), requiredTotalStock);
                throw new AppException(ErrorCode.STOCK_INSUFFICIENT);
            }

            // Deduct stock immediately
            product.setStockQuantity(product.getStockQuantity() - requiredTotalStock);
            productsToUpdate.add(product);

            ProductCombo productCombo = ProductCombo.builder()
                    .buildCombo(savedCombo)
                    .product(product)
                    .productQuantity(itemReq.getProductQuantity())
                    .build();
            productCombosToSave.add(productCombo);
        }

        productRepository.saveAll(productsToUpdate);
        List<ProductCombo> savedProductCombos = productComboRepository.saveAll(productCombosToSave);

        BuildComboResponse response = buildComboMapper.toBuildComboResponse(savedCombo);
        response.setItems(buildComboMapper.toProductComboResponseList(savedProductCombos));
        return response;
    }

    @Override
    public PageResponse<BuildComboResponse> getCombos(
            String status,
            MealType mealType,
            String region,
            String type,
            String keyword,
            int page,
            int size,
            String sortBy,
            String sortDirection
    ) {
        log.info("Fetching build combos - status: {}, mealType: {}, region: {}, type: {}, keyword: {}, page: {}, size: {}",
                status, mealType, region, type, keyword, page, size);

        String effectiveStatus = (status != null && !status.isBlank()) ? status.trim().toUpperCase() : "ACTIVE";
        String effectiveSortBy = (sortBy != null && !sortBy.isBlank()) ? sortBy : "comboId";
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;

        int pageIndex = Math.max(0, page - 1);
        int pageSize = Math.max(1, size);
        Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by(direction, effectiveSortBy));

        Specification<BuildCombo> spec = BuildComboSpecification.filterCombos(
                effectiveStatus, mealType, region, type, keyword
        );
        Page<BuildCombo> comboPage = buildComboRepository.findAll(spec, pageable);

        List<BuildComboResponse> responseList = comboPage.getContent().stream().map(combo -> {
            BuildComboResponse resp = buildComboMapper.toBuildComboResponse(combo);
            List<ProductCombo> items = productComboRepository.findByBuildComboComboId(combo.getComboId());
            resp.setItems(buildComboMapper.toProductComboResponseList(items));
            return resp;
        }).toList();

        return PageResponse.from(comboPage, responseList);
    }

    @Override
    public BuildComboResponse getComboById(Integer comboId) {
        log.info("Fetching build combo details for id: {}", comboId);

        BuildCombo buildCombo = buildComboRepository.findById(comboId)
                .orElseThrow(() -> new AppException(ErrorCode.COMBO_NOT_FOUND));

        BuildComboResponse response = buildComboMapper.toBuildComboResponse(buildCombo);
        List<ProductCombo> items = productComboRepository.findByBuildComboComboId(comboId);
        response.setItems(buildComboMapper.toProductComboResponseList(items));
        return response;
    }

    @Override
    public PageResponse<BuildComboResponse> getMyCombos(String status, MealType mealType, int page, int size) {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        log.info("Fetching build combos for shop user id: {}, status: {}, mealType: {}", currentUserId, status, mealType);

        int pageIndex = Math.max(0, page - 1);
        int pageSize = Math.max(1, size);
        Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by(Sort.Direction.DESC, "comboId"));

        String filterStatus = (status != null && !status.isBlank()) ? status.trim().toUpperCase() : null;
        Specification<BuildCombo> spec = BuildComboSpecification.filterShopCombos(currentUserId, filterStatus, mealType);
        Page<BuildCombo> comboPage = buildComboRepository.findAll(spec, pageable);

        List<BuildComboResponse> responseList = comboPage.getContent().stream().map(combo -> {
            BuildComboResponse resp = buildComboMapper.toBuildComboResponse(combo);
            List<ProductCombo> items = productComboRepository.findByBuildComboComboId(combo.getComboId());
            resp.setItems(buildComboMapper.toProductComboResponseList(items));
            return resp;
        }).toList();

        return PageResponse.from(comboPage, responseList);
    }

    @Override
    @Transactional
    public BuildComboResponse updateCombo(Integer comboId, BuildComboUpdateRequest request) {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        log.info("Updating build combo id: {} by user id: {}", comboId, currentUserId);

        BuildCombo buildCombo = buildComboRepository.findById(comboId)
                .orElseThrow(() -> new AppException(ErrorCode.COMBO_NOT_FOUND));

        if (buildCombo.getShop() == null || !Objects.equals(buildCombo.getShop().getId(), currentUserId)) {
            throw new AppException(ErrorCode.NOT_COMBO_OWNER);
        }

        int sold = (buildCombo.getSoldQuantity() != null) ? buildCombo.getSoldQuantity() : 0;
        if (request.getTotalQuantity() < sold) {
            throw new AppException(ErrorCode.TOTAL_QUANTITY_LESS_THAN_SOLD);
        }

        // Two-way inventory adjustment
        int oldUnsold = buildCombo.getTotalQuantity() - sold;
        List<ProductCombo> oldItems = productComboRepository.findByBuildComboComboId(comboId);

        // 1. Refund stock from old unsold combos
        for (ProductCombo oldItem : oldItems) {
            Product prod = oldItem.getProduct();
            int refundStock = oldItem.getProductQuantity() * oldUnsold;
            prod.setStockQuantity((prod.getStockQuantity() != null ? prod.getStockQuantity() : 0) + refundStock);
            productRepository.save(prod);
        }

        // 2. Delete old items
        productComboRepository.deleteByBuildComboComboId(comboId);

        // Validate duplicates in update items list
        Set<Integer> seenNewProductIds = new HashSet<>();
        for (ProductComboItemRequest itemReq : request.getItems()) {
            if (!seenNewProductIds.add(itemReq.getProductId())) {
                throw new AppException(ErrorCode.DUPLICATE_PRODUCT_IN_COMBO);
            }
        }

        // 3. Deduct stock for new items based on new unsold quantity
        int newUnsold = request.getTotalQuantity() - sold;
        List<Product> productsToUpdate = new ArrayList<>();
        List<ProductCombo> newItemsToSave = new ArrayList<>();

        for (ProductComboItemRequest itemReq : request.getItems()) {

            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

            if (product.getShop() == null || !Objects.equals(product.getShop().getId(), currentUserId)) {
                throw new AppException(ErrorCode.PRODUCT_NOT_BELONG_TO_SHOP);
            }

            if (!"ACTIVE".equalsIgnoreCase(product.getStatus())) {
                throw new AppException(ErrorCode.PRODUCT_NOT_ACTIVE);
            }

            int requiredStock = itemReq.getProductQuantity() * newUnsold;
            if (product.getStockQuantity() == null || product.getStockQuantity() < requiredStock) {
                throw new AppException(ErrorCode.STOCK_INSUFFICIENT);
            }

            product.setStockQuantity(product.getStockQuantity() - requiredStock);
            productsToUpdate.add(product);

            ProductCombo productCombo = ProductCombo.builder()
                    .buildCombo(buildCombo)
                    .product(product)
                    .productQuantity(itemReq.getProductQuantity())
                    .build();
            newItemsToSave.add(productCombo);
        }

        productRepository.saveAll(productsToUpdate);
        List<ProductCombo> savedItems = productComboRepository.saveAll(newItemsToSave);

        // 4. Update BuildCombo entity fields
        buildComboMapper.updateBuildCombo(buildCombo, request);
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            buildCombo.setStatus(request.getStatus().trim().toUpperCase());
        }

        BuildCombo updatedCombo = buildComboRepository.save(buildCombo);
        BuildComboResponse response = buildComboMapper.toBuildComboResponse(updatedCombo);
        response.setItems(buildComboMapper.toProductComboResponseList(savedItems));
        return response;
    }

    @Override
    @Transactional
    public BuildComboResponse updateStatus(Integer comboId, String status) {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        log.info("Updating status for build combo id: {} to '{}' by user id: {}", comboId, status, currentUserId);

        BuildCombo buildCombo = buildComboRepository.findById(comboId)
                .orElseThrow(() -> new AppException(ErrorCode.COMBO_NOT_FOUND));

        if (buildCombo.getShop() == null || !Objects.equals(buildCombo.getShop().getId(), currentUserId)) {
            throw new AppException(ErrorCode.NOT_COMBO_OWNER);
        }

        String normalizedStatus = (status != null && !status.isBlank()) ? status.trim().toUpperCase() : "ACTIVE";
        buildCombo.setStatus(normalizedStatus);
        BuildCombo savedCombo = buildComboRepository.save(buildCombo);

        BuildComboResponse response = buildComboMapper.toBuildComboResponse(savedCombo);
        List<ProductCombo> items = productComboRepository.findByBuildComboComboId(comboId);
        response.setItems(buildComboMapper.toProductComboResponseList(items));
        return response;
    }

    @Override
    @Transactional
    public void deleteCombo(Integer comboId) {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        log.info("Deleting build combo id: {} by user id: {}", comboId, currentUserId);

        BuildCombo buildCombo = buildComboRepository.findById(comboId)
                .orElseThrow(() -> new AppException(ErrorCode.COMBO_NOT_FOUND));

        if (buildCombo.getShop() == null || !Objects.equals(buildCombo.getShop().getId(), currentUserId)) {
            throw new AppException(ErrorCode.NOT_COMBO_OWNER);
        }

        int sold = (buildCombo.getSoldQuantity() != null) ? buildCombo.getSoldQuantity() : 0;
        int total = (buildCombo.getTotalQuantity() != null) ? buildCombo.getTotalQuantity() : 0;
        int unsoldQuantity = Math.max(0, total - sold);

        // Refund inventory for unsold combos
        if (unsoldQuantity > 0) {
            List<ProductCombo> items = productComboRepository.findByBuildComboComboId(comboId);
            for (ProductCombo item : items) {
                Product prod = item.getProduct();
                int refundStock = item.getProductQuantity() * unsoldQuantity;
                prod.setStockQuantity((prod.getStockQuantity() != null ? prod.getStockQuantity() : 0) + refundStock);
                productRepository.save(prod);
            }
        }

        // Soft delete: status INACTIVE and cap totalQuantity at soldQuantity
        buildCombo.setStatus("INACTIVE");
        buildCombo.setTotalQuantity(sold);
        buildComboRepository.save(buildCombo);
        log.info("Build combo id: {} soft deleted (set INACTIVE), refunded {} unsold units", comboId, unsoldQuantity);
    }

    private User getAndValidateShopUser(Integer userId) {
        User shop = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (shop.getRole() == null || !"SHOP".equalsIgnoreCase(shop.getRole().getName())) {
            throw new AppException(ErrorCode.USER_NOT_A_SHOP);
        }

        if (shop.getStatus() != UserStatus.ACTIVE) {
            throw new AppException(ErrorCode.SHOP_NOT_ACTIVE);
        }

        return shop;
    }
}
