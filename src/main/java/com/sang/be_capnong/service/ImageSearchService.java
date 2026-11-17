package com.sang.be_capnong.service;

import com.sang.be_capnong.dto.response.ImageSearchResponse;
import org.springframework.web.multipart.MultipartFile;

public interface ImageSearchService {
    ImageSearchResponse searchProductsByImage(MultipartFile file, String clientIp);
}

