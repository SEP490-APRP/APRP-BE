package com.sang.be_capnong.controller;

import com.sang.be_capnong.dto.request.AllergenCreationRequest;
import com.sang.be_capnong.dto.request.HealthConditionCreationRequest;
import com.sang.be_capnong.dto.response.AllergenResponse;
import com.sang.be_capnong.dto.response.ApiResponse;
import com.sang.be_capnong.dto.response.HealthConditionResponse;
import com.sang.be_capnong.service.CatalogService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/catalogs")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class CatalogController {

    CatalogService catalogService;

    @GetMapping("/allergens")
    public ApiResponse<List<AllergenResponse>> getAllAllergens() {
        log.info("REST request to get all allergens");
        return ApiResponse.<List<AllergenResponse>>builder()
                .result(catalogService.getAllAllergens())
                .build();
    }

    @PostMapping("/allergens")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AllergenResponse> createAllergen(@RequestBody @Valid AllergenCreationRequest request) {
        log.info("REST request to create allergen: {}", request.getName());
        return ApiResponse.<AllergenResponse>builder()
                .result(catalogService.createAllergen(request))
                .build();
    }

    @DeleteMapping("/allergens/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> deleteAllergen(@PathVariable("id") Integer id) {
        log.info("REST request to delete allergen id: {}", id);
        catalogService.deleteAllergen(id);
        return ApiResponse.<String>builder()
                .result("Allergen deleted successfully")
                .build();
    }

    @GetMapping("/health-conditions")
    public ApiResponse<List<HealthConditionResponse>> getAllHealthConditions() {
        log.info("REST request to get all health conditions");
        return ApiResponse.<List<HealthConditionResponse>>builder()
                .result(catalogService.getAllHealthConditions())
                .build();
    }

    @PostMapping("/health-conditions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<HealthConditionResponse> createHealthCondition(@RequestBody @Valid HealthConditionCreationRequest request) {
        log.info("REST request to create health condition: {}", request.getName());
        return ApiResponse.<HealthConditionResponse>builder()
                .result(catalogService.createHealthCondition(request))
                .build();
    }

    @DeleteMapping("/health-conditions/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> deleteHealthCondition(@PathVariable("id") Integer id) {
        log.info("REST request to delete health condition id: {}", id);
        catalogService.deleteHealthCondition(id);
        return ApiResponse.<String>builder()
                .result("Health condition deleted successfully")
                .build();
    }
}
