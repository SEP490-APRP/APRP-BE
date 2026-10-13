package com.sang.be_capnong.controller;

import com.sang.be_capnong.dto.request.BuildComboCreationRequest;
import com.sang.be_capnong.dto.request.BuildComboUpdateRequest;
import com.sang.be_capnong.dto.response.ApiResponse;
import com.sang.be_capnong.dto.response.BuildComboResponse;
import com.sang.be_capnong.dto.response.PageResponse;
import com.sang.be_capnong.enums.MealType;
import com.sang.be_capnong.service.BuildComboService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/combos")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class BuildComboController {

    BuildComboService buildComboService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('SHOP')")
    public ApiResponse<BuildComboResponse> createCombo(@RequestBody @Valid BuildComboCreationRequest request) {
        log.info("REST request to create build combo: {}", request.getComboName());
        return ApiResponse.<BuildComboResponse>builder()
                .result(buildComboService.createCombo(request))
                .build();
    }

    @GetMapping
    public ApiResponse<PageResponse<BuildComboResponse>> getCombos(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "mealType", required = false) MealType mealType,
            @RequestParam(value = "region", required = false) String region,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sortBy", defaultValue = "comboId") String sortBy,
            @RequestParam(value = "sortDirection", defaultValue = "desc") String sortDirection
    ) {
        log.info("REST request to get build combos list");
        return ApiResponse.<PageResponse<BuildComboResponse>>builder()
                .result(buildComboService.getCombos(status, mealType, region, type, keyword, page, size, sortBy, sortDirection))
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<BuildComboResponse> getComboById(@PathVariable("id") Integer id) {
        log.info("REST request to get build combo details for id: {}", id);
        return ApiResponse.<BuildComboResponse>builder()
                .result(buildComboService.getComboById(id))
                .build();
    }

    @GetMapping("/my-shop")
    @PreAuthorize("hasRole('SHOP')")
    public ApiResponse<PageResponse<BuildComboResponse>> getMyCombos(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "mealType", required = false) MealType mealType,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        log.info("REST request to get shop build combos");
        return ApiResponse.<PageResponse<BuildComboResponse>>builder()
                .result(buildComboService.getMyCombos(status, mealType, page, size))
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SHOP')")
    public ApiResponse<BuildComboResponse> updateCombo(
            @PathVariable("id") Integer id,
            @RequestBody @Valid BuildComboUpdateRequest request
    ) {
        log.info("REST request to update build combo id: {}", id);
        return ApiResponse.<BuildComboResponse>builder()
                .result(buildComboService.updateCombo(id, request))
                .build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('SHOP')")
    public ApiResponse<BuildComboResponse> updateStatus(
            @PathVariable("id") Integer id,
            @RequestParam("status") String status
    ) {
        log.info("REST request to update status for build combo id: {} to {}", id, status);
        return ApiResponse.<BuildComboResponse>builder()
                .result(buildComboService.updateStatus(id, status))
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SHOP')")
    public ApiResponse<String> deleteCombo(@PathVariable("id") Integer id) {
        log.info("REST request to delete build combo id: {}", id);
        buildComboService.deleteCombo(id);
        return ApiResponse.<String>builder()
                .result("Build combo has been deleted successfully and inventory refunded")
                .build();
    }
}
