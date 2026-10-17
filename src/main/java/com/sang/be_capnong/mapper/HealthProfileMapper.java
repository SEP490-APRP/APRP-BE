package com.sang.be_capnong.mapper;

import com.sang.be_capnong.dto.request.AllergenCreationRequest;
import com.sang.be_capnong.dto.request.HealthConditionCreationRequest;
import com.sang.be_capnong.dto.response.AllergenResponse;
import com.sang.be_capnong.dto.response.HealthConditionResponse;
import com.sang.be_capnong.entity.Allergen;
import com.sang.be_capnong.entity.HealthCondition;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface HealthProfileMapper {

    AllergenResponse toAllergenResponse(Allergen allergen);
    List<AllergenResponse> toAllergenResponseList(List<Allergen> allergens);

    @Mapping(target = "id", ignore = true)
    Allergen toAllergen(AllergenCreationRequest request);

    HealthConditionResponse toHealthConditionResponse(HealthCondition condition);
    List<HealthConditionResponse> toHealthConditionResponseList(List<HealthCondition> conditions);

    @Mapping(target = "id", ignore = true)
    HealthCondition toHealthCondition(HealthConditionCreationRequest request);
}
