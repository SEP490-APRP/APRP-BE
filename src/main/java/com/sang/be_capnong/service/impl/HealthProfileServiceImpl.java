package com.sang.be_capnong.service.impl;

import com.sang.be_capnong.dto.request.ManualHealthProfileUpdateRequest;
import com.sang.be_capnong.dto.response.AllergenResponse;
import com.sang.be_capnong.dto.response.HealthConditionResponse;
import com.sang.be_capnong.dto.response.UserHealthProfileResponse;
import com.sang.be_capnong.entity.Allergen;
import com.sang.be_capnong.entity.HealthCondition;
import com.sang.be_capnong.entity.User;
import com.sang.be_capnong.entity.UserAllergy;
import com.sang.be_capnong.entity.UserHealthCondition;
import com.sang.be_capnong.exception.AppException;
import com.sang.be_capnong.exception.ErrorCode;
import com.sang.be_capnong.mapper.HealthProfileMapper;
import com.sang.be_capnong.repository.AllergenRepository;
import com.sang.be_capnong.repository.HealthConditionRepository;
import com.sang.be_capnong.repository.UserAllergyRepository;
import com.sang.be_capnong.repository.UserHealthConditionRepository;
import com.sang.be_capnong.repository.UserRepository;
import com.sang.be_capnong.service.HealthProfileService;
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
public class HealthProfileServiceImpl implements HealthProfileService {

    UserRepository userRepository;
    AllergenRepository allergenRepository;
    HealthConditionRepository healthConditionRepository;
    UserAllergyRepository userAllergyRepository;
    UserHealthConditionRepository userHealthConditionRepository;
    HealthProfileMapper healthProfileMapper;

    @Override
    @Transactional(readOnly = true)
    public UserHealthProfileResponse getMyHealthProfile() {
        Integer buyerId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(buyerId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        return buildProfileResponse(user);
    }

    @Override
    @Transactional
    public UserHealthProfileResponse updateMyHealthProfileManual(ManualHealthProfileUpdateRequest request) {
        Integer buyerId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(buyerId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (request.getDietPreference() != null) {
            user.setDietPreference(request.getDietPreference());
            userRepository.save(user);
        }

        if (request.getAllergenIds() != null) {
            userAllergyRepository.deleteByBuyer_Id(buyerId);
            List<UserAllergy> newAllergies = new ArrayList<>();
            for (Integer allergenId : request.getAllergenIds()) {
                Allergen allergen = allergenRepository.findById(allergenId)
                        .orElseThrow(() -> new AppException(ErrorCode.ALLERGEN_NOT_FOUND));
                newAllergies.add(UserAllergy.builder()
                        .buyer(user)
                        .allergen(allergen)
                        .build());
            }
            if (!newAllergies.isEmpty()) {
                userAllergyRepository.saveAll(newAllergies);
            }
        }

        if (request.getConditionIds() != null) {
            userHealthConditionRepository.deleteByBuyer_Id(buyerId);
            List<UserHealthCondition> newConditions = new ArrayList<>();
            for (Integer conditionId : request.getConditionIds()) {
                HealthCondition condition = healthConditionRepository.findById(conditionId)
                        .orElseThrow(() -> new AppException(ErrorCode.HEALTH_CONDITION_NOT_FOUND));
                newConditions.add(UserHealthCondition.builder()
                        .buyer(user)
                        .condition(condition)
                        .build());
            }
            if (!newConditions.isEmpty()) {
                userHealthConditionRepository.saveAll(newConditions);
            }
        }

        return buildProfileResponse(user);
    }

    private UserHealthProfileResponse buildProfileResponse(User user) {
        List<UserAllergy> userAllergies = userAllergyRepository.findByBuyer_Id(user.getId());
        List<AllergenResponse> allergyResponses = userAllergies.stream()
                .map(ua -> healthProfileMapper.toAllergenResponse(ua.getAllergen()))
                .toList();

        List<UserHealthCondition> userConditions = userHealthConditionRepository.findByBuyer_Id(user.getId());
        List<HealthConditionResponse> conditionResponses = userConditions.stream()
                .map(uc -> healthProfileMapper.toHealthConditionResponse(uc.getCondition()))
                .toList();

        return UserHealthProfileResponse.builder()
                .buyerId(user.getId())
                .dietPreference(user.getDietPreference())
                .allergies(allergyResponses)
                .conditions(conditionResponses)
                .build();
    }
}
