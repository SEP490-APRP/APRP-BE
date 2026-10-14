package com.sang.be_capnong.service;

import com.sang.be_capnong.dto.ModerationCheckResult;

public interface ImageModerationService {
    ModerationCheckResult checkImageUrl(String imageUrl);

    default ModerationCheckResult checkImageUrl(String imageUrl, String productName) {
        return checkImageUrl(imageUrl, productName, null);
    }

    default ModerationCheckResult checkImageUrl(String imageUrl, String productName, Integer categoryId) {
        return checkImageUrl(imageUrl);
    }
}

