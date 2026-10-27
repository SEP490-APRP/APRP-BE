package com.sang.be_capnong.service;

import com.sang.be_capnong.dto.request.NutritionInfoRequest;
import com.sang.be_capnong.dto.request.ProductAllergensUpdateRequest;
import com.sang.be_capnong.dto.response.NutritionInfoResponse;
import com.sang.be_capnong.dto.response.ProductAllergenResponse;

import java.util.List;

public interface ProductNutritionService {
    NutritionInfoResponse getProductNutrition(Integer productId);
    NutritionInfoResponse upsertProductNutrition(Integer productId, NutritionInfoRequest request);

    List<ProductAllergenResponse> getProductAllergens(Integer productId);
    List<ProductAllergenResponse> updateProductAllergens(Integer productId, ProductAllergensUpdateRequest request);
}
