package com.sang.be_capnong.controller;

import com.sang.be_capnong.dto.request.MysteryBoxCreationRequest;
import com.sang.be_capnong.dto.request.MysteryBoxUpdateRequest;
import com.sang.be_capnong.dto.response.ApiResponse;
import com.sang.be_capnong.dto.response.MysteryBoxResponse;
import com.sang.be_capnong.dto.response.PageResponse;
import com.sang.be_capnong.service.MysteryBoxService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/mystery-boxes")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class MysteryBoxController {

    MysteryBoxService mysteryBoxService;

    @PostMapping
    @PreAuthorize("hasRole('SHOP')")
    public ApiResponse<MysteryBoxResponse> createMysteryBox(@RequestBody @Valid MysteryBoxCreationRequest request) {
        log.info("REST request to create mystery box: {}", request.getBoxType());
        return ApiResponse.<MysteryBoxResponse>builder()
                .result(mysteryBoxService.createMysteryBox(request))
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<MysteryBoxResponse> getMysteryBoxById(@PathVariable("id") Integer id) {
        log.info("REST request to get mystery box by id: {}", id);
        return ApiResponse.<MysteryBoxResponse>builder()
                .result(mysteryBoxService.getMysteryBoxById(id))
                .build();
    }

    @GetMapping
    public ApiResponse<PageResponse<MysteryBoxResponse>> getMysteryBoxes(
            @RequestParam(name = "shopId", required = false) Integer shopId,
            @RequestParam(name = "boxType", required = false) String boxType,
            @RequestParam(name = "isActive", required = false) Boolean isActive,
            @RequestParam(name = "minPrice", required = false) BigDecimal minPrice,
            @RequestParam(name = "maxPrice", required = false) BigDecimal maxPrice,
            @RequestParam(name = "page", required = false, defaultValue = "1") int page,
            @RequestParam(name = "size", required = false, defaultValue = "10") int size,
            @RequestParam(name = "sortBy", required = false, defaultValue = "mysteryId") String sortBy,
            @RequestParam(name = "sortDirection", required = false, defaultValue = "desc") String sortDirection
    ) {
        log.info("REST request to get catalog mystery boxes");
        return ApiResponse.<PageResponse<MysteryBoxResponse>>builder()
                .result(mysteryBoxService.getMysteryBoxes(shopId, boxType, isActive, minPrice, maxPrice, page, size, sortBy, sortDirection))
                .build();
    }

    @GetMapping("/my-shop")
    @PreAuthorize("hasRole('SHOP')")
    public ApiResponse<PageResponse<MysteryBoxResponse>> getMyMysteryBoxes(
            @RequestParam(name = "page", required = false, defaultValue = "1") int page,
            @RequestParam(name = "size", required = false, defaultValue = "10") int size
    ) {
        log.info("REST request to get shop mystery boxes");
        return ApiResponse.<PageResponse<MysteryBoxResponse>>builder()
                .result(mysteryBoxService.getMyMysteryBoxes(page, size))
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SHOP')")
    public ApiResponse<MysteryBoxResponse> updateMysteryBox(
            @PathVariable("id") Integer id,
            @RequestBody @Valid MysteryBoxUpdateRequest request
    ) {
        log.info("REST request to update mystery box id: {}", id);
        return ApiResponse.<MysteryBoxResponse>builder()
                .result(mysteryBoxService.updateMysteryBox(id, request))
                .build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SHOP', 'ADMIN')")
    public ApiResponse<MysteryBoxResponse> toggleMysteryBoxStatus(
            @PathVariable("id") Integer id,
            @RequestParam("isActive") Boolean isActive
    ) {
        log.info("REST request to toggle status of mystery box id: {} to {}", id, isActive);
        return ApiResponse.<MysteryBoxResponse>builder()
                .result(mysteryBoxService.toggleMysteryBoxStatus(id, isActive))
                .build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SHOP')")
    public ApiResponse<Void> deleteMysteryBox(@PathVariable("id") Integer id) {
        log.info("REST request to delete mystery box id: {}", id);
        mysteryBoxService.deleteMysteryBox(id);
        return ApiResponse.<Void>builder()
                .message("Mystery box deleted successfully")
                .build();
    }
}
