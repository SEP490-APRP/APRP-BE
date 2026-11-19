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
import com.sang.be_capnong.service.CloudinaryService;
import com.sang.be_capnong.service.ImageModerationService;
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

import com.sang.be_capnong.dto.response.ComboCatalogItemResponse;
import com.sang.be_capnong.entity.MysteryBox;
import com.sang.be_capnong.mapper.ProductMapper;
import com.sang.be_capnong.repository.MysteryBoxRepository;
import com.sang.be_capnong.repository.specification.MysteryBoxSpecification;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Transactional(readOnly = true)
public class BuildComboServiceImpl implements BuildComboService {

    BuildComboRepository buildComboRepository;
    ProductComboRepository productComboRepository;
    ProductRepository productRepository;
    UserRepository userRepository;
    BuildComboMapper buildComboMapper;
    ImageModerationService imageModerationService;
    CloudinaryService cloudinaryService;
    MysteryBoxRepository mysteryBoxRepository;
    ProductMapper productMapper;

    @Override
    @Transactional
    public BuildComboResponse createCombo(BuildComboCreationRequest request) {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        log.info("Creating build combo '{}' by shop user id: {}", request.getComboName(), currentUserId);

        User shop = getAndValidateShopUser(currentUserId);

        // Kiểm duyệt ảnh combo bằng AI (với categoryId = 5 cho phép hình ảnh mâm đồ ăn nấu chín / combo rau củ)
        if (request.getImageUrl() != null && !request.getImageUrl().isBlank()) {
            com.sang.be_capnong.dto.ModerationCheckResult checkResult = 
                    imageModerationService.checkImageUrl(request.getImageUrl(), request.getComboName(), 5);
            if (!checkResult.isAgricultural()) {
                log.warn("Build combo image rejected (Level 1) for combo '{}': {}", request.getComboName(), checkResult.getRejectionReason());
                cloudinaryService.deleteImageByUrl(request.getImageUrl());
                throw new AppException(ErrorCode.IMAGE_NOT_AGRICULTURAL_PRODUCE);
            }
        }

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
        buildCombo.setStatus("PENDING");

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
        return getCombos(null, status, mealType, region, type, keyword, page, size, sortBy, sortDirection);
    }

    @Override
    public PageResponse<BuildComboResponse> getCombos(
            Integer shopId,
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
        log.info("Fetching build combos - shopId: {}, status: {}, mealType: {}, region: {}, type: {}, keyword: {}, page: {}, size: {}",
                shopId, status, mealType, region, type, keyword, page, size);

        String effectiveStatus = (status != null && !status.isBlank()) ? status.trim().toUpperCase() : "ACTIVE";
        String effectiveSortBy = (sortBy != null && !sortBy.isBlank()) ? sortBy : "comboId";
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;

        int pageIndex = Math.max(0, page - 1);
        int pageSize = Math.max(1, size);
        Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by(direction, effectiveSortBy));

        Specification<BuildCombo> spec = BuildComboSpecification.filterCombos(
                shopId, effectiveStatus, mealType, region, type, keyword
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
        String oldImageUrlToDelete = null;
        if (request.getImageUrl() != null && !request.getImageUrl().isBlank()) {
            String newUrl = request.getImageUrl().trim();
            if (!newUrl.equals(buildCombo.getImageUrl())) {
                String contextName = (request.getComboName() != null && !request.getComboName().isBlank())
                        ? request.getComboName().trim()
                        : buildCombo.getComboName();
                com.sang.be_capnong.dto.ModerationCheckResult checkResult = 
                        imageModerationService.checkImageUrl(newUrl, contextName, 5);
                if (!checkResult.isAgricultural()) {
                    log.warn("Updated combo image rejected (Level 1) for combo '{}': {}", contextName, checkResult.getRejectionReason());
                    cloudinaryService.deleteImageByUrl(newUrl);
                    throw new AppException(ErrorCode.IMAGE_NOT_AGRICULTURAL_PRODUCE);
                }
                if (buildCombo.getImageUrl() != null && !buildCombo.getImageUrl().isBlank()) {
                    oldImageUrlToDelete = buildCombo.getImageUrl();
                }
            }
        }

        buildComboMapper.updateBuildCombo(buildCombo, request);
        // Phương án B: Chuyển về PENDING để Admin duyệt lại mỗi khi chỉnh sửa combo
        buildCombo.setStatus("PENDING");

        BuildCombo updatedCombo = buildComboRepository.save(buildCombo);

        if (oldImageUrlToDelete != null) {
            try {
                cloudinaryService.deleteImageByUrl(oldImageUrlToDelete);
            } catch (Exception e) {
                log.warn("Failed to delete old combo image from Cloudinary: {}", e.getMessage());
            }
        }

        BuildComboResponse response = buildComboMapper.toBuildComboResponse(updatedCombo);
        response.setItems(buildComboMapper.toProductComboResponseList(savedItems));
        return response;
    }

    @Override
    @Transactional
    public BuildComboResponse updateStatus(Integer comboId, String status) {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        boolean isAdmin = SecurityUtils.isAdmin();
        log.info("Updating status for build combo id: {} to '{}' by user id: {} (isAdmin: {})", comboId, status, currentUserId, isAdmin);

        BuildCombo buildCombo = buildComboRepository.findById(comboId)
                .orElseThrow(() -> new AppException(ErrorCode.COMBO_NOT_FOUND));

        if (!isAdmin && (buildCombo.getShop() == null || !Objects.equals(buildCombo.getShop().getId(), currentUserId))) {
            throw new AppException(ErrorCode.NOT_COMBO_OWNER);
        }

        String normalizedStatus = (status != null && !status.isBlank()) ? status.trim().toUpperCase() : "ACTIVE";

        // Shop cannot self-activate combo directly if it is not already ACTIVE
        if ("ACTIVE".equalsIgnoreCase(normalizedStatus) && !isAdmin) {
            if (!"ACTIVE".equalsIgnoreCase(buildCombo.getStatus())) {
                throw new AppException(ErrorCode.CANNOT_ACTIVATE_DIRECTLY);
            }
        }

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

    @Override
    public PageResponse<ComboCatalogItemResponse> getCatalog(
            String category,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String keyword,
            String sortBy,
            int page,
            int size
    ) {
        log.info("Fetching unified combo & mystery box catalog - category: {}, minPrice: {}, maxPrice: {}, keyword: {}, sortBy: {}, page: {}, size: {}",
                category, minPrice, maxPrice, keyword, sortBy, page, size);

        String normalizedCategory = category != null ? category.trim().toLowerCase() : "all";
        int pageIndex = Math.max(0, page - 1);
        int pageSize = Math.max(1, size);

        List<ComboCatalogItemResponse> items = new ArrayList<>();

        boolean queryMystery = "all".equals(normalizedCategory) || "mystery".equals(normalizedCategory);
        boolean queryCombo = "all".equals(normalizedCategory) || "combo".equals(normalizedCategory) || "family".equals(normalizedCategory);
        boolean queryBulk = "all".equals(normalizedCategory) || "bulk".equals(normalizedCategory);

        // 1. Fetch and map MysteryBoxes
        if (queryMystery) {
            Specification<MysteryBox> mysterySpec = MysteryBoxSpecification.filterMysteryBoxes(
                    null, null, true, minPrice, maxPrice, keyword
            );
            List<MysteryBox> boxes = mysteryBoxRepository.findAll(mysterySpec);
            for (MysteryBox box : boxes) {
                items.add(mapMysteryBoxToCatalogItem(box));
            }
        }

        // 2. Fetch and map BuildCombos
        if (queryCombo || queryBulk) {
            String typeFilter = null;
            if ("bulk".equals(normalizedCategory)) {
                typeFilter = "BULK";
            }
            Specification<BuildCombo> comboSpec = BuildComboSpecification.filterCombos(
                    null, "ACTIVE", null, null, typeFilter, keyword, minPrice, maxPrice
            );
            List<BuildCombo> combos = buildComboRepository.findAll(comboSpec);
            for (BuildCombo combo : combos) {
                boolean isBulk = "BULK".equalsIgnoreCase(combo.getType()) 
                        || (combo.getComboName() != null && combo.getComboName().toLowerCase().contains("thùng"));

                if ("combo".equals(normalizedCategory) || "family".equals(normalizedCategory)) {
                    if (isBulk) continue;
                }
                if ("bulk".equals(normalizedCategory)) {
                    if (!isBulk && (combo.getPrice() == null || combo.getPrice().compareTo(BigDecimal.valueOf(200000)) < 0)) {
                        continue;
                    }
                }
                items.add(mapBuildComboToCatalogItem(combo));
            }
        }

        // 3. Sorting
        String sortKey = sortBy != null ? sortBy.trim().toLowerCase() : "featured";
        Comparator<ComboCatalogItemResponse> comparator;
        switch (sortKey) {
            case "price-asc":
            case "price_asc":
                comparator = Comparator.comparing(ComboCatalogItemResponse::getPrice, Comparator.nullsLast(BigDecimal::compareTo));
                break;
            case "price-desc":
            case "price_desc":
                comparator = Comparator.comparing(ComboCatalogItemResponse::getPrice, Comparator.nullsFirst(BigDecimal::compareTo)).reversed();
                break;
            case "discount":
                comparator = Comparator.comparing((ComboCatalogItemResponse item) -> {
                    if (item.getOriginalPrice() != null && item.getPrice() != null) {
                        return item.getOriginalPrice().subtract(item.getPrice());
                    }
                    return BigDecimal.ZERO;
                }, Comparator.nullsFirst(BigDecimal::compareTo)).reversed();
                break;
            case "best-selling":
            case "bestselling":
                comparator = Comparator.comparing(ComboCatalogItemResponse::getSoldQuantity, Comparator.nullsFirst(Integer::compareTo)).reversed();
                break;
            case "featured":
            default:
                comparator = Comparator.comparing((ComboCatalogItemResponse item) -> {
                    Double rating = (item.getShop() != null && item.getShop().getRatingAverage() != null) 
                            ? item.getShop().getRatingAverage() : 0.0;
                    return rating;
                }, Comparator.reverseOrder()).thenComparing(ComboCatalogItemResponse::getSoldQuantity, Comparator.nullsFirst(Integer::compareTo).reversed());
                break;
        }
        items.sort(comparator);

        // 4. In-memory Pagination
        int totalElements = items.size();
        int totalPages = (int) Math.ceil((double) totalElements / pageSize);
        if (totalPages == 0) totalPages = 1;

        int fromIndex = Math.min(pageIndex * pageSize, totalElements);
        int toIndex = Math.min(fromIndex + pageSize, totalElements);
        List<ComboCatalogItemResponse> pagedContent = items.subList(fromIndex, toIndex);

        return PageResponse.<ComboCatalogItemResponse>builder()
                .page(page)
                .size(pageSize)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .first(page <= 1)
                .last(page >= totalPages)
                .content(pagedContent)
                .build();
    }

    private ComboCatalogItemResponse mapBuildComboToCatalogItem(BuildCombo combo) {
        BigDecimal price = combo.getPrice() != null ? combo.getPrice() : BigDecimal.ZERO;
        BigDecimal originalPrice = price.multiply(BigDecimal.valueOf(1.6)).setScale(0, RoundingMode.HALF_UP);
        int discountPercent = 38;
        boolean isBulk = "BULK".equalsIgnoreCase(combo.getType()) || (combo.getComboName() != null && combo.getComboName().toLowerCase().contains("thùng"));
        String category = isBulk ? "Thùng lớn" : "Combo";
        String badge = isBulk ? "Thùng lớn 10kg" : (combo.getMealType() != null ? combo.getMealType().name() : "Combo tươi");
        int sold = combo.getSoldQuantity() != null ? combo.getSoldQuantity() : 0;
        int total = combo.getTotalQuantity() != null ? combo.getTotalQuantity() : 0;
        int stock = Math.max(0, total - sold);

        return ComboCatalogItemResponse.builder()
                .id("combo-" + combo.getComboId())
                .realId(combo.getComboId())
                .itemType("BUILD_COMBO")
                .name(combo.getComboName())
                .price(price)
                .originalPrice(originalPrice)
                .discountPercent(discountPercent)
                .unit(isBulk ? "thùng" : "combo")
                .description(combo.getDescription() != null ? combo.getDescription() : combo.getCookingSuggestion())
                .imageUrl(combo.getImageUrl())
                .category(category)
                .badgeHighlight(badge)
                .soldQuantity(sold)
                .stockQuantity(stock)
                .region(combo.getRegion())
                .shop(combo.getShop() != null ? productMapper.toShopSummaryResponse(combo.getShop()) : null)
                .build();
    }

    private ComboCatalogItemResponse mapMysteryBoxToCatalogItem(MysteryBox box) {
        BigDecimal price = box.getPrice() != null ? box.getPrice() : BigDecimal.ZERO;
        BigDecimal originalPrice = price.multiply(BigDecimal.valueOf(2.0)).setScale(0, RoundingMode.HALF_UP);
        int discountPercent = 50;
        int sold = box.getSoldQuantity() != null ? box.getSoldQuantity() : 0;
        int total = box.getTotalQuantity() != null ? box.getTotalQuantity() : 0;
        int stock = Math.max(0, total - sold);

        return ComboCatalogItemResponse.builder()
                .id("mystery-" + box.getMysteryId())
                .realId(box.getMysteryId())
                .itemType("MYSTERY_BOX")
                .name(box.getBoxType() != null ? box.getBoxType() : "Hộp Nông Sản Bí Ẩn")
                .price(price)
                .originalPrice(originalPrice)
                .discountPercent(discountPercent)
                .unit("hộp")
                .description(box.getDescription() != null ? box.getDescription() : box.getNote())
                .imageUrl(box.getImageUrl())
                .category("Túi mù")
                .badgeHighlight("Túi mù")
                .soldQuantity(sold)
                .stockQuantity(stock)
                .region(box.getShop() != null && box.getShop().getAddress() != null ? box.getShop().getAddress() : null)
                .shop(box.getShop() != null ? productMapper.toShopSummaryResponse(box.getShop()) : null)
                .build();
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
