package com.sang.be_capnong.controller;

import com.sang.be_capnong.dto.response.ApiResponse;
import com.sang.be_capnong.dto.response.ImageSearchResponse;
import com.sang.be_capnong.service.ImageSearchService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ImageSearchController {

    ImageSearchService imageSearchService;

    @PostMapping(value = "/search-by-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ImageSearchResponse> searchProductsByImage(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request
    ) {
        String clientIp = extractClientIp(request);
        log.info("REST request to search products by image from IP: {}, file: {}, size: {} bytes",
                clientIp, file.getOriginalFilename(), file.getSize());

        ImageSearchResponse response = imageSearchService.searchProductsByImage(file, clientIp);

        return ApiResponse.<ImageSearchResponse>builder()
                .result(response)
                .build();
    }

    private String extractClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        String xf = request.getHeader("X-Forwarded-For");
        // Chỉ chấp nhận X-Forwarded-For nếu request đi qua Reverse Proxy nội bộ (localhost / Nginx)
        if (xf != null && !xf.isBlank() && (remoteAddr != null && (remoteAddr.equals("127.0.0.1") || remoteAddr.equals("0:0:0:0:0:0:0:1")))) {
            return xf.split(",")[0].trim();
        }
        return remoteAddr != null ? remoteAddr : "ANONYMOUS";
    }
}

