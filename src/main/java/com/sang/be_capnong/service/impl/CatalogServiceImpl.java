package com.sang.be_capnong.service.impl;

import com.sang.be_capnong.dto.request.AllergenCreationRequest;
import com.sang.be_capnong.dto.request.HealthConditionCreationRequest;
import com.sang.be_capnong.dto.response.AllergenResponse;
import com.sang.be_capnong.dto.response.HealthConditionResponse;
import com.sang.be_capnong.entity.Allergen;
import com.sang.be_capnong.entity.HealthCondition;
import com.sang.be_capnong.exception.AppException;
import com.sang.be_capnong.exception.ErrorCode;
import com.sang.be_capnong.mapper.HealthProfileMapper;
import com.sang.be_capnong.repository.AllergenRepository;
import com.sang.be_capnong.repository.HealthConditionRepository;
import com.sang.be_capnong.service.CatalogService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class CatalogServiceImpl implements CatalogService {

    AllergenRepository allergenRepository;
    HealthConditionRepository healthConditionRepository;
    HealthProfileMapper healthProfileMapper;

    @Override
    @Transactional(readOnly = true)
    public List<AllergenResponse> getAllAllergens() {
        return healthProfileMapper.toAllergenResponseList(allergenRepository.findAll());
    }

    @Override
    @Transactional
    public AllergenResponse createAllergen(AllergenCreationRequest request) {
        String trimmedName = request.getName().trim();
        if (allergenRepository.existsByNameIgnoreCase(trimmedName)) {
            throw new AppException(ErrorCode.ALLERGEN_NAME_EXISTED);
        }
        Allergen allergen = Allergen.builder().name(trimmedName).build();
        return healthProfileMapper.toAllergenResponse(allergenRepository.save(allergen));
    }

    @Override
    @Transactional
    public void deleteAllergen(Integer id) {
        if (!allergenRepository.existsById(id)) {
            throw new AppException(ErrorCode.ALLERGEN_NOT_FOUND);
        }
        allergenRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HealthConditionResponse> getAllHealthConditions() {
        return healthProfileMapper.toHealthConditionResponseList(healthConditionRepository.findAll());
    }

    @Override
    @Transactional
    public HealthConditionResponse createHealthCondition(HealthConditionCreationRequest request) {
        String trimmedName = request.getName().trim();
        if (healthConditionRepository.existsByNameIgnoreCase(trimmedName)) {
            throw new AppException(ErrorCode.CONDITION_NAME_EXISTED);
        }
        HealthCondition condition = HealthCondition.builder().name(trimmedName).build();
        return healthProfileMapper.toHealthConditionResponse(healthConditionRepository.save(condition));
    }

    @Override
    @Transactional
    public void deleteHealthCondition(Integer id) {
        if (!healthConditionRepository.existsById(id)) {
            throw new AppException(ErrorCode.HEALTH_CONDITION_NOT_FOUND);
        }
        healthConditionRepository.deleteById(id);
    }
}
