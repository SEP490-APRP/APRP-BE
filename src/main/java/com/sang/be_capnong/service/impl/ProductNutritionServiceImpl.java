package com.sang.be_capnong.service.impl;

import com.sang.be_capnong.dto.request.NutritionInfoRequest;
import com.sang.be_capnong.dto.request.ProductAllergensUpdateRequest;
import com.sang.be_capnong.dto.response.AllergenResponse;
import com.sang.be_capnong.dto.response.NutritionInfoResponse;
import com.sang.be_capnong.dto.response.ProductAllergenResponse;
import com.sang.be_capnong.entity.Allergen;
import com.sang.be_capnong.entity.NutritionInfo;
import com.sang.be_capnong.entity.Product;
import com.sang.be_capnong.entity.ProductAllergen;
import com.sang.be_capnong.exception.AppException;
import com.sang.be_capnong.exception.ErrorCode;
import com.sang.be_capnong.mapper.HealthProfileMapper;
import com.sang.be_capnong.mapper.NutritionMapper;
import com.sang.be_capnong.repository.AllergenRepository;
import com.sang.be_capnong.repository.NutritionInfoRepository;
import com.sang.be_capnong.repository.ProductAllergenRepository;
import com.sang.be_capnong.repository.ProductRepository;
import com.sang.be_capnong.service.ProductNutritionService;
import com.sang.be_capnong.util.SecurityUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ProductNutritionServiceImpl implements ProductNutritionService {

    ProductRepository productRepository;
    NutritionInfoRepository nutritionInfoRepository;
    ProductAllergenRepository productAllergenRepository;
    AllergenRepository allergenRepository;
    NutritionMapper nutritionMapper;
    HealthProfileMapper healthProfileMapper;

    @Override
    @Transactional(readOnly = true)
    public NutritionInfoResponse getProductNutrition(Integer productId) {
        Product product = findProductOrThrow(productId);
        NutritionInfo nutritionInfo = nutritionInfoRepository.findByProduct_ProductId(product.getProductId())
                .orElseThrow(() -> new AppException(ErrorCode.NUTRITION_NOT_FOUND));
        return nutritionMapper.toNutritionInfoResponse(nutritionInfo);
    }

    @Override
    @Transactional
    public NutritionInfoResponse upsertProductNutrition(Integer productId, NutritionInfoRequest request) {
        Product product = findProductOrThrow(productId);
        checkProductOwnershipOrAdmin(product);

        NutritionInfo nutritionInfo = nutritionInfoRepository.findByProduct_ProductId(product.getProductId())
                .orElse(null);

        if (nutritionInfo == null) {
            nutritionInfo = nutritionMapper.toNutritionInfo(request);
            nutritionInfo.setProduct(product);
        } else {
            nutritionMapper.updateNutritionInfo(nutritionInfo, request);
        }

        NutritionInfo saved = nutritionInfoRepository.save(nutritionInfo);
        return nutritionMapper.toNutritionInfoResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductAllergenResponse> getProductAllergens(Integer productId) {
        findProductOrThrow(productId);
        List<ProductAllergen> productAllergens = productAllergenRepository.findByProduct_ProductId(productId);
        return mapToProductAllergenResponses(productAllergens);
    }

    @Override
    @Transactional
    public List<ProductAllergenResponse> updateProductAllergens(Integer productId, ProductAllergensUpdateRequest request) {
        Product product = findProductOrThrow(productId);
        checkProductOwnershipOrAdmin(product);

        productAllergenRepository.deleteByProduct_ProductId(productId);

        if (request.getAllergenIds() != null && !request.getAllergenIds().isEmpty()) {
            List<ProductAllergen> items = new ArrayList<>();
            for (Integer allergenId : request.getAllergenIds()) {
                Allergen allergen = allergenRepository.findById(allergenId)
                        .orElseThrow(() -> new AppException(ErrorCode.ALLERGEN_NOT_FOUND));
                items.add(ProductAllergen.builder()
                        .product(product)
                        .allergen(allergen)
                        .build());
            }
            productAllergenRepository.saveAll(items);
        }

        List<ProductAllergen> updated = productAllergenRepository.findByProduct_ProductId(productId);
        return mapToProductAllergenResponses(updated);
    }

    private Product findProductOrThrow(Integer productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    private void checkProductOwnershipOrAdmin(Product product) {
        if (SecurityUtils.isAdmin()) {
            return;
        }
        Integer currentUserId = SecurityUtils.getCurrentUserId();
        if (product.getShop() == null || product.getShop().getId() != currentUserId) {
            throw new AppException(ErrorCode.NOT_PRODUCT_OWNER);
        }
    }

    private List<ProductAllergenResponse> mapToProductAllergenResponses(List<ProductAllergen> list) {
        return list.stream()
                .map(pa -> ProductAllergenResponse.builder()
                        .id(pa.getId())
                        .productId(pa.getProduct().getProductId())
                        .allergen(healthProfileMapper.toAllergenResponse(pa.getAllergen()))
                        .build())
                .toList();
    }
}
