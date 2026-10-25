package com.sang.be_capnong.controller;

import com.sang.be_capnong.dto.request.NutritionInfoRequest;
import com.sang.be_capnong.dto.request.ProductAllergensUpdateRequest;
import com.sang.be_capnong.dto.response.ApiResponse;
import com.sang.be_capnong.dto.response.NutritionInfoResponse;
import com.sang.be_capnong.dto.response.ProductAllergenResponse;
import com.sang.be_capnong.service.ProductNutritionService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products/{productId}")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ProductNutritionController {

    ProductNutritionService productNutritionService;

    @GetMapping("/nutrition")
    public ApiResponse<NutritionInfoResponse> getProductNutrition(@PathVariable("productId") Integer productId) {
        log.info("REST request to get nutrition info for product id: {}", productId);
        return ApiResponse.<NutritionInfoResponse>builder()
                .result(productNutritionService.getProductNutrition(productId))
                .build();
    }

    @PutMapping("/nutrition")
    @PreAuthorize("hasAnyRole('SHOP', 'ADMIN')")
    public ApiResponse<NutritionInfoResponse> upsertProductNutrition(
            @PathVariable("productId") Integer productId,
            @RequestBody @Valid NutritionInfoRequest request
    ) {
        log.info("REST request to upsert nutrition info for product id: {}", productId);
        return ApiResponse.<NutritionInfoResponse>builder()
                .result(productNutritionService.upsertProductNutrition(productId, request))
                .build();
    }

    @GetMapping("/allergens")
    public ApiResponse<List<ProductAllergenResponse>> getProductAllergens(@PathVariable("productId") Integer productId) {
        log.info("REST request to get allergens for product id: {}", productId);
        return ApiResponse.<List<ProductAllergenResponse>>builder()
                .result(productNutritionService.getProductAllergens(productId))
                .build();
    }

    @PutMapping("/allergens")
    @PreAuthorize("hasAnyRole('SHOP', 'ADMIN')")
    public ApiResponse<List<ProductAllergenResponse>> updateProductAllergens(
            @PathVariable("productId") Integer productId,
            @RequestBody @Valid ProductAllergensUpdateRequest request
    ) {
        log.info("REST request to update allergens for product id: {}", productId);
        return ApiResponse.<List<ProductAllergenResponse>>builder()
                .result(productNutritionService.updateProductAllergens(productId, request))
                .build();
    }
}
