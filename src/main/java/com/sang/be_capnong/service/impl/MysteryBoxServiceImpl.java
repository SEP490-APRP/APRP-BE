package com.sang.be_capnong.service.impl;

import com.sang.be_capnong.dto.request.MysteryBoxCreationRequest;
import com.sang.be_capnong.dto.request.MysteryBoxUpdateRequest;
import com.sang.be_capnong.dto.request.ProductMysteryItemRequest;
import com.sang.be_capnong.dto.response.MysteryBoxResponse;
import com.sang.be_capnong.dto.response.PageResponse;
import com.sang.be_capnong.dto.response.ProductMysteryResponse;
import com.sang.be_capnong.entity.MysteryBox;
import com.sang.be_capnong.entity.Product;
import com.sang.be_capnong.entity.ProductMystery;
import com.sang.be_capnong.entity.User;
import com.sang.be_capnong.enums.UserStatus;
import com.sang.be_capnong.exception.AppException;
import com.sang.be_capnong.exception.ErrorCode;
import com.sang.be_capnong.mapper.MysteryBoxMapper;
import com.sang.be_capnong.repository.MysteryBoxRepository;
import com.sang.be_capnong.repository.ProductMysteryRepository;
import com.sang.be_capnong.repository.ProductRepository;
import com.sang.be_capnong.repository.UserRepository;
import com.sang.be_capnong.repository.specification.MysteryBoxSpecification;
import com.sang.be_capnong.service.CloudinaryService;
import com.sang.be_capnong.service.ImageModerationService;
import com.sang.be_capnong.service.MysteryBoxService;
import com.sang.be_capnong.util.SecurityUtils;
import org.springframework.data.jpa.domain.Specification;
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

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class MysteryBoxServiceImpl implements MysteryBoxService {

    MysteryBoxRepository mysteryBoxRepository;
    ProductMysteryRepository productMysteryRepository;
    ProductRepository productRepository;
    UserRepository userRepository;
    MysteryBoxMapper mysteryBoxMapper;
    ImageModerationService imageModerationService;
    CloudinaryService cloudinaryService;

    @Override
    @Transactional
    public MysteryBoxResponse createMysteryBox(MysteryBoxCreationRequest request) {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        log.info("Creating mystery box for user id: {}, boxType: {}", currentUserId, request.getBoxType());

        User shop = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (shop.getRole() == null || !"SHOP".equalsIgnoreCase(shop.getRole().getName())) {
            throw new AppException(ErrorCode.USER_NOT_A_SHOP);
        }

        if (shop.getStatus() != UserStatus.ACTIVE || Boolean.FALSE.equals(shop.getShopActive())) {
            throw new AppException(ErrorCode.SHOP_NOT_ACTIVE);
        }

        // Kiểm duyệt ảnh túi mù bằng AI (với categoryId = 5 cho phép hình ảnh hộp quà, giỏ hàng, bao bì đóng gói)
        if (request.getImageUrl() != null && !request.getImageUrl().isBlank()) {
            com.sang.be_capnong.dto.ModerationCheckResult checkResult = 
                    imageModerationService.checkImageUrl(request.getImageUrl(), request.getBoxType(), 5);
            if (!checkResult.isAgricultural()) {
                log.warn("Mystery box image rejected (Level 1) for boxType '{}': {}", request.getBoxType(), checkResult.getRejectionReason());
                cloudinaryService.deleteImageByUrl(request.getImageUrl());
                throw new AppException(ErrorCode.IMAGE_NOT_AGRICULTURAL_PRODUCE);
            }
        }

        // Validate duplicates in items list
        Set<Integer> seenProductIds = new HashSet<>();
        for (ProductMysteryItemRequest item : request.getItems()) {
            if (!seenProductIds.add(item.getProductId())) {
                throw new AppException(ErrorCode.DUPLICATE_PRODUCT_IN_BOX);
            }
        }

        // Validate each product, verify ownership, check active status, and verify/deduct stock
        List<Product> productsToUpdate = new ArrayList<>();
        List<ProductMystery> mysteryItems = new ArrayList<>();

        for (ProductMysteryItemRequest item : request.getItems()) {
            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

            if (product.getShop() == null || product.getShop().getId() != currentUserId) {
                throw new AppException(ErrorCode.PRODUCT_NOT_BELONG_TO_SHOP);
            }

            if (!"ACTIVE".equalsIgnoreCase(product.getStatus())) {
                throw new AppException(ErrorCode.PRODUCT_NOT_ACTIVE);
            }

            int requiredTotal = item.getProductQuantity() * request.getTotalQuantity();
            if (product.getStockQuantity() == null || product.getStockQuantity() < requiredTotal) {
                log.error("Stock insufficient for product id: {}. Available: {}, Required: {}",
                        product.getProductId(), product.getStockQuantity(), requiredTotal);
                throw new AppException(ErrorCode.STOCK_INSUFFICIENT);
            }

            product.setStockQuantity(product.getStockQuantity() - requiredTotal);
            productsToUpdate.add(product);
        }

        // Deduct inventory
        productRepository.saveAll(productsToUpdate);

        // Build and save MysteryBox
        MysteryBox mysteryBox = mysteryBoxMapper.toMysteryBox(request);
        mysteryBox.setShop(shop);
        mysteryBox.setIsActive(false);
        mysteryBox.setSoldQuantity(0);

        MysteryBox savedBox = mysteryBoxRepository.save(mysteryBox);

        // Link items
        for (int i = 0; i < request.getItems().size(); i++) {
            ProductMysteryItemRequest itemReq = request.getItems().get(i);
            Product prod = productsToUpdate.get(i);

            ProductMystery pm = ProductMystery.builder()
                    .mysteryBox(savedBox)
                    .product(prod)
                    .productQuantity(itemReq.getProductQuantity())
                    .build();
            mysteryItems.add(pm);
        }

        List<ProductMystery> savedItems = productMysteryRepository.saveAll(mysteryItems);
        savedBox.setProductMysteries(savedItems);

        MysteryBoxResponse response = mysteryBoxMapper.toMysteryBoxResponse(savedBox);
        response.setItems(mysteryBoxMapper.toProductMysteryResponseList(savedItems));
        return response;
    }

    @Override
    public MysteryBoxResponse getMysteryBoxById(Integer id) {
        log.info("Fetching mystery box by id: {}", id);

        MysteryBox box = mysteryBoxRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MYSTERY_BOX_NOT_FOUND));

        MysteryBoxResponse response = mysteryBoxMapper.toMysteryBoxResponse(box);

        Integer currentUserId = null;
        try {
            currentUserId = SecurityUtils.getCurrentUserId();
        } catch (Exception ignored) {
            // Unauthenticated caller
        }

        boolean isOwner = (currentUserId != null && box.getShop() != null && box.getShop().getId() == currentUserId);
        if (isOwner) {
            List<ProductMystery> items = productMysteryRepository.findByMysteryBox_MysteryId(id);
            response.setItems(mysteryBoxMapper.toProductMysteryResponseList(items));
        } else {
            response.setItems(null); // Option A: Keep secret for buyers
        }

        return response;
    }

    @Override
    public PageResponse<MysteryBoxResponse> getMysteryBoxes(
            Integer shopId,
            String boxType,
            Boolean isActive,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            int page,
            int size,
            String sortBy,
            String sortDirection
    ) {
        log.info("Fetching mystery boxes: shopId={}, boxType={}, isActive={}, page={}, size={}",
                shopId, boxType, isActive, page, size);

        int effectivePage = Math.max(0, page > 0 ? page - 1 : 0);
        int effectiveSize = Math.min(Math.max(1, size), 100);

        String sortField = "mysteryId";
        if (sortBy != null && !sortBy.isBlank()) {
            if ("price".equalsIgnoreCase(sortBy.trim())) {
                sortField = "price";
            } else if ("soldQuantity".equalsIgnoreCase(sortBy.trim())) {
                sortField = "soldQuantity";
            }
        }

        Sort sort = "asc".equalsIgnoreCase(sortDirection)
                ? Sort.by(sortField).ascending()
                : Sort.by(sortField).descending();

        Pageable pageable = PageRequest.of(effectivePage, effectiveSize, sort);

        Boolean effectiveIsActive = isActive != null ? isActive : true;
        String filterBoxType = (boxType != null && !boxType.isBlank()) ? boxType.trim() : null;

        Specification<MysteryBox> spec = MysteryBoxSpecification.filterMysteryBoxes(
                shopId, filterBoxType, effectiveIsActive, minPrice, maxPrice);
        Page<MysteryBox> boxPage = mysteryBoxRepository.findAll(spec, pageable);

        List<MysteryBoxResponse> responseList = mysteryBoxMapper.toMysteryBoxResponseList(boxPage.getContent());
        // Items remain null in general listing
        return PageResponse.from(boxPage, responseList);
    }

    @Override
    public PageResponse<MysteryBoxResponse> getMyMysteryBoxes(int page, int size) {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        log.info("Fetching mystery boxes for shop id: {}, page: {}, size: {}", currentUserId, page, size);

        int effectivePage = Math.max(0, page > 0 ? page - 1 : 0);
        int effectiveSize = Math.min(Math.max(1, size), 100);

        Pageable pageable = PageRequest.of(effectivePage, effectiveSize, Sort.by("mysteryId").descending());
        Page<MysteryBox> boxPage = mysteryBoxRepository.findByShop_Id(currentUserId, pageable);

        List<MysteryBoxResponse> responseList = boxPage.getContent().stream().map(box -> {
            MysteryBoxResponse res = mysteryBoxMapper.toMysteryBoxResponse(box);
            List<ProductMystery> items = productMysteryRepository.findByMysteryBox_MysteryId(box.getMysteryId());
            res.setItems(mysteryBoxMapper.toProductMysteryResponseList(items));
            return res;
        }).toList();

        return PageResponse.from(boxPage, responseList);
    }

    @Override
    @Transactional
    public MysteryBoxResponse updateMysteryBox(Integer id, MysteryBoxUpdateRequest request) {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        log.info("Updating mystery box id: {} by user id: {}", id, currentUserId);

        MysteryBox box = mysteryBoxRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MYSTERY_BOX_NOT_FOUND));

        if (box.getShop() == null || box.getShop().getId() != currentUserId) {
            throw new AppException(ErrorCode.NOT_MYSTERY_BOX_OWNER);
        }

        // Adjust totalQuantity and inventory delta if requested
        if (request.getTotalQuantity() != null) {
            int newTotal = request.getTotalQuantity();
            int sold = box.getSoldQuantity() != null ? box.getSoldQuantity() : 0;
            if (newTotal < sold) {
                throw new AppException(ErrorCode.TOTAL_QUANTITY_LESS_THAN_SOLD);
            }

            int currentTotal = box.getTotalQuantity() != null ? box.getTotalQuantity() : 0;
            int delta = newTotal - currentTotal;

            if (delta != 0) {
                List<ProductMystery> items = productMysteryRepository.findByMysteryBox_MysteryId(id);
                for (ProductMystery item : items) {
                    Product p = item.getProduct();
                    int stockDiff = item.getProductQuantity() * delta;

                    if (delta > 0) { // Increasing box count requires more stock
                        if (p.getStockQuantity() == null || p.getStockQuantity() < stockDiff) {
                            throw new AppException(ErrorCode.STOCK_INSUFFICIENT);
                        }
                        p.setStockQuantity(p.getStockQuantity() - stockDiff);
                    } else { // Decreasing box count refunds stock
                        p.setStockQuantity(p.getStockQuantity() - stockDiff); // stockDiff is negative, so subtracting adds back
                    }
                    productRepository.save(p);
                }
                box.setTotalQuantity(newTotal);
            }
        }

        String oldImageUrlToDelete = null;
        if (request.getImageUrl() != null && !request.getImageUrl().isBlank()) {
            String newUrl = request.getImageUrl().trim();
            if (!newUrl.equals(box.getImageUrl())) {
                String boxType = (request.getBoxType() != null && !request.getBoxType().isBlank())
                        ? request.getBoxType().trim()
                        : box.getBoxType();
                com.sang.be_capnong.dto.ModerationCheckResult checkResult = 
                        imageModerationService.checkImageUrl(newUrl, boxType, 5);
                if (!checkResult.isAgricultural()) {
                    log.warn("Updated mystery box image rejected (Level 1) for boxType '{}': {}", boxType, checkResult.getRejectionReason());
                    cloudinaryService.deleteImageByUrl(newUrl);
                    throw new AppException(ErrorCode.IMAGE_NOT_AGRICULTURAL_PRODUCE);
                }
                if (box.getImageUrl() != null && !box.getImageUrl().isBlank()) {
                    oldImageUrlToDelete = box.getImageUrl();
                }
            }
        }

        mysteryBoxMapper.updateMysteryBox(box, request);
        // Phương án B: Chuyển về isActive = false để Admin duyệt lại mỗi khi chỉnh sửa mystery box
        box.setIsActive(false);

        MysteryBox savedBox = mysteryBoxRepository.save(box);

        if (oldImageUrlToDelete != null) {
            try {
                cloudinaryService.deleteImageByUrl(oldImageUrlToDelete);
            } catch (Exception e) {
                log.warn("Failed to delete old mystery box image from Cloudinary: {}", e.getMessage());
            }
        }

        MysteryBoxResponse res = mysteryBoxMapper.toMysteryBoxResponse(savedBox);
        List<ProductMystery> items = productMysteryRepository.findByMysteryBox_MysteryId(id);
        res.setItems(mysteryBoxMapper.toProductMysteryResponseList(items));
        return res;
    }

    @Override
    @Transactional
    public MysteryBoxResponse toggleMysteryBoxStatus(Integer id, Boolean isActive) {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        boolean isAdmin = SecurityUtils.isAdmin();
        log.info("Toggling mystery box status id: {} to isActive: {} by user: {} (isAdmin: {})", id, isActive, currentUserId, isAdmin);

        MysteryBox box = mysteryBoxRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MYSTERY_BOX_NOT_FOUND));

        if (!isAdmin && (box.getShop() == null || box.getShop().getId() != currentUserId)) {
            throw new AppException(ErrorCode.NOT_MYSTERY_BOX_OWNER);
        }

        boolean targetActive = Boolean.TRUE.equals(isActive);

        // Shop cannot self-activate mystery box directly if it is not already active
        if (targetActive && !isAdmin) {
            if (!Boolean.TRUE.equals(box.getIsActive())) {
                throw new AppException(ErrorCode.CANNOT_ACTIVATE_DIRECTLY);
            }
        }

        box.setIsActive(targetActive);
        MysteryBox savedBox = mysteryBoxRepository.save(box);

        MysteryBoxResponse res = mysteryBoxMapper.toMysteryBoxResponse(savedBox);
        List<ProductMystery> items = productMysteryRepository.findByMysteryBox_MysteryId(id);
        res.setItems(mysteryBoxMapper.toProductMysteryResponseList(items));
        return res;
    }

    @Override
    @Transactional
    public void deleteMysteryBox(Integer id) {
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        log.info("Deleting mystery box id: {} by user id: {}", id, currentUserId);

        MysteryBox box = mysteryBoxRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MYSTERY_BOX_NOT_FOUND));

        if (box.getShop() == null || box.getShop().getId() != currentUserId) {
            throw new AppException(ErrorCode.NOT_MYSTERY_BOX_OWNER);
        }

        int sold = box.getSoldQuantity() != null ? box.getSoldQuantity() : 0;
        int total = box.getTotalQuantity() != null ? box.getTotalQuantity() : 0;
        int unsoldBoxes = Math.max(0, total - sold);

        // Refund unsold products back to stock
        if (unsoldBoxes > 0) {
            List<ProductMystery> items = productMysteryRepository.findByMysteryBox_MysteryId(id);
            for (ProductMystery item : items) {
                Product p = item.getProduct();
                int refundAmount = item.getProductQuantity() * unsoldBoxes;
                p.setStockQuantity((p.getStockQuantity() != null ? p.getStockQuantity() : 0) + refundAmount);
                productRepository.save(p);
            }
        }

        // Soft delete: deactivate and close remaining inventory
        box.setIsActive(false);
        box.setTotalQuantity(sold);
        mysteryBoxRepository.save(box);
        log.info("Soft deleted mystery box id: {}, deactivated and refunded {} unsold units", id, unsoldBoxes);
    }
}
