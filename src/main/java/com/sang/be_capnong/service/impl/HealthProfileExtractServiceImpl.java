package com.sang.be_capnong.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sang.be_capnong.dto.request.ConfirmHealthProfileRequest;
import com.sang.be_capnong.dto.request.HealthProfileExtractRequest;
import com.sang.be_capnong.dto.response.HealthProfileExtractResponse;
import com.sang.be_capnong.dto.response.UserHealthProfileResponse;
import com.sang.be_capnong.entity.*;
import com.sang.be_capnong.enums.DietPreference;
import com.sang.be_capnong.exception.AppException;
import com.sang.be_capnong.exception.ErrorCode;
import com.sang.be_capnong.repository.*;
import com.sang.be_capnong.service.HealthProfileExtractService;
import com.sang.be_capnong.service.HealthProfileService;
import com.sang.be_capnong.service.mealplan.ai.ExtractionInput;
import com.sang.be_capnong.service.mealplan.ai.MealPlanAiClient;
import com.sang.be_capnong.service.mealplan.ai.ProfileExtraction;
import com.sang.be_capnong.util.SecurityUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class HealthProfileExtractServiceImpl implements HealthProfileExtractService {

    UserRepository userRepository;
    AllergenRepository allergenRepository;
    HealthConditionRepository healthConditionRepository;
    UserAllergyRepository userAllergyRepository;
    UserHealthConditionRepository userHealthConditionRepository;
    ProfileUpdateRequestRepository profileUpdateRequestRepository;
    HealthProfileService healthProfileService;
    MealPlanAiClient mealPlanAiClient;
    ObjectMapper objectMapper;

    @Override
    @Transactional
    public HealthProfileExtractResponse extractProfile(HealthProfileExtractRequest request) {
        Integer buyerId = SecurityUtils.getCurrentUserId();
        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        log.debug("Buyer {} requested health profile extraction", buyerId);

        // 1. Audit log record
        ProfileUpdateRequest audit = ProfileUpdateRequest.builder()
                .buyer(buyer)
                .rawInputText(request.getText())
                .createdAt(LocalDateTime.now())
                .confirmedAt(null)
                .build();
        audit = profileUpdateRequestRepository.save(audit);

        // 2. Load catalogs and current lists
        List<String> knownAllergens = allergenRepository.findAll().stream()
                .map(Allergen::getName)
                .toList();

        List<String> knownConditions = healthConditionRepository.findAll().stream()
                .map(HealthCondition::getName)
                .toList();

        List<String> currentAllergies = userAllergyRepository.findByBuyer_Id(buyerId).stream()
                .map(ua -> ua.getAllergen().getName())
                .toList();

        List<String> currentConditions = userHealthConditionRepository.findByBuyer_Id(buyerId).stream()
                .map(uc -> uc.getCondition().getName())
                .toList();

        ExtractionInput input = ExtractionInput.builder()
                .text(request.getText())
                .knownAllergens(knownAllergens)
                .knownConditions(knownConditions)
                .currentAllergies(currentAllergies)
                .currentConditions(currentConditions)
                .build();

        // 3. Call AI client (Contract A)
        ProfileExtraction diff = mealPlanAiClient.extractProfile(input);

        // 4. Save extracted diff to audit record
        try {
            audit.setExtractedDiff(objectMapper.writeValueAsString(diff));
            profileUpdateRequestRepository.save(audit);
        } catch (Exception e) {
            log.warn("Failed to serialize extracted diff: {}", e.getMessage());
        }

        // 5. Get current profile
        UserHealthProfileResponse currentProfile = healthProfileService.getMyHealthProfile();

        return HealthProfileExtractResponse.builder()
                .requestId(audit.getId())
                .diff(diff)
                .currentProfile(currentProfile)
                .planRequest(diff.getPlanRequest())
                .needsClarification(diff.getNeedsClarification())
                .build();
    }

    @Override
    @Transactional
    public UserHealthProfileResponse confirmProfileUpdate(int requestId, ConfirmHealthProfileRequest request) {
        Integer buyerId = SecurityUtils.getCurrentUserId();
        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        ProfileUpdateRequest audit = profileUpdateRequestRepository.findByIdAndBuyer_Id(requestId, buyerId)
                .orElseThrow(() -> new AppException(ErrorCode.REQUEST_NOT_FOUND));

        if (audit.getConfirmedAt() != null) {
            throw new AppException(ErrorCode.REQUEST_ALREADY_CONFIRMED);
        }

        ProfileExtraction diff = request.getDiff();
        if (diff == null && audit.getExtractedDiff() != null) {
            try {
                diff = objectMapper.readValue(audit.getExtractedDiff(), ProfileExtraction.class);
            } catch (Exception e) {
                log.warn("Failed to deserialize saved diff: {}", e.getMessage());
            }
        }

        if (diff != null) {
            // Apply add conditions
            if (diff.getAddConditions() != null) {
                for (String condName : splitTokens(diff.getAddConditions())) {
                    if (condName == null || condName.isBlank()) continue;
                    String cleanName = condName.trim();
                    HealthCondition condition = healthConditionRepository.findByNameIgnoreCase(cleanName)
                            .orElseGet(() -> healthConditionRepository.save(HealthCondition.builder().name(cleanName).build()));

                    if (!userHealthConditionRepository.existsByBuyer_IdAndCondition_Id(buyerId, condition.getId())) {
                        userHealthConditionRepository.save(UserHealthCondition.builder()
                                .buyer(buyer)
                                .condition(condition)
                                .build());
                    }
                }
            }

            // Apply remove conditions
            if (diff.getRemoveConditions() != null) {
                for (String condName : splitTokens(diff.getRemoveConditions())) {
                    if (condName == null || condName.isBlank()) continue;
                    healthConditionRepository.findByNameIgnoreCase(condName.trim())
                            .ifPresent(c -> userHealthConditionRepository.deleteByBuyer_IdAndCondition_Id(buyerId, c.getId()));
                }
            }

            // Apply add allergies
            if (diff.getAddAllergies() != null) {
                for (String aName : splitTokens(diff.getAddAllergies())) {
                    if (aName == null || aName.isBlank()) continue;
                    String cleanName = aName.trim();
                    Allergen allergen = allergenRepository.findByNameIgnoreCase(cleanName)
                            .orElseGet(() -> allergenRepository.save(Allergen.builder().name(cleanName).build()));

                    if (!userAllergyRepository.existsByBuyer_IdAndAllergen_Id(buyerId, allergen.getId())) {
                        userAllergyRepository.save(UserAllergy.builder()
                                .buyer(buyer)
                                .allergen(allergen)
                                .build());
                    }
                }
            }

            // Apply remove allergies
            if (diff.getRemoveAllergies() != null) {
                for (String aName : splitTokens(diff.getRemoveAllergies())) {
                    if (aName == null || aName.isBlank()) continue;
                    allergenRepository.findByNameIgnoreCase(aName.trim())
                            .ifPresent(a -> userAllergyRepository.deleteByBuyer_IdAndAllergen_Id(buyerId, a.getId()));
                }
            }

            // Apply diet preference if suggested and confirmed
            if (diff.getDietPreference() != null && !diff.getDietPreference().isBlank()) {
                try {
                    DietPreference dp = DietPreference.valueOf(diff.getDietPreference().trim().toUpperCase());
                    buyer.setDietPreference(dp);
                    userRepository.save(buyer);
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid diet preference from diff: {}", diff.getDietPreference());
                }
            }
        }

        audit.setConfirmedAt(LocalDateTime.now());
        profileUpdateRequestRepository.save(audit);

        return healthProfileService.getMyHealthProfile();
    }

    private List<String> splitTokens(List<String> rawList) {
        if (rawList == null) return Collections.emptyList();
        List<String> out = new ArrayList<>();
        for (String item : rawList) {
            if (item == null || item.isBlank()) continue;
            String[] parts = item.split("[,;\\n]+");
            for (String part : parts) {
                String clean = part.trim();
                if (!clean.isEmpty() && !out.contains(clean)) {
                    out.add(clean);
                }
            }
        }
        return out;
    }
}
