package com.sang.be_capnong.service;

import com.sang.be_capnong.dto.request.AllergenCreationRequest;
import com.sang.be_capnong.dto.request.HealthConditionCreationRequest;
import com.sang.be_capnong.dto.response.AllergenResponse;
import com.sang.be_capnong.dto.response.HealthConditionResponse;

import java.util.List;

public interface CatalogService {
    List<AllergenResponse> getAllAllergens();
    AllergenResponse createAllergen(AllergenCreationRequest request);
    void deleteAllergen(Integer id);

    List<HealthConditionResponse> getAllHealthConditions();
    HealthConditionResponse createHealthCondition(HealthConditionCreationRequest request);
    void deleteHealthCondition(Integer id);
}
