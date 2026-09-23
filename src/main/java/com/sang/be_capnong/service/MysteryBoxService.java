package com.sang.be_capnong.service;

import com.sang.be_capnong.dto.request.MysteryBoxCreationRequest;
import com.sang.be_capnong.dto.request.MysteryBoxUpdateRequest;
import com.sang.be_capnong.dto.response.MysteryBoxResponse;
import com.sang.be_capnong.dto.response.PageResponse;

import java.math.BigDecimal;

public interface MysteryBoxService {

    MysteryBoxResponse createMysteryBox(MysteryBoxCreationRequest request);

    MysteryBoxResponse getMysteryBoxById(Integer id);

    PageResponse<MysteryBoxResponse> getMysteryBoxes(
            Integer shopId,
            String boxType,
            Boolean isActive,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            int page,
            int size,
            String sortBy,
            String sortDirection
    );

    PageResponse<MysteryBoxResponse> getMyMysteryBoxes(int page, int size);

    MysteryBoxResponse updateMysteryBox(Integer id, MysteryBoxUpdateRequest request);

    MysteryBoxResponse toggleMysteryBoxStatus(Integer id, Boolean isActive);

    void deleteMysteryBox(Integer id);
}
