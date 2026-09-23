package com.sang.be_capnong.service;

import com.sang.be_capnong.dto.request.BuildComboCreationRequest;
import com.sang.be_capnong.dto.request.BuildComboUpdateRequest;
import com.sang.be_capnong.dto.response.BuildComboResponse;
import com.sang.be_capnong.dto.response.PageResponse;
import com.sang.be_capnong.enums.MealType;

public interface BuildComboService {

    BuildComboResponse createCombo(BuildComboCreationRequest request);

    PageResponse<BuildComboResponse> getCombos(
            String status,
            MealType mealType,
            String region,
            String type,
            String keyword,
            int page,
            int size,
            String sortBy,
            String sortDirection
    );

    BuildComboResponse getComboById(Integer comboId);

    PageResponse<BuildComboResponse> getMyCombos(String status, MealType mealType, int page, int size);

    BuildComboResponse updateCombo(Integer comboId, BuildComboUpdateRequest request);

    BuildComboResponse updateStatus(Integer comboId, String status);

    void deleteCombo(Integer comboId);
}
